import java.io.IOException;

public class ResponseAccumulator {
    private boolean receivedAll = false;
    private byte[] responseHeadBytes = new byte[]{};
    private byte[] normalizedHeadBytes;
    private byte[] bodyPart;

    public void accumulateBytes(byte[] headPart, int offt, int len) {
        int length = responseHeadBytes.length + len;
        byte[] bytes = new byte[length];
        System.arraycopy(responseHeadBytes, 0, bytes,0, responseHeadBytes.length);
        System.arraycopy(headPart, offt, bytes, responseHeadBytes.length, len);
        responseHeadBytes = bytes;

        int crlfPosition = Util.contains(responseHeadBytes, Constants.CRLFCRLF);
        if (crlfPosition > -1) {
            receivedAll = true;
            int bodyStartPosition = crlfPosition + Constants.CRLFCRLF.length;
            int bodyPartLength = responseHeadBytes.length - bodyStartPosition;
            bodyPart = new byte[bodyPartLength];
            System.arraycopy(responseHeadBytes, bodyStartPosition, bodyPart, 0, bodyPartLength);
        }
    }

    public byte[] getBodyPart() {
        return bodyPart;
    }

    public boolean receivedAll() {
        return receivedAll;
    }

    public void normalizeHead() throws IOException {
        HttpResponseParser parser = new HttpResponseParser();
        parser.parse(responseHeadBytes, responseHeadBytes.length);
        normalizedHeadBytes = parser.getNormalizedHeadBytes();
    }

    public byte[] getNormalizedHeadBytes() {
        return normalizedHeadBytes;
    }
}
