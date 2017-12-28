import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

class ServerConnectionHandler extends ConnectionHandler {

    private SelectionKey key;
    private SelectionKey peer;
    private ByteBuffer request;
    private ResponseAccumulator responseAccumulator;
    private boolean httpMessageHeadRead = false;
    private ByteBuffer in;
    private ByteBuffer out;

    ServerConnectionHandler(SelectionKey key, SelectionKey peer) {
        ClientConnectionHandler peerAttachment = (ClientConnectionHandler) peer.attachment();
        this.key = key;
        this.peer = peer;

        this.in = peerAttachment.out;
        byte[] rawRequest = peerAttachment.requestAccumulator.getNormalizedHeadBytes();
        this.request = ByteBuffer.allocate(rawRequest.length);
        this.request.put(rawRequest);
        this.request.flip();

        this.responseAccumulator = new ResponseAccumulator();
    }

    @Override
    void read() throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        int bytesRead = 0;
        if (!httpMessageHeadRead) {
            ByteBuffer buf = ByteBuffer.allocate(Constants.BUFFER_SIZE);
            try {
                bytesRead = socketChannel.read(buf);
            } catch (IOException e) {
                close(key);
            }
            System.out.println(Constants.READ + "from socket " + socketChannel + " " + bytesRead + " bytes");
            if (bytesRead == -1) {
                close(key);
            }
            responseAccumulator.accumulateBytes(buf.array(), 0, buf.position());

            if (responseAccumulator.receivedAll()) {
                httpMessageHeadRead = true;

                responseAccumulator.normalizeHead();
                ClientConnectionHandler peerAttachment = (ClientConnectionHandler) peer.attachment();

                byte[] rawResponse = responseAccumulator.getNormalizedHeadBytes();

                peerAttachment.response = ByteBuffer.allocate(rawResponse.length);
                peerAttachment.response.put(rawResponse);
                peerAttachment.response.flip();

                byte[] rawBodyPart = responseAccumulator.getBodyPart();
                peerAttachment.out.flip();
                peerAttachment.out.clear();
                peerAttachment.out.put(rawBodyPart);
                if (peer.isValid()) {
                    peer.interestOps(peer.interestOps() | SelectionKey.OP_WRITE);
                }
            }
        } else {
            key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
            bytesRead = socketChannel.read(in);
            System.out.println(Constants.READ + "from socket " + socketChannel + " " + bytesRead + " bytes");
            if (peer.isValid()) {
                peer.interestOps(peer.interestOps() | SelectionKey.OP_WRITE);
            }

            if (bytesRead == -1) {
                close(key);
            }
        }
    }

    @Override
    void write() throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        int bytesWritten = socketChannel.write(request);
        if (request.remaining() == 0) {
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
            socketChannel.shutdownOutput();
        }
        System.out.println(Constants.WRITE + "to   socket " + socketChannel + " " + bytesWritten + " bytes");
    }

    private void close(SelectionKey key) throws IOException {
        System.out.println(Constants.CLOSE + key.channel());
        key.cancel();
        key.channel().close();
        ((ClientConnectionHandler) peer.attachment()).peer = null;
        if (peer.isValid()) {
            peer.interestOps(SelectionKey.OP_WRITE);
        }
    }
}
