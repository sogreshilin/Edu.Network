package model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by Alexander on 01/11/2017.
 */
public class MyServerSocket {

    private final Logger LOG = LogManager.getLogger(MyServerSocket.class);

    private DatagramSocketHelper datagramSocketHelper;
    private ArrayBlockingQueue<MySocket> requests = new ArrayBlockingQueue<>(1024);
    private SocketState state = SocketState.CLOSED;
    private final Object acceptLock = new Object();
    private Thread acceptingThread;

    public MyServerSocket(int port) throws IOException {
        datagramSocketHelper = new DatagramSocketHelper(this, port);
        state = SocketState.RUNNING;
    }

    public MySocket accept() throws IOException {
        if (state == SocketState.RUNNING) {
            try {
                MySocket socket;
                synchronized (acceptLock) {
                    acceptingThread = Thread.currentThread();
                    socket = requests.take();
                    acceptingThread = null;
                }
                return socket;
            } catch (InterruptedException e) {
                throw new SocketException("socket was closed");
            }
        } else {
            throw new IOException("socket was closed");
        }
    }

    public void addClient(MySocket socket) {
        try {
            requests.put(socket);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void close() throws IOException {
        state = SocketState.CLOSED;
        if (datagramSocketHelper == null) {
            throw new IOException("socket was already closed");
        }
        datagramSocketHelper.setHelperState(SocketState.CLOSED);
        if (acceptingThread != null) {
            acceptingThread.interrupt();
            try {
                acceptingThread.join();
            } catch (InterruptedException e) {
                throw new IOException("unable to close socket correctly");
            }
        }
        datagramSocketHelper = null;
    }
}
