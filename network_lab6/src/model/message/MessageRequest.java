package model.message;

/**
 * Created by Alexander on 24/11/2017.
 */
public class MessageRequest {
    private String message;

    public MessageRequest(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "MessageRequest{" +
                "message='" + message + '\'' +
                '}';
    }
}
