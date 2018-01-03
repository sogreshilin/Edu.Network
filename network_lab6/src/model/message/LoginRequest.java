package model.message;

/**
 * Created by Alexander on 23/11/2017.
 */
public class LoginRequest {
    private String username;

    public LoginRequest(String username) {
        if (username == null) {
            throw new NullPointerException();
        }
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}

