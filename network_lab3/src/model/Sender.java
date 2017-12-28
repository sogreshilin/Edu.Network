package model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Alexander on 02/10/2017.
 */
public class Sender implements Runnable {
    private static final int TIMEOUT = 300;
    private static final Logger LOG = LogManager.getLogger(Sender.class);

    private DatagramSocket socket;
    private List<Packet> list = new ArrayList<>();
    private final Object lock = new Object();

    public Sender(DatagramSocket socket) {
        this.socket = socket;
    }

    private void send(Packet packet) {
        try {
            long currentTime = System.currentTimeMillis();
            long timeLeft = TIMEOUT - (currentTime - packet.lastAttempt());
            if (timeLeft > 0) {
                Thread.sleep(timeLeft);
            }
            socket.send(packet.datagram());
            packet.setLastAttempt(System.currentTimeMillis());
            LOG.trace("datagram sent to {}", packet.datagram().getSocketAddress());

        } catch (InterruptedException | IOException e) {
            LOG.error(e);
        }
    }

    void remove(UUID uuid) {
        synchronized (lock) {
            list.remove(new Packet(uuid));
        }
    }

    void add(UUID uuid, DatagramPacket datagram, boolean once) {
        Packet packet = new Packet(uuid, datagram, 0, once);
        synchronized (lock) {
            list.add(0, packet);
            lock.notifyAll();
        }
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                Packet packet;
                synchronized (lock) {
                    while (list.isEmpty()) {
                        lock.wait();
                    }
                    packet = list.remove(0);
                    if (!packet.once()) {
                        list.add(packet);
                    }
                }
                send(packet);
            }
        } catch (InterruptedException e) {
            LOG.error("interrupted", e);
        }
    }
}