import java.io.IOException;

public abstract class ConnectionHandler {

    abstract void read() throws IOException;

    abstract void write() throws IOException;
}
