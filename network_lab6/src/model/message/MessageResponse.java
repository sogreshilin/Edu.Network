package model.message;

/**
 * Created by Alexander on 24/11/2017.
 */
public class MessageResponse {
    private long id;
    private String message;

    public MessageResponse(long id, String message) {
        this.id = id;
        this.message = message;
    }

    public long getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }
}
