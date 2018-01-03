package model.message;

import model.entity.Message;

import java.util.List;

/**
 * Created by Alexander on 23/11/2017.
 */
public class MessagesResponse {
    private List<Message> messages;

    public MessagesResponse(List<Message> messages) {
        this.messages = messages;
    }

    public List<Message> getMessages() {
        return messages;
    }
}
