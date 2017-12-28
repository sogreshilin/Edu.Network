package model;

import model.message.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.*;

/**
 * Created by Alexander on 01/11/2017.
 */
public class DatagramSocketHelper implements MessageHandler {
    private final Logger LOG = LogManager.getLogger(DatagramSocketHelper.class);
    private final Object senderAwaker = new Object();
    private HashSet<AbstractMessage> history = new HashSet<>();
    private DatagramSocket datagramSocket;
    private MyServerSocket serverSocket;
    private Thread receiverThread;
    private Thread senderThread;
    private SocketState helperState;
    private Map<InetSocketAddress, MySocket> clients = new HashMap<>();
    private HashSet<AbstractMessage> finAckMessages = new HashSet<>();
    private Sender sender;


    DatagramSocketHelper() throws SocketException {
        datagramSocket = new DatagramSocket();
        receiverThread = new Thread(new Receiver(this, datagramSocket), "Receiver");
        sender = new Sender(this, datagramSocket);
        senderThread = new Thread(sender, "Sender");
        helperState = SocketState.CLOSED;
        start();
    }

    DatagramSocketHelper(MyServerSocket serverSocket, int port) {
        try {
            datagramSocket = new DatagramSocket(port);
            this.serverSocket = serverSocket;
            receiverThread = new Thread(new Receiver(this, datagramSocket));
            sender = new Sender(this, datagramSocket);
            senderThread = new Thread(sender, "Sender");
            helperState = SocketState.RUNNING;
            start();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public Object getSenderAwaker() {
        return senderAwaker;
    }

    SocketState getState() {
        return helperState;
    }

    void setHelperState(SocketState helperState) {
        this.helperState = helperState;
    }

    private void start() {
        receiverThread.start();
        senderThread.start();
    }

    private void stop() {
        setHelperState(SocketState.FINISHED);
        LOG.trace("sender interrupted");
        senderThread.interrupt();
    }

    private void addClient(InetSocketAddress address) {
        MySocket socket = new MySocket(this, address);
        register(address, socket);
        serverSocket.addClient(socket);
    }

    void register(InetSocketAddress address, MySocket socket) {
        synchronized (senderAwaker) {
            clients.put(address, socket);
            senderAwaker.notifyAll();
        }
    }

    ArrayList<MySocket> getClients() throws InterruptedException {
        ArrayList<MySocket> list;
        synchronized (senderAwaker) {
            while (clients.isEmpty()) {
                senderAwaker.wait();
            }
            list = new ArrayList<>(clients.values());
        }
        return list;
    }


    @Override
    public void process(SynMessage message) {
        if (!history.contains(message)) {
            history.add(message);
            LOG.trace("SYN from {}", message.getSender());
            addClient(message.getSender());

            SynAckMessage response = new SynAckMessage();
            response.setUuid(UUID.randomUUID());
            response.setReceiver(message.getSender());
            MySocket socket = clients.get(message.getSender());
            socket.addOutgoing(response);
            LOG.trace("SYN-ACK to : {}", response.getReceiver());
        }
    }

    @Override
    public void process(SynAckMessage message) {
        if (!history.contains(message)) {
            history.add(message);
            LOG.trace("SYN-ACK from {}", message.getSender());
            MySocket socket = clients.get(message.getSender());
            socket.addSystemIncoming(message);
        } else {
            acknowledge(message);
        }
    }

    @Override
    public void process(AckMessage message) {
        if (!history.contains(message)) {
            history.add(message);
            if (finAckMessages.contains(message)) {
                finAckMessages.remove(message);
                MySocket socket = clients.get(message.getSender());
                socket.addIncoming(new FinMessage());
                synchronized (senderAwaker) {
                    clients.remove(message.getSender());
                }
                if (helperState == SocketState.CLOSED && clients.isEmpty()) {
                    stop();
                    return;
                }
            }
            MySocket socket = clients.get(message.getSender());
            if (socket != null) {
                socket.deleteOutgoing(message);
            }
        }

    }

    @Override
    public void process(RegMessage message) {
        LOG.trace("REG-{} from {}", message.getSequenceNumber(), message.getSender());
        MySocket socket = clients.get(message.getSender());
        boolean wasAdded = socket.addIncoming(message);
        if (wasAdded) {
            acknowledge(message);
        }
    }

    @Override
    public void process(FinMessage message) {
        if (!history.contains(message)) {
            history.add(message);
            LOG.trace("FIN from {}", message.getSender());

            FinAckMessage response = new FinAckMessage();
            response.setUuid(UUID.randomUUID());
            response.setReceiver(message.getSender());
            finAckMessages.add(response);
            MySocket socket = clients.get(message.getSender());
            socket.addOutgoing(response);
            socket.setState(SocketState.CLOSED);
        }
    }

    private HashSet<AbstractMessage> finAckHistory = new HashSet<>();

    @Override
    public void process(FinAckMessage message) {
        if (!finAckHistory.contains(message)) {
            finAckHistory.add(message);
            MySocket socket;
            synchronized (senderAwaker) {
                socket = clients.remove(message.getSender());
            }
            socket.addSystemIncoming(message);
            socket.deleteOutgoing(message);
            acknowledge(message);
            if (helperState == SocketState.CLOSED && clients.isEmpty()) {
                stop();
                return;
            }
        } else {
            acknowledge(message);
        }
    }

    void acknowledge(AbstractMessage message) {
        AckMessage ackMessage = new AckMessage();
        ackMessage.setReceiver(message.getSender());
        ackMessage.setUuid(message.getUuid());
        sender.send(ackMessage);
    }
}
