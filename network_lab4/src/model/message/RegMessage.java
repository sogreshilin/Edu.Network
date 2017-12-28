package model.message;

import model.MessageHandler;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Arrays;

/**
 * Created by Alexander on 25/09/2017.
 */
@XmlRootElement(name = AbstractMessage.REG)
public class RegMessage extends AbstractMessage implements Serializable {
    private byte[] body;

    public byte[] getBody() {
        return body;
    }

    public RegMessage() {}

    public RegMessage(byte[] body) {
        super();
        this.body = body;
    }

    @XmlElement(name = MSG_BODY)
    public void setBody(byte[] body) {
        this.body = body;
    }

    @Override
    public void process(MessageHandler handler) {
        handler.process(this);
    }

    @Override
    public String toString() {
        return "RegMessage@" + this.hashCode() + "[" + body.length + "]";
    }
}
