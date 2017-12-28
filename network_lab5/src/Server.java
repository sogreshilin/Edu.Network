import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by Alexander on 03/11/2017.
 */
public class Server implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(Server.class);
    private static final int QUEUE_CAPACITY = 100;
    private static final int MAX_WORK_TIME = 10_000;
    private static final int SERVER_PORT = 5000;
    private static final int SO_TIMEOUT = 3000;
    private final Object lock = new Object();
    private State state = State.WORKING;
    private String hash;
    private int rangeBegin;
    private String password = "";
    private Map<UUID, RangeData> rangeDataHashMap = new LinkedHashMap<>();
    private ServerSocket serverSocket;
    private BlockingQueue<RangeData> unfinishedTasks = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

    Server(String hash, int port) {
        this.hash = hash;
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(SO_TIMEOUT);
        } catch (IOException e) {
            LOGGER.error("input/output error while creating server socket");
        }

    }

    private static void printUsage() {
        System.err.println("Usage: ./md5_server_decrypter md5_hash port");
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            printUsage();
            return;
        }
        String hash = args[0];
        int port;
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            printUsage();
            LOGGER.error("unable to parse port");
            return;
        }
        new Server(MD5Hash.getHash("TTTTTTTTTTT"), SERVER_PORT).run();
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public void run() {
        new Thread(() -> {
            while (!finished()) {
                try {
                    Socket socket = serverSocket.accept();
                    new Thread(() -> new ClientHandler(socket, this)).start();
                } catch (IOException e) {
                    // this exception is thrown when SO_TIMEOUT expired
                }
            }
            LOGGER.info("password decrypted: {}", password);
        }).run();
    }

    String getHash() {
        return hash;
    }

    void setState(State state) {
        this.state = state;
    }

    boolean finished() {
        return state == State.FINISHED;
    }

    boolean hasClient(UUID uuid) {
        synchronized (lock) {
            return rangeDataHashMap.containsKey(uuid);
        }
    }

    void notifyAboutFinishedWork(UUID uuid) {
        synchronized (lock) {
            unfinishedTasks.remove(new RangeData(uuid, null, 0));
        }
    }

    void addClient(UUID uuid) {
        synchronized (lock) {
            rangeDataHashMap.put(uuid, null);
        }
    }

    Range getRange(UUID uuid) {
        RangeData rangeData = null;
        if (!unfinishedTasks.isEmpty()) {
            synchronized (lock) {
                if (System.currentTimeMillis() - unfinishedTasks.peek().timeWhenTaskWasGiven > MAX_WORK_TIME) {
                    try {
                        rangeData = unfinishedTasks.take();
                        rangeData.uuid = uuid;
                        rangeData.timeWhenTaskWasGiven = System.currentTimeMillis();
                        unfinishedTasks.put(rangeData);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        }

        if (rangeData != null) {
            return rangeData.range;
        }
        Range range;
        synchronized (lock) {
            if (rangeBegin > RangeIterator.LAST_STRING_NUMBER) {
                setState(State.FINISHED);
                throw new IndexOutOfBoundsException();
            }
            long rangeLength = Long.min(RangeIterator.LAST_STRING_NUMBER - rangeBegin, Constants.RANGE_LENGTH);
            range = new Range(rangeBegin, (int) rangeLength);
            rangeBegin += Constants.RANGE_LENGTH;
        }
        unfinishedTasks.add(new RangeData(uuid, range, System.currentTimeMillis()));
        return range;
    }

    enum State {WORKING, FINISHED}

    private class RangeData {
        UUID uuid;
        Range range;
        long timeWhenTaskWasGiven;

        public RangeData(UUID uuid, Range range, long timeWhenTaskWasGiven) {
            this.uuid = uuid;
            this.range = range;
            this.timeWhenTaskWasGiven = timeWhenTaskWasGiven;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            RangeData rangeData = (RangeData) o;

            return uuid.equals(rangeData.uuid);
        }

        @Override
        public int hashCode() {
            return uuid.hashCode();
        }

        @Override
        public String toString() {
            return String.valueOf(uuid.getLeastSignificantBits());
        }
    }
}
