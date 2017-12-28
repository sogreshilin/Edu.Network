package model.message;

import model.MessageHandler;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * Created by Alexander on 25/09/2017.
 */
@XmlRootElement(name = Message.TEXT)
public class TextMessage extends AbstractMessage implements Serializable {
    private String name;
    private String text;

    public String getName() {
        return name;
    }

    public String getText() {
        return text;
    }

    @XmlElement(name = MSG_SENDER_NAME)
    public void setName(String name) {
        this.name = name;
    }

    @XmlElement(name = MSG_CONTENT)
    public void setText(String text) {
        this.text = text;
    }

    @Override
    public void process(MessageHandler handler) {
        handler.process(this);
    }
}
