import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class HttpRequestParser {

    private BufferedReader reader;
    private String method = "";
    private String protocol = "";
    private String host = "";
    private int code;
    private String message = "";
    private int port = 80;
    private int[] version = new int[2];
    private String pathAndQuery = "";
    private final  List<String> implementedMethods = Arrays.asList("GET", "POST", "HEAD");
    private final static String implementedProtocol = "http";


    private HashMap<String, String> headers = new LinkedHashMap<>();

    public void parse(byte[] input, int length) throws IOException, NotImplementedException {
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

    private void parseFirstLine(String line) throws NotImplementedException {
        String[] str = line.split("\\s");

        if (str.length != 3) {
            throw new NotImplementedException();
        }

        method = str[0];
        if (implementedMethods.contains(method)) {

            int index = str[1].indexOf(':');
            protocol = str[1].substring(0, index);
            if (!protocol.equals(implementedProtocol)) {
                throw new NotImplementedException();
            }

            index += 3;
            host = str[1].substring(index, str[1].indexOf('/', index));
            pathAndQuery = str[1].substring(str[1].indexOf(host) + host.length());
            if (host.indexOf(':') != -1) {
                port = Integer.parseInt(host.substring(host.indexOf(':') + 1));
                if (port < 0 || port >= (1 << 16)) {
                    port = 80;
                }
                host = host.substring(0, host.indexOf(':'));
            }

            version[0] = Integer.parseInt(str[2].substring(5, 6));
            version[1] = Integer.parseInt(str[2].substring(7, 8));
        } else {
            System.out.println(method);
            throw new NotImplementedException();
        }


    }

    private static final Charset ENCODING = StandardCharsets.ISO_8859_1;

    public byte[] getNormalizedHeadBytes() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write((method + " ").getBytes(ENCODING));
            baos.write((pathAndQuery + " ").getBytes(ENCODING));
            baos.write(("HTTP/1.0" + Constants.CRLF).getBytes(ENCODING));

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



    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
