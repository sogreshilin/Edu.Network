import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class HttpResponseParser {

    private BufferedReader reader;
    private int[] version = new int[2];
    private int statusCode;
    private String reasonPhrase;


    private HashMap<String, String> headers = new LinkedHashMap<>();

    public void parse(byte[] input, int length) throws IOException {
        reader = new BufferedReader(
                new InputStreamReader(
                        new ByteArrayInputStream(input, 0, length), StandardCharsets.UTF_8));

        String line = reader.readLine();
        parseFirstLine(line);

        parseHeaders();
    }

    private void parseHeaders() throws IOException {
        String line;
        while (!(line = reader.readLine()).isEmpty()) {
            String[] pair = line.split(": ");
            headers.put(pair[0], pair[1]);
        }
    }

    private void parseFirstLine(String line) {
        String[] str = line.split("\\s");

        version[0] = Integer.parseInt(str[0].substring(5, 6));
        version[1] = Integer.parseInt(str[0].substring(7, 8));

        statusCode = Integer.parseInt(str[1]);
        if (str.length == 3) {
            reasonPhrase = str[2];
        }
    }

    private static final Charset ENCODING = StandardCharsets.ISO_8859_1;

    public byte[] getNormalizedHeadBytes() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(("HTTP/1.0 ").getBytes(ENCODING));
            baos.write((statusCode + " ").getBytes(ENCODING));
            baos.write((reasonPhrase + Constants.CRLF).getBytes(ENCODING));

            headers.put("Connection", "close");
            for (Map.Entry e : headers.entrySet()) {
                baos.write((e.getKey() + ": ").getBytes(ENCODING));
                baos.write(((e.getValue() + Constants.CRLF).getBytes(ENCODING)));
            }
            baos.write(Constants.CRLF.getBytes(ENCODING));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
    }
}
