package model;

import model.message.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class MySocket implements Closeable {
    private static final int QUEUE_CAPACITY = 128;
    private static final long TIMEOUT = 100;
    private static final int BUFFER_SIZE = 1024;
    private static final int PACKET_BUFFER_SIZE = 1024;
    private final Logger LOG = LogManager.getLogger(MyServerSocket.class);
    private DatagramSocketHelper helper;
    private int lastReceivedSequenceNumber = -1;
    private int lastSentSequenceNumber = 0;
    private SocketState state = SocketState.CLOSED;
    private BlockingQueue<AbstractMessage> incoming;
    private ArrayList<OutgoingMessage> outgoing = new ArrayList<>(QUEUE_CAPACITY);
    private final Object outgoingModificationLock = new Object();
    private final Object outgoingEmpty = new Object();
    private BlockingQueue<AbstractMessage> systemIncoming;
    private InetSocketAddress receiverAddress;
    private SocketType socketType;
    private MyByteBuffer byteBuffer = new MyByteBuffer(BUFFER_SIZE);

    private final Object senderAwaker;

    private enum SocketType {SERVER, CLIENT}

    public MySocket(DatagramSocketHelper helper, InetSocketAddress address) {
        state = SocketState.RUNNING;
        incoming = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        systemIncoming = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        receiverAddress = address;
        this.helper = helper;
        socketType = SocketType.SERVER;
        senderAwaker = helper.getSenderAwaker();
    }

    public MySocket() {
        incoming = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        systemIncoming = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        try {
            helper = new DatagramSocketHelper();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        socketType = SocketType.CLIENT;
        senderAwaker = helper.getSenderAwaker();
    }

    public void setState(SocketState state) {
        this.state = state;
        if (socketType == SocketType.CLIENT) {
            helper.setHelperState(SocketState.CLOSED);
        }
    }

    boolean addIncoming(AbstractMessage message) {
        try {
            if (state == SocketState.RUNNING) {
                if (lastReceivedSequenceNumber == message.getSequenceNumber() - 1) {
                    lastReceivedSequenceNumber++;
                    incoming.add(message);
                    return true;
                }
            } else {
                incoming.add(message);
                return true;
            }
            return false;
        } catch (IllegalStateException e) {
            LOG.trace("Queue is full. Message was not added");
            return false;
        }
    }

    public void connect(InetSocketAddress endpoint, int timeout) throws IOException {
        try {
            lastSentSequenceNumber = 0;
            receiverAddress = endpoint;
            helper.register(endpoint, this);
            SynMessage synMessage = new SynMessage();
            synMessage.setReceiver(endpoint);
            UUID synUUID = UUID.randomUUID();
            synMessage.setUuid(synUUID);

            state = SocketState.RUNNING;
            addOutgoing(synMessage);

            AbstractMessage message = systemIncoming.poll(timeout, TimeUnit.MILLISECONDS);
            if (message == null) {
                throw new IOException("connection cannot be established");
            }
            if (!(message instanceof SynAckMessage)) {
                throw new IOException("Three hand shakes rule doesn't work properly");
            }
            state = SocketState.RUNNING;
            deleteOutgoing(synMessage);
            helper.acknowledge(message);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }



    public void write(byte[] buffer, int offset, int length) throws IOException {
        while (length > 0) {
            int chunkSize = Math.min(length, PACKET_BUFFER_SIZE);
            length -= chunkSize;
            byte[] bytes = Arrays.copyOfRange(buffer, offset, offset + chunkSize);
            offset += chunkSize;

            RegMessage message = new RegMessage();
            message.setBody(bytes);
            message.setReceiver(receiverAddress);
            message.setUuid(UUID.randomUUID());
            addOutgoing(message);
        }
    }

    AbstractMessage getMessageToSend() throws InterruptedException {
        if (outgoing.isEmpty()) {
            return null;
        }
        rearrangeBytesInRegMessages();
        OutgoingMessage outgoingMessage;
        synchronized (outgoingModificationLock) {
            outgoingMessage = outgoing.get(0);
            outgoingMessage.lastSend = System.currentTimeMillis();
            outgoingMessage.onceSent = true;
        }
        return outgoingMessage.message;
    }

    private void rearrangeBytesInRegMessages() {
        synchronized (outgoingModificationLock) {
//            System.out.println("before compacting : ");
//            System.out.println(outgoing);
//            System.out.println(outgoing.size());
            List<OutgoingMessage> outgoingMessages = outgoing.stream()
                    .filter(m -> m.message instanceof RegMessage && !m.onceSent && !m.compact)
                    .collect(Collectors.toList());

            if (!outgoingMessages.isEmpty()) {
                outgoing.removeAll(outgoingMessages);

                lastSentSequenceNumber = outgoingMessages.get(0).message.getSequenceNumber();
                ByteBuffer buf = ByteBuffer.allocate(PACKET_BUFFER_SIZE);
                for (OutgoingMessage m : outgoingMessages) {
                    byte[] bytes = ((RegMessage) m.message).getBody();
                    int remaining = buf.remaining();
                    if (remaining > bytes.length) {
                        buf.put(bytes);
                    } else {
                        buf.put(Arrays.copyOfRange(bytes, 0, remaining));
                        byte[] messageBytes = new byte[buf.position()];
                        buf.flip();
                        buf.get(messageBytes);
                        buf.compact();
                        wrapBytesAndAddOutgoing(messageBytes);
                        buf.put(Arrays.copyOfRange(bytes, remaining, bytes.length));
                    }
                }
                if (buf.position() > 0) {
                    byte[] messageBytes = new byte[buf.position()];
                    buf.flip();
                    buf.get(messageBytes);
                    buf.compact();
                    wrapBytesAndAddOutgoing(messageBytes);
                }
            }
//            System.out.println("after compacting : ");
//            System.out.println(outgoing);
//            System.out.println(outgoing.size());
            outgoingModificationLock.notifyAll();
        }

    }

    private void wrapBytesAndAddOutgoing(byte[] messageBytes) {
        RegMessage regMessage = new RegMessage(messageBytes);
        regMessage.setUuid(UUID.randomUUID());
        regMessage.setReceiver(receiverAddress);
        regMessage.setSequenceNumber(lastSentSequenceNumber);
        lastSentSequenceNumber++;
        OutgoingMessage outgoingMessage = new OutgoingMessage(regMessage);
        if (messageBytes.length == PACKET_BUFFER_SIZE) {
            outgoingMessage.compact = true;
        }
        outgoing.add(outgoingMessage);
    }

    long leftToWait() {
        long rv = Long.MAX_VALUE;
        if (!outgoing.isEmpty()) {
            OutgoingMessage outgoingMessage = outgoing.get(0);
            rv = TIMEOUT - (System.currentTimeMillis() - outgoingMessage.lastSend);
        }
        return rv;
    }

    void deleteOutgoing(AbstractMessage message) {
        synchronized (outgoingModificationLock) {
            outgoing.remove(new OutgoingMessage(message));
            outgoingModificationLock.notifyAll();
        }
        synchronized (outgoingEmpty) {
            if (outgoing.isEmpty()) {
                outgoingEmpty.notifyAll();
            }
        }
    }

    void addOutgoing(AbstractMessage message) {
        try {
            synchronized (outgoingModificationLock) {
                while (outgoing.size() >= QUEUE_CAPACITY) {
                    outgoingModificationLock.wait();
                }
                if (state == SocketState.RUNNING) {
                        if (message instanceof RegMessage) {
                            message.setSequenceNumber(lastSentSequenceNumber);
                            lastSentSequenceNumber++;
                        }
                        outgoing.add(new OutgoingMessage(message));
                } else if (message instanceof FinMessage) {
                    outgoing.add(new OutgoingMessage(message));
                }
            }
            synchronized (senderAwaker) {
                senderAwaker.notify();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void addSystemIncoming(AbstractMessage message) {
        try {
            systemIncoming.put(message);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public int read() throws IOException {
        try {
            if (byteBuffer.isEmpty()) {
                AbstractMessage message;
                message = incoming.take();
                if (message instanceof RegMessage) {
                    byteBuffer.put(((RegMessage) message).getBody());
                } else if (message instanceof FinMessage) {
                    return -1;
                }
            }
            return byteBuffer.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public int read(byte[] buffer, int offset, int length) throws IOException {
        if (offset < 0 || length < 0 || length > buffer.length - offset) {
            throw new IndexOutOfBoundsException();
        } else if (length == 0) {
            return 0;
        }
        int i = 0;

        int c = read();
        if (c == -1) {
            return -1;
        }
        buffer[offset] = (byte) c;

        i = 1;
        for (; i < length; i++) {
            if (!byteBuffer.isEmpty() || incoming.peek() != null) {
                c = read();
            } else {
                return i;
            }
            if (c == -1) {
                break;
            }
            buffer[offset + i] = (byte) c;
        }

        return i;
    }

    public final int readInt() throws IOException {
        int ch1 = read();
        int ch2 = read();
        int ch3 = read();
        int ch4 = read();
        if ((ch1 | ch2 | ch3 | ch4) < 0)
            throw new EOFException();
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }

    public final void writeInt(int v) throws IOException {
        byte[] writeBuffer = new byte[4];
        writeBuffer[0] = (byte) ((v >>> 24) & 0xFF);
        writeBuffer[1] = (byte) ((v >>> 16) & 0xFF);
        writeBuffer[2] = (byte) ((v >>> 8) & 0xFF);
        writeBuffer[3] = (byte) ((v >>> 0) & 0xFF);
        write(writeBuffer, 0, 4);
    }

    public long readLong() throws IOException {
        int ch1 = read();
        int ch2 = read();
        int ch3 = read();
        int ch4 = read();
        int ch5 = read();
        int ch6 = read();
        int ch7 = read();
        int ch8 = read();
        if ((ch1 | ch2 | ch3 | ch4 | ch5 | ch6 | ch7 | ch8) < 0) {
            throw new EOFException();
        }
        return ((long) ch1 << 56) +
                ((long) ch2 << 48) +
                ((long) ch3 << 40) +
                ((long) ch4 << 32) +
                ((long) ch5 << 24) +
                (ch6 << 16) +
                (ch7 << 8) +
                (ch8 << 0);
    }

    public final void writeLong(long value) throws IOException {
        byte[] writeBuffer = new byte[8];
        writeBuffer[0] = (byte) ((value >>> 56) & 0xFF);
        writeBuffer[1] = (byte) ((value >>> 48) & 0xFF);
        writeBuffer[2] = (byte) ((value >>> 40) & 0xFF);
        writeBuffer[3] = (byte) ((value >>> 32) & 0xFF);
        writeBuffer[4] = (byte) ((value >>> 24) & 0xFF);
        writeBuffer[5] = (byte) ((value >>> 16) & 0xFF);
        writeBuffer[6] = (byte) ((value >>> 8) & 0xFF);
        writeBuffer[7] = (byte) ((value >>> 0) & 0xFF);
        write(writeBuffer, 0, 8);
    }

    @Override
    public void close() throws IOException {
        state = SocketState.CLOSED;
        FinMessage finMessage = new FinMessage();
        finMessage.setReceiver(receiverAddress);
        finMessage.setUuid(UUID.randomUUID());
        addOutgoing(finMessage);
        try {
            AbstractMessage message = systemIncoming.take();
            if (message instanceof FinAckMessage) {
                deleteOutgoing(finMessage);
                synchronized (outgoingEmpty) {
                    while (!outgoing.isEmpty()) {
                        outgoingEmpty.wait();
                    }
                }
                helper.acknowledge(message);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class MyByteBuffer {
        byte[] buffer;
        int position;
        int length;

        MyByteBuffer(int size) {
            buffer = new byte[size];
            position = 0;
            length = 0;
        }

        int get() {
            int b = buffer[position];
            position++;
            return b & 0xFF;
        }

        boolean isEmpty() {
            return position == length;
        }

        void put(byte[] data) {
            position = 0;
            length = data.length;
            buffer = Arrays.copyOf(data, data.length);
        }
    }

    private class OutgoingMessage {
        AbstractMessage message;
        long lastSend;
        boolean onceSent = false;
        boolean compact = false;

        public OutgoingMessage(AbstractMessage message, long lastSend) {
            this.message = message;
            this.lastSend = lastSend;
        }

        OutgoingMessage(AbstractMessage message) {
            this.message = message;
            this.lastSend = 0;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null) return false;

            OutgoingMessage that = (OutgoingMessage) o;

            return message.equals(that.message);
        }

        @Override
        public int hashCode() {
            return message.hashCode();
        }

        @Override
        public String toString() {
            return "OutgoingMessage{" +
                    "message=" + message +
                    ", compact=" + compact +
                    '}';
        }
    }


}
