package lab02;


import model.MySocket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;


/**
 * Created by Alexander on 15/09/2017.
 */
public class SimpleClient {

    private FileInputStream fileInputStream;
    private MySocket socket;
    private static final int TIMEOUT = 5000;
    private static final Logger log = LogManager.getLogger(SimpleClient.class);

    private void connect(InetAddress ip, int port) {
        socket = new MySocket();
        try {
            socket.connect(new InetSocketAddress(ip, port), TIMEOUT);
        } catch (SocketTimeoutException e) {
            log.error("timeout expired", e);
            System.exit(-1);
        } catch (IOException e) {
            log.error("connecting to server error", e);
            System.exit(-1);
        }
        log.info("connected to server");
    }

    private void writeFile() throws IOException {
        byte[] buffer = new byte[Const.BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = fileInputStream.read(buffer)) > 0) {
            socket.write(buffer, 0, bytesRead);
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        }
    }

    private void sendFile(String fileName) {
        try {
            fileInputStream = new FileInputStream(fileName);
        } catch (FileNotFoundException e) {
            log.error("file not found", e);
            System.exit(-1);
        }
        File file = new File(fileName);
        try {
            socket.writeInt(file.getName().length());
            byte[] bytes = file.getName().getBytes(StandardCharsets.UTF_8);
            socket.write(bytes, 0, bytes.length);
            socket.writeLong(file.length());
            writeFile();
        } catch (IOException e) {
            log.warn("file sending error", e);
            return;
        }
        log.info("file successfully sent");
    }

    private void receiveStatus() {
        try {
            int status = socket.readInt();
            switch (status) {
                case Const.SUCCESS : {
                    log.info("server received file");
                    break;
                }
                case Const.FAILURE: {
                    log.error("server failed to receive file");
                    break;
                }
                default: {
                    log.warn("invalid message from server");
                }
            }
        } catch (IOException e) {
            log.warn("reading from socket error", e);
        }
    }

    public static void main(String[] args) {
        InetAddress address;
        try {
            address = InetAddress.getByName(args[1]);
        } catch (UnknownHostException e) {
            log.error("invalid ip error", e);
            return;
        }
        String fileName = null;
        try {
            fileName = new BufferedReader(new InputStreamReader(System.in)).readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
//        String fileName = args[0];
        int port = Integer.parseInt(args[2]);

        SimpleClient client = new SimpleClient();
        client.connect(address, port);
        client.sendFile(fileName);
        client.receiveStatus();

    }

}
