package model;

import model.message.*;

/**
 * Created by Alexander on 13/11/2017.
 */
public interface MessageHandler {
    void process(SynMessage message);
    void process(AckMessage message);
    void process(RegMessage message);
    void process(FinMessage message);
    void process(FinAckMessage message);
    void process(SynAckMessage message);
}
