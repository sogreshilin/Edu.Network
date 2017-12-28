package model;

import model.message.ChangeParentMessage;
import model.message.JoinMessage;
import model.message.LeaveMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Alexander on 25/09/2017.
 */
public class Controller {
    private Sender sender;
    private final int loss;
    private Boolean amIRoot;
    private final String name;
    private DatagramSocket socket;
    private MessageHandler handler;
    private InetSocketAddress parent;
    private InetSocketAddress myself;
    private List<UUID> lastMessages = new ArrayList<>();
    private List<Subscriber> subscribers = new ArrayList<>();
    private List<InetSocketAddress> children = new ArrayList<>();

    private final Object lock = new Object();

    private static final Logger LOG = LogManager.getLogger(Controller.class);

    Controller(Config config) throws SocketException {
        this.name = config.getName();
        this.loss = config.getLoss();
        this.amIRoot = config.getParent() == null;
        this.parent = config.getParent();
        this.socket = new DatagramSocket(config.getPort());
        this.sender = new Sender(socket);
        this.handler = new MessageHandler(this);
    }

    public void handle(String outgoingMessageText) {
        if (outgoingMessageText.toLowerCase().equals("exit")) {
            sendChangeParentMsg();
            sendLeaveMsg();
        } else {
            for (Subscriber subscriber : subscribers) {
                subscriber.process(name, outgoingMessageText);
            }
            handler.sendTextMessageToAll(outgoingMessageText);
        }
    }

    void joinChat() {
        if (!amIRoot) {
            JoinMessage joinMessage = new JoinMessage();
            joinMessage.setUuid(UUID.randomUUID());
            joinMessage.setReceiver(parent);
            handler.addToSender(joinMessage);
        }
        new Thread(new Receiver(this), "Receiver").start();
        new Thread(sender, "Sender").start();
    }

    void sendChangeParentMsg() {
        InetSocketAddress newParent = null;
        if (amIRoot) {
            if (!children.isEmpty()) {
                newParent = children.get(0);
            } else {
                System.exit(0);
            }
        } else {
            newParent = parent;
        }
        for (InetSocketAddress child : children) {
            ChangeParentMessage message = new ChangeParentMessage();
            message.setToNewParent(child == newParent);
            message.setParentIP(newParent.getAddress().getHostAddress());
            message.setParentPort(newParent.getPort());
            message.setUuid(UUID.randomUUID());
            message.setReceiver(child);
            synchronized (lock) {
                lastMessages.add(message.getUuid());
            }
            handler.addToSender(message);
        }
    }

    void sendLeaveMsg() {
        if (!amIRoot) {
            LeaveMessage message = new LeaveMessage();
            message.setUuid(UUID.randomUUID());
            synchronized (lock) {
                lastMessages.add(message.getUuid());
            }
            message.setReceiver(parent);
            handler.addToSender(message);
        }
    }

    void removeFromLastMessages(UUID uuid) {
        synchronized (lock) {
            if (getLastMessages().contains(uuid)) {
                getLastMessages().remove(uuid);
                if (getLastMessages().isEmpty()) {
                    System.exit(0);
                }
            }
        }
    }

    /*******************************************************
     *
     * SUBSCRIBERS
     *
     *******************************************************/

    public interface Subscriber {
        void process(String name, String text);
    }

    public void addSubscriber(Subscriber subscriber) {
        subscribers.add(subscriber);
    }

    List<Subscriber> getSubscribers() {
        return subscribers;
    }


    /*******************************************************
     *
     * GETTERS and SETTERS
     *
     *******************************************************/

    int loss() {
        return loss;
    }

    InetSocketAddress parent() {
        return parent;
    }

    List<InetSocketAddress> children() {
        return children;
    }

    MessageHandler handler() {
        return handler;
    }

    public String name() {
        return name;
    }

    InetSocketAddress myself() {
        return myself;
    }

    DatagramSocket socket() {
        return socket;
    }

    void setParent(InetSocketAddress parent) {
        this.parent = parent;
    }

    void setAmIRoot(Boolean amIRoot) {
        this.amIRoot = amIRoot;
    }

    Sender getSender() {
        return sender;
    }

    List<UUID> getLastMessages() {
        return lastMessages;
    }

    List<InetSocketAddress> neighbors() {
        List<InetSocketAddress> neighbors = new ArrayList<>();
        for (InetSocketAddress child : children) {
            neighbors.add(child);
        }
        if (parent != null) {
            neighbors.add(parent);
        }
        return neighbors;
    }

}
