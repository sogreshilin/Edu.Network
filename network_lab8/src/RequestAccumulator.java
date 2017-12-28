import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class RequestAccumulator {
    private boolean receivedAll = false;
    private byte[] requestHeadBytes = new byte[]{};
    private byte[] normalizedHeadBytes;
    private String host;
    private int port;

    public void accumulateBytes(byte[] headPart, int offt, int len) {
        int length = requestHeadBytes.length + len;
        byte[] bytes = new byte[length];
        System.arraycopy(requestHeadBytes, 0, bytes,0, requestHeadBytes.length);
        System.arraycopy(headPart, offt, bytes, requestHeadBytes.length, len);
        requestHeadBytes = bytes;
        receivedAll = Util.contains(requestHeadBytes, Constants.CRLFCRLF) > -1;
    }

    public boolean receivedAll() {
        return receivedAll;
    }

    public void normalizeHead() throws IOException, NotImplementedException {
        HttpRequestParser parser = new HttpRequestParser();
        parser.parse(requestHeadBytes, requestHeadBytes.length);
        normalizedHeadBytes = parser.getNormalizedHeadBytes();
        host = parser.getHost();
        port = parser.getPort();
    }

    public byte[] getNormalizedHeadBytes() {
        return normalizedHeadBytes;
    }

    public InetSocketAddress getInetSocketAddress() throws UnknownHostException {
        return new InetSocketAddress(InetAddress.getByName(host), port);
    }
}
