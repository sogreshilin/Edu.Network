package model;

import java.net.DatagramPacket;
import java.util.UUID;

/**
 * Created by Alexander on 06/10/2017.
 */
public class Packet {
    private UUID uuid;
    private DatagramPacket datagram;
    private long lastAttempt;
    private boolean once;

    Packet(UUID uuid) {
        this.uuid = uuid;
    }

    Packet(UUID uuid, DatagramPacket datagram, long lastAttempt, boolean once) {
        this.uuid = uuid;
        this.once = once;
        this.datagram = datagram;
        this.lastAttempt = lastAttempt;
    }

    public UUID uuid() {
        return uuid;
    }

    DatagramPacket datagram() {
        return datagram;
    }

    long lastAttempt() {
        return lastAttempt;
    }

    boolean once() {
        return once;
    }

    void setLastAttempt(long lastAttempt) {
        this.lastAttempt = lastAttempt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Packet packet = (Packet) o;

        return uuid.equals(packet.uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    @Override
    public String toString() {
        return uuid.toString();
    }
}
