import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class ClientConnectionHandler extends ConnectionHandler {

    Selector selector;
    SelectionKey key;
    SelectionKey peer;
    RequestAccumulator requestAccumulator;
    boolean httpMessageHeadWritten = false;
    boolean httpMessageHeadRead = false;
    ByteBuffer out;
    ByteBuffer in;
    ByteBuffer response;

    public ClientConnectionHandler(Selector selector, SelectionKey key) {
        this.selector = selector;
        this.key = key;
        this.in = ByteBuffer.allocate(Constants.BUFFER_SIZE);
        this.out = ByteBuffer.allocate(Constants.BUFFER_SIZE);
        this.requestAccumulator = new RequestAccumulator();
    }

    @Override
    void read() throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        int bytesRead = 0;

        if (!httpMessageHeadRead) {
            ByteBuffer in = ByteBuffer.allocate(Constants.BUFFER_SIZE);
            bytesRead = socketChannel.read(in);

            System.out.println(Constants.READ + "from socket " + socketChannel + " " + bytesRead + " bytes");
            if (bytesRead == -1) {
                close(key);
            }
            requestAccumulator.accumulateBytes(in.array(), 0, in.position());

            if (requestAccumulator.receivedAll()) {
                httpMessageHeadRead = true;
                try {
                    requestAccumulator.normalizeHead();
                } catch (NotImplementedException e) {
                    out.put(Constants.NOT_IMPLEMENTED);
                    httpMessageHeadWritten = true;
                    key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                    return;
                }

                InetSocketAddress hostAddress;
                try {
                    hostAddress = requestAccumulator.getInetSocketAddress();
                } catch (UnknownHostException e) {
                    out.put(Constants.BAD_GATEWAY);
                    httpMessageHeadWritten = true;
                    key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                    return;
                }

                SocketChannel serverSocketChannel = SocketChannel.open();
                serverSocketChannel.configureBlocking(false);
                serverSocketChannel.connect(hostAddress);
                serverSocketChannel.register(selector, SelectionKey.OP_CONNECT);

                ServerConnectionHandler serverHandler =
                        new ServerConnectionHandler(serverSocketChannel.keyFor(selector), key);

                serverSocketChannel.keyFor(selector).attach(serverHandler);
                this.peer = serverSocketChannel.keyFor(selector);
            }
        }
    }

    @Override
    void write() throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        ClientConnectionHandler handler = (ClientConnectionHandler) key.attachment();

        int bytesWritten = 0;
        if (!httpMessageHeadWritten) {
            if (response != null) {
                bytesWritten = socketChannel.write(response);
                System.out.println(Constants.WRITE + "to   socket " + socketChannel + " " + bytesWritten + " bytes");
            } else {
                close(key);
                return;
            }
            httpMessageHeadWritten = (response.remaining() == 0);
        } else {

            out.flip();
            try {
                bytesWritten = socketChannel.write(out);
                System.out.println(Constants.WRITE + "to   socket " + socketChannel + " " + bytesWritten + " bytes");
            } catch (IOException e) {
                socketChannel.close();
            }
            out.compact();


            if (out.position() < out.limit() && peer != null) {
                handler.peer.interestOps(handler.peer.interestOps() | SelectionKey.OP_READ);
                peer.interestOps(peer.interestOps() | SelectionKey.OP_READ);
            }

            if (out.position() == 0) {
                key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                if (peer == null) {
                    close(key);
                }
            }
        }
    }

    private void close(SelectionKey key) throws IOException {
        System.out.println(Constants.CLOSE + key.channel());
        key.cancel();
        key.channel().close();
    }
}
