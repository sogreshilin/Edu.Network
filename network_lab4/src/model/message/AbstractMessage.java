package model.message;

import model.MessageHandler;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import java.net.InetSocketAddress;
import java.util.UUID;

/**
 * Created by Alexander on 25/09/2017.
 */
public abstract class AbstractMessage {

    public static final String ACK = "ack";
    public static final String SYN = "syn";
    public static final String FIN = "fin";
    public static final String REG = "reg";
    public static final String SYNACK = "synack";
    public static final String FINACK = "finack";

    public static final String MSG_ID = "message_id";
    public static final String MSG_BODY = "body";
    public static final String SEQ_NUMBER = "seq_number";

    private InetSocketAddress sender;
    private InetSocketAddress receiver;
    private UUID uuid;

    private int sequenceNumber;

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    @XmlElement(name = AbstractMessage.SEQ_NUMBER)
    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public abstract void process(MessageHandler handler);

    public InetSocketAddress getSender() {
        return sender;
    }

    @XmlTransient
    public void setSender(InetSocketAddress sender) {
        this.sender = sender;
    }

    public InetSocketAddress getReceiver() {
        return receiver;
    }

    @XmlTransient
    public void setReceiver(InetSocketAddress receiver) {
        this.receiver = receiver;
    }

    public UUID getUuid() {
        return uuid;
    }

    @XmlElement(name = AbstractMessage.MSG_ID)
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;

        AbstractMessage message = (AbstractMessage) o;

        return uuid.equals(message.uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }
}
