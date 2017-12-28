package model;

import javafx.util.Pair;
import model.message.AbstractMessage;
import model.message.Message;
import model.serializing.MessageDeserializer;
import model.serializing.XmlMessageDeserializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Alexander on 25/09/2017.
 */
public class Receiver implements Runnable {
    private static final int HISTORY_SIZE = 256;

    private DatagramSocket socket;
    private MessageDeserializer deserializer;
    private Controller controller;
    private List<Pair<UUID, InetSocketAddress>> history = new ArrayList<>(HISTORY_SIZE);

    private static final Logger LOG = LogManager.getLogger(Receiver.class);
    private static final int BUFFER_SIZE = 1024 * 1024;

    public Receiver(Controller controller) {
        this.controller = controller;
        socket = controller.socket();
        deserializer = new XmlMessageDeserializer();
    }

    private void process(AbstractMessage message) {
        if (history.contains(new Pair<>(message.getUuid(), message.getSender()))) {
            LOG.trace("resending {} message", Message.ACK);
            controller.handler().acknowledge(message.getUuid(), message.getSender());
        } else {
            if (history.size() == HISTORY_SIZE) {
                history.remove(0);
            }
            history.add(new Pair<>(message.getUuid(), message.getSender()));
            message.process(controller.handler());
        }
    }

    @Override
    public void run() {
        byte[] buf = new byte[BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        try {
            while (!Thread.interrupted()) {
                socket.receive(packet);
                if (Math.random() * 100 > controller.loss()) {
                    AbstractMessage message = (AbstractMessage)
                            deserializer.deserialize(packet.getData(),
                                    packet.getLength());
                    message.setSender(new InetSocketAddress(packet.getAddress(), packet.getPort()));
                    this.process(message);
                } else {
                LOG.trace("package lost");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
