package model.message;

import model.MessageHandler;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * Created by Alexander on 25/09/2017.
 */
@XmlRootElement(name = Message.LEAVE)
public class LeaveMessage extends AbstractMessage implements Serializable {
    @Override
    public void process(MessageHandler handler) {
        handler.process(this);
    }
}
