import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class ClientHandler implements Runnable {
    private static final Logger LOGGER = LogManager.getLogger(ClientHandler.class);

    private DataInputStream is;
    private DataOutputStream os;
    private Socket socket;
    private Server server;

    ClientHandler(Socket socket, Server server) {
        try {
            this.server = server;
            this.socket = socket;
            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());
            if (!server.finished()) {
                run();
            }
        } catch (IOException e) {
            LOGGER.error("input/output error", e);
        }

    }

    private UUID readUUID() throws IOException {
        long least = is.readLong();
        long most = is.readLong();
        return new UUID(most, least);
    }

    private String readDecryptedParrword() throws IOException {
        int stringLength = is.readInt();
        byte[] bytes = new byte[stringLength];
        int bytesRead = 0;
        while (bytesRead != stringLength) {
            bytesRead += is.read(bytes, bytesRead, stringLength);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private void writeHashString() throws IOException {
        String hash = server.getHash();
        os.write(hash.getBytes(StandardCharsets.UTF_8));
    }

    private void writeRange(Range range) throws IOException {
        os.writeLong(range.getBegin());
        os.writeInt(range.getLength());
    }

    @Override
    public void run() {
        try {
            UUID uuid = readUUID();

            boolean passwordFound = is.readBoolean();

            if (passwordFound) {
                String password = readDecryptedParrword();
                server.setState(Server.State.FINISHED);
                server.setPassword(password);
                return;
            }
            server.notifyAboutFinishedWork(uuid);
            if (!server.hasClient(uuid)) {
                writeHashString();
                server.addClient(uuid);
            }

            Range range = server.getRange(uuid);
            writeRange(range);
        } catch (IndexOutOfBoundsException e) {
            LOGGER.trace("tried all strings, password was not found");
        } catch (IOException e) {
            LOGGER.error("input/output error");
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                LOGGER.error("unable to close socket");
            }
        }
    }
}
