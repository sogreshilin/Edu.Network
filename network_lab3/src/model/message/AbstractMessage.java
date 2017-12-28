package model.message;

import model.MessageHandler;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import java.net.InetSocketAddress;
import java.util.UUID;

/**
 * Created by Alexander on 25/09/2017.
 */
public abstract class AbstractMessage implements Message {
    private InetSocketAddress sender;
    private InetSocketAddress receiver;
    private UUID uuid;

    @Override
    public abstract void process(MessageHandler handler);

    @XmlTransient
    public void setSender(InetSocketAddress sender) {
        this.sender = sender;
    }

    @XmlTransient
    public void setReceiver(InetSocketAddress receiver) {
        this.receiver = receiver;
    }

    public InetSocketAddress getSender() {
        return sender;
    }

    public InetSocketAddress getReceiver() {
        return receiver;
    }

    @XmlElement(name = Message.MSG_ID)
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }
}
