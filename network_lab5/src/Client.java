import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;


public class Client implements Runnable {
    private static final Logger LOGGER = LogManager.getLogger(Client.class);
    private static final int MAX_CONNECT_ATTEMPTS = 5;
    private static final int PRINT_TIMEOUT = 3000;

    private DataInputStream is;
    private DataOutputStream os;
    private InetSocketAddress address;
    private Socket socket;
    private UUID uuid = UUID.randomUUID();

    Client(InetSocketAddress address) {
        this.address = address;
    }

    private static void printUsage() {
        System.err.println("Usage: ./md5_client_decrypter server_ip server_port");
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            printUsage();
            return;
        }
        int port;
        try {
            port = Integer.parseInt(args[1]);
            if (port < 1024 || port >= (1 << 16)) {
                printUsage();
                LOGGER.error("Invalid port value");
                return;
            }
        } catch (NumberFormatException e) {
            printUsage();
            LOGGER.error("Unable to parse port");
            return;
        }
        InetSocketAddress address = new InetSocketAddress(args[0], port);
        Client client = new Client(address);
        client.run();
    }

    private void connectToServer() {
        for (int i = 0; i < MAX_CONNECT_ATTEMPTS; ++i) {
            socket = new Socket();
            try {
                socket.connect(address, 1000);
                is = new DataInputStream(socket.getInputStream());
                os = new DataOutputStream(socket.getOutputStream());
                return;
            } catch (SocketTimeoutException e) {
                LOGGER.trace("unable to access server, attempt {}/{}", i + 1, MAX_CONNECT_ATTEMPTS);
            } catch (IOException e) {
                LOGGER.error("connecting to server error");
                try {
                    socket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                System.exit(-1);
            }
        }
        System.exit(-1);
    }

    private void writeUUID() throws IOException {
        os.writeLong(uuid.getLeastSignificantBits());
        os.writeLong(uuid.getMostSignificantBits());
    }

    private void writeDecryptedPassword(String password) throws IOException {
        os.writeBoolean(true);
        os.writeInt(password.length());
        os.write(password.getBytes(StandardCharsets.UTF_8));
    }

    private void requestTaskFromServer() throws IOException {
        os.writeBoolean(false);
    }

    private String readHashString() throws IOException {
        byte[] hashBytes = new byte[Constants.HASH_LENGTH];
        int bytesRead = 0;
        while (bytesRead != Constants.HASH_LENGTH) {
            bytesRead += is.read(hashBytes, bytesRead, Constants.HASH_LENGTH);
        }
        return new String(hashBytes);
    }

    private Range readRange() throws IOException {
        long rangeBegin = is.readLong();
        int rangeLength = is.readInt();
        return new Range(rangeBegin, rangeLength);
    }

    @Override
    public void run() {
        boolean passwordFound = false;
        boolean firstIteration = true;
        String decryptedPassword = "";
        String hash = "";
        Range range = new Range(0, 0);
        while (true) {
            connectToServer();
            try {
                writeUUID();
                if (passwordFound) {
                    writeDecryptedPassword(decryptedPassword);
                    return;
                } else {
                    requestTaskFromServer();
                }

                if (firstIteration) {
                    hash = readHashString();
                    firstIteration = false;
                }
                range = readRange();
            } catch (IOException e) {
                LOGGER.error("server closed socket");
                return;
            } finally {
                try {
                    socket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }


            long candidateTime = 0;

            for (String password : range) {
                if (System.currentTimeMillis() - candidateTime > PRINT_TIMEOUT) {
                    LOGGER.info("candidate : {}", password);
                    candidateTime = System.currentTimeMillis();
                }
                String hashed = MD5Hash.getHash(password);
                if (hashed.equals(hash)) {
                    LOGGER.info("password decrypted : {}", password);
                    decryptedPassword = password;
                    passwordFound = true;
                    break;
                }
            }

        }


    }
}
