package model.message;

import model.MessageHandler;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * Created by Alexander on 13/11/2017.
 */

@XmlRootElement(name = AbstractMessage.SYNACK)
public class SynAckMessage extends AbstractMessage implements Serializable {
    @Override
    public void process(MessageHandler handler) {
        handler.process(this);
    }
}
