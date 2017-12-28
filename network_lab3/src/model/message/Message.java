package model.message;

import model.MessageHandler;

/**
 * Created by Alexander on 30/09/2017.
 */
public interface Message {
    /**
     *
     * names of message types to be serialized
     *
     */
    String ACK = "ack";
    String JOIN = "join";
    String LEAVE = "leave";
    String TEXT = "text";
    String CHANGE_PARENT = "parent_change";


    /**
     *
     * names of message fields to be serialized
     *
     */
    String PARENT_IP = "parent_ip";
    String PARENT_PORT = "parent_port";
    String MSG_ID = "message_id";
    String MSG_SENDER_NAME = "sender_name";
    String MSG_CONTENT = "content";
    String TO_NEW_PARENT = "to_new_parent";


    /**
     *
     * @param handler processes the message depending
     *                       on message type
     *
     */
    void process(MessageHandler handler);
}
