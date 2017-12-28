import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;

public class Proxy implements Runnable {

    private int localPort;
    private Selector selector;

    public static void main(String[] args) throws IOException {
        new Proxy(10081).run();
    }

    Proxy(int localPort) throws IOException {
        this.localPort = localPort;
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
        ConnectionHandler handler = (ConnectionHandler) key.attachment();
        handler.write();
    }

    private void connect(SelectionKey key) {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        try {
            socketChannel.finishConnect();
        } catch (IOException e) {
            close(key);
            return;
        }
        key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
        System.out.println(Constants.CONNECT + socketChannel);
    }

    private void close(SelectionKey key) {

    }

    private void read(SelectionKey key) throws IOException {
        ConnectionHandler handler = (ConnectionHandler) key.attachment();
        handler.read();
    }

    private void accept(SelectionKey key) throws IOException {
        SocketChannel clientSocketChannel = ((ServerSocketChannel) key.channel()).accept();
        clientSocketChannel.configureBlocking(false);
        System.out.println(Constants.ACCEPT + clientSocketChannel);
        clientSocketChannel.register(selector, SelectionKey.OP_READ);
        ClientConnectionHandler handler = new ClientConnectionHandler(selector, clientSocketChannel.keyFor(selector));
        clientSocketChannel.keyFor(selector).attach(handler);
    }

    private void printSelectedSockets(int count) {
        System.out.println();
        System.out.print(Constants.SELECT + count + " socket(s) :");
        selector.selectedKeys().forEach(selectionKey ->
                Constants.operations.forEach((key, value) -> {
                    if ((key & selectionKey.interestOps()) > 0) {
                        System.out.print(" " + value);
                    }
                })
        );
        System.out.println();
    }
}
