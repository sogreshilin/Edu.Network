package model.message;

import lombok.Getter;
import model.MessageHandler;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * Created by Alexander on 25/09/2017.
 */
@XmlRootElement(name = Message.CHANGE_PARENT)
public class ChangeParentMessage extends AbstractMessage implements Serializable {

    private String parentIP;
    private int parentPort;
    private boolean toNewParent;

    public String getParentIP() {
        return parentIP;
    }

    public int getParentPort() {
        return parentPort;
    }

    public boolean getToNewParent() {
        return toNewParent;
    }

    @XmlElement(name = Message.PARENT_IP)
    public void setParentIP(String parentIP) {
        this.parentIP = parentIP;
    }

    @XmlElement(name = Message.PARENT_PORT)
    public void setParentPort(int parentPort) {
        this.parentPort = parentPort;
    }

    @XmlElement(name = Message.TO_NEW_PARENT)
    public void setToNewParent(boolean toNewParent) {
        this.toNewParent = toNewParent;
    }

    @Override
    public void process(MessageHandler handler) {
        handler.process(this);
    }
}

