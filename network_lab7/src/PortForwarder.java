import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by Alexander on 16/12/2017.
 */
public class PortForwarder implements Runnable {

    class Attachment {
        boolean readFinished = false;
        boolean writeFinished = false;
        ByteBuffer in;
        ByteBuffer out;
        SelectionKey peer;
    }

    static final int BUFFER_SIZE = 1024;
    private static final String READ = "READ    : ";
    private static final String WRITE = "WRITE   : ";
    private static final String ACCEPT = "ACCEPT  : ";
    private static final String SELECT = "SELECT  : ";
    private static final String CLOSE = "CLOSED  : ";
    private static final String CONNECT = "CONNECT : ";
    private static final int OP_NONE = 0;

    private static final HashMap<Integer, String> operations = new HashMap<>();

    static {
        operations.put(SelectionKey.OP_READ, "read");
        operations.put(SelectionKey.OP_WRITE, "write");
        operations.put(SelectionKey.OP_ACCEPT, "accept");
        operations.put(SelectionKey.OP_CONNECT, "connect");
    }

    private int localPort;
    private InetSocketAddress hostAddress;
    private Selector selector;

    private static final byte[] PIWIGO_IP = {87, 98, (byte) 147, 22};

    public static void main(String[] args) throws IOException {
        new PortForwarder(10080, "piwigo.org", 80).run();
    }

    public PortForwarder(int localPort, String hostName, int hostPort) throws IOException {
        this.localPort = localPort;
        this.hostAddress = new InetSocketAddress(InetAddress.getByAddress(PIWIGO_IP), hostPort);
        this.selector = initSelector();
    }

    private Selector initSelector() throws IOException {
        Selector socketSelector = SelectorProvider.provider().openSelector();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.socket().bind(new InetSocketAddress(localPort));
        serverSocketChannel.register(socketSelector, SelectionKey.OP_ACCEPT);
        return socketSelector;
    }

    @Override
    public void run() {
        try {
            while (true) {
                int count = selector.select();
                printSelectedSockets(count);

                Iterator iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = (SelectionKey) iterator.next();
                    iterator.remove();
                    if (key.isAcceptable()) {
                        accept(key);
                    } else if (key.isConnectable()) {
                        connect(key);
                    } else if (key.isReadable()) {
                        read(key);
                    } else if (key.isWritable()) {
                        write(key);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void write(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        Attachment attachment = (Attachment) key.attachment();

        attachment.out.flip();
        if (!attachment.writeFinished) {
            int bytesWritten = socketChannel.write(attachment.out);
            System.out.println(WRITE + "to   socket " + socketChannel + " " + bytesWritten + " bytes");
        }
        attachment.out.compact();

        if (attachment.out.position() == 0) {
            if (attachment.readFinished) {
                tryClose(key);
            } else if (((Attachment) attachment.peer.attachment()).readFinished) {
                attachment.writeFinished = true;
                socketChannel.shutdownOutput();
                System.out.println(WRITE + "finished (flag is set, shutdown is sent) on " + attachment.peer.channel());
            } else {
                attachment.peer.interestOps(attachment.peer.interestOps() | SelectionKey.OP_READ);
                key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
            }
        }
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        Attachment attachment = (Attachment) key.attachment();

        int bytesRead = socketChannel.read(attachment.in);

        System.out.println(READ + "from socket " + socketChannel + " " + bytesRead + " bytes");

        if (bytesRead == -1) {
            attachment.readFinished = true;
            System.out.println(READ + "finished (flag is set) on " + socketChannel);
            key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
            if (attachment.writeFinished) {
                tryClose(key);
                return;
            }
            if (((Attachment) attachment.peer.attachment()).out.position() == 0) {
                ((Attachment) attachment.peer.attachment()).writeFinished = true;
                ((SocketChannel) attachment.peer.channel()).shutdownOutput();
                System.out.println(WRITE + "finished (flag is set, shutdown is sent) on " + attachment.peer.channel());
            }
        } else {
            attachment.peer.interestOps(attachment.peer.interestOps() | SelectionKey.OP_WRITE);
            key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
        }
    }

    private void tryClose(SelectionKey key) throws IOException {
        Attachment attachment = (Attachment) key.attachment();
        if (attachment.readFinished && attachment.writeFinished) {
            System.out.println(CLOSE + key.channel());
            key.cancel();
            key.channel().close();
            SelectionKey peerKey = ((Attachment) key.attachment()).peer;
            if (peerKey != null) {
                ((Attachment) peerKey.attachment()).peer = null;
                ((Attachment) peerKey.attachment()).readFinished = true;
                if (((Attachment) peerKey.attachment()).out.position() == 0) {
                    ((Attachment) peerKey.attachment()).writeFinished = true;
                }
                peerKey.interestOps(SelectionKey.OP_WRITE);
            }
        }
    }

    private void connect(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        socketChannel.finishConnect();
        SelectionKey peer = ((Attachment) key.attachment()).peer;
        key.interestOps(SelectionKey.OP_READ);
        peer.interestOps(SelectionKey.OP_READ);
//        System.out.println(CONNECT + socketChannel);
        System.out.println(CONNECT + "pair = (" + peer.channel() + ", " + socketChannel + ")");
    }

    private void accept(SelectionKey key) throws IOException {
        SocketChannel leftSocketChannel = ((ServerSocketChannel) key.channel()).accept();
        leftSocketChannel.configureBlocking(false);
        leftSocketChannel.register(selector, OP_NONE);

        SocketChannel rightSocketChannel = SocketChannel.open();
        rightSocketChannel.configureBlocking(false);
        rightSocketChannel.connect(hostAddress);
        rightSocketChannel.register(selector, SelectionKey.OP_CONNECT);

        Attachment rightAttachment = new Attachment();
        rightAttachment.peer = leftSocketChannel.keyFor(selector);
        rightAttachment.in = ByteBuffer.allocate(BUFFER_SIZE);
        rightAttachment.out = ByteBuffer.allocate(BUFFER_SIZE);
        rightSocketChannel.keyFor(selector).attach(rightAttachment);

        Attachment leftAttachment = new Attachment();
        leftAttachment.peer = rightSocketChannel.keyFor(selector);
        leftAttachment.out = rightAttachment.in;
        leftAttachment.in = rightAttachment.out;
        leftSocketChannel.keyFor(selector).attach(leftAttachment);

        System.out.println(ACCEPT + rightSocketChannel);
    }

    private void printSelectedSockets(int count) {
        System.out.println();
        System.out.print(SELECT + count + " socket(s) :");
        selector.selectedKeys().forEach(selectionKey ->
                operations.forEach((key, value) -> {
                    if ((key & selectionKey.interestOps()) > 0) {
                        System.out.print(" " + value);
                    }
                })
        );
        System.out.println();
    }
}
