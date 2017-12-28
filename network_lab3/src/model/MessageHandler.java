package model;

import jdk.nashorn.internal.objects.annotations.Constructor;
import lombok.Getter;
import lombok.Setter;
import model.message.*;
import model.serializing.MessageSerializer;
import model.serializing.XmlMessageSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.UUID;

/**
 * Created by Alexander on 06/10/2017.
 */
public class MessageHandler {
    private final Sender sender;
    private final Controller controller;
    private MessageSerializer serializer;

    private static final Logger LOG = LogManager.getLogger(MessageHandler.class);

    MessageHandler(Controller controller) {
        this.sender = controller.getSender();
        this.controller = controller;
        this.serializer = new XmlMessageSerializer();
    }

    void addToSender(AbstractMessage message) {
        addToSender(message, false);
    }

    private void addToSender(AbstractMessage message, boolean once) {
        byte[] data = serializer.serialize(message);
        sender.add(message.getUuid(), new DatagramPacket(data,
                        data.length, message.getReceiver().getAddress(),
                        message.getReceiver().getPort()), once);

    }

    public void process(AckMessage msg) {
        LOG.trace("received {} message from {}", Message.ACK, msg.getSender());
        sender.remove(msg.getUuid());
        controller.removeFromLastMessages(msg.getUuid());
    }

    public void process(ChangeParentMessage msg) {
        LOG.trace("received {} message from {}", Message.CHANGE_PARENT, msg.getSender());
        try {
            InetAddress parentIP = InetAddress.getByName(msg.getParentIP());
            int parentPort = msg.getParentPort();
            InetSocketAddress newParent = new InetSocketAddress(parentIP, parentPort);
            if (msg.getToNewParent()) {
                controller.setAmIRoot(true);
                controller.setParent(null);
            } else {
                controller.setParent(newParent);
            }
            if (controller.parent() != null) {
                JoinMessage message = new JoinMessage();
                message.setUuid(UUID.randomUUID());
                message.setReceiver(controller.parent());
                addToSender(message, false);
            }
            LOG.trace("changed parent to {}", controller.parent());
            acknowledge(msg.getUuid(), msg.getSender());
        } catch (UnknownHostException e) {
            LOG.error("invalid parent address", e);
        }
    }

    public void process(JoinMessage msg) {
        LOG.trace("received {} message from {}", Message.JOIN, msg.getSender());
        InetSocketAddress node = msg.getSender();
        if (!controller.children().contains(node)) {
            controller.children().add(node);
        }
        LOG.trace("added child {} to the children list {}", node, controller.children());
        acknowledge(msg.getUuid(), node);
    }

    public void process(LeaveMessage msg) {
        LOG.trace("received {} message from {}", Message.LEAVE, msg.getSender());
        InetSocketAddress node = msg.getSender();
        if (controller.children().contains(node)) {
            controller.children().remove(node);
            LOG.trace("removed child {} from the children list {}", node, controller.children());
        }
        acknowledge(msg.getUuid(), node);
    }

    public void process(TextMessage msg) {
        LOG.trace("received {} message from {}", Message.TEXT, msg.getSender());
        InetAddress ip = msg.getSender().getAddress();
        int port = msg.getSender().getPort();
        InetSocketAddress node = new InetSocketAddress(ip, port);
        acknowledge(msg.getUuid(), node);
        for (Controller.Subscriber subscriber : controller.getSubscribers()) {
            subscriber.process(msg.getName(), msg.getText());
        }
        resendTextMessageToAllExceptSender(msg);
    }

    void acknowledge(UUID uuid, InetSocketAddress node) {
        AckMessage response = new AckMessage();
        response.setUuid(uuid);
        response.setReceiver(node);
        addToSender(response, true);
    }

    void sendTextMessageToAll(String line) {
        for (InetSocketAddress child : controller.neighbors()) {
            TextMessage message = new TextMessage();
            message.setText(line);
            message.setName(controller.name());
            message.setUuid(UUID.randomUUID());
            message.setReceiver(child);
            addToSender(message, false);
        }
    }

    private void resendTextMessageToAllExceptSender(TextMessage message) {
        for (InetSocketAddress child : controller.neighbors()) {
            if (!child.equals(message.getSender())) {
                message.setReceiver(child);
                addToSender(message, false);
            }
        }

    }
}
