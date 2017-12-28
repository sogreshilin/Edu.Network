import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/**
 * Created by Alexander on 15/09/2017.
 */
public class Server {

    private ServerSocket serverSocket;
    private Thread acceptor;
    private static final Logger log = LogManager.getLogger(Server.class);

    public Server(int port) {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            log.error("server socket creating error", e);
            System.exit(1);
        }
        log.info("server socket created");
    }

    public void start() {
        acceptor = new Thread(new ClientAcceptor(), "Acceptor");
        acceptor.start();
    }

    public static void main(String[] args) {
        new Server(Integer.parseInt(args[0])).start();
    }

    private class ClientAcceptor implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    log.info("new client accepted");
//                    socket.setKeepAlive(true);
                    new Thread(new ClientHandler(socket)).start();
                } catch (IOException e) {
                    log.info("IO error while waiting for connection", e);
                }
            }
        }
    }

    private class ClientHandler implements Runnable {
        private Socket socket;
        private DataInputStream inputStream;
        private DataOutputStream outputStream;

        private static final String DIR_PATH = "./uploads/";

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        private void prepare() {
            try {
                inputStream = new DataInputStream(socket.getInputStream());
                outputStream = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                log.info("getting stream error", e);
            }
        }

        private byte[] readFileName() throws IOException {
            int length = inputStream.readInt();
            byte[] buffer = new byte[length];
            inputStream.read(buffer, 0, length);
            return buffer;
        }

        private FileOutputStream getFileOutputStream() throws IOException {
            String fileName = new String(readFileName(), StandardCharsets.UTF_8);
            File file = new File(DIR_PATH + new File(fileName).getName());
            log.info("created file " + DIR_PATH + file.getName());
            file.getParentFile().mkdirs();
            file.createNewFile();
            return new FileOutputStream(file);
        }

        private long readFileLength() throws IOException {
            long length = inputStream.readLong();
            if (length < 0) {
                log.warn("negative file size");
                throw new IOException("negative file size");
            }
            log.info("receiving file " + length + " bytes");
            return length;
        }

        private long bps(long bytes, long period) {
            return bytes * 8 * 1000 / period;
        }

        private long kbps(long bytes, long period) {
            return bytes * 8 * 1000 / 1024 / period;
        }

        private long mbps(long bytes, long period) {
            return bytes * 8 * 1000 / 1024 / 1024 / period;
        }

        private int readDataToFile() {
            try (FileOutputStream fileOutputStream = getFileOutputStream()) {
                long length = readFileLength();

                byte[] buffer = new byte[Const.BUFFER_SIZE];
                int bytesRead = 0;
                long bytesReceived = 0;
                long bytesReceivedPrev = 0;

                long startTime = System.currentTimeMillis();
                long prevTime = System.currentTimeMillis();
                long currentTime = 0;

                while (bytesReceived < length && (bytesRead = inputStream.read(buffer, 0, buffer.length)) > 0) {
                    if ((currentTime = System.currentTimeMillis()) - prevTime > Const.TIMEOUT) {
                        log.info("speed: current = {} Mbps, average = {} Mbps",
                                mbps(bytesReceived - bytesReceivedPrev, currentTime - prevTime),
                                mbps(bytesReceived, currentTime - startTime));
                        bytesReceivedPrev = bytesReceived;
                        prevTime = currentTime;
                    }

                    bytesReceived += bytesRead;
                    fileOutputStream.write(buffer, 0, bytesRead);
                }

                log.info("received all file, average speed = {} Mbps", mbps(length, System.currentTimeMillis() - startTime));
            } catch (IOException e) {
                log.info("reading from socket error", e);
                return -1;
            }
            return 0;
        }

        @Override
        public void run() {
            try {
                prepare();
                if (readDataToFile() != 0) {
                    outputStream.writeInt(Const.FAILURE);
                } else {
                    outputStream.writeInt(Const.SUCCESS);
                    log.info("sent success message");
                }
            } catch (IOException e) {
                log.warn("writing to socket error", e);
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    log.warn("closing socket error", e);
                }
            }
        }
    }

}
