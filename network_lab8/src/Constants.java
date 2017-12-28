import java.nio.channels.SelectionKey;
import java.nio.charset.StandardCharsets;
import java.util.Map;

interface Constants {
    int BUFFER_SIZE = 1024 ;
    String READ = "READ    : ";
    String WRITE = "WRITE   : ";
    String ACCEPT = "ACCEPT  : ";
    String SELECT = "SELECT  : ";
    String CLOSE = "CLOSED  : ";
    String CONNECT = "CONNECT : ";

    byte[] NOT_IMPLEMENTED = "HTTP/1.0 501 Not Implemented".getBytes(StandardCharsets.UTF_8);
    byte[] BAD_GATEWAY = "HTTP/1.0 502 Bad Gateway".getBytes(StandardCharsets.UTF_8);
    byte[] OK = "HTTP/1.0 200 OK".getBytes(StandardCharsets.UTF_8);

    String CRLF = "\r\n";
    byte[] CRLFCRLF = "\r\n\r\n".getBytes(StandardCharsets.UTF_8);

    Map<Integer, String> operations = Map.ofEntries(
            Map.entry(SelectionKey.OP_READ, "read"),
            Map.entry(SelectionKey.OP_WRITE, "write"),
            Map.entry(SelectionKey.OP_ACCEPT, "accept"),
            Map.entry(SelectionKey.OP_CONNECT, "connect")
    );
}
