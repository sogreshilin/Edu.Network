package model.message;

import java.util.UUID;

/**
 * Created by Alexander on 23/11/2017.
 */
public class LoginResponse {
    private int id;
    private String username;
    private boolean online;
    private UUID token;

    public LoginResponse(int id, String username, boolean online, UUID token) {
        this.id = id;
        this.username = username;
        this.online = online;
        this.token = token;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public boolean isOnline() {
        return online;
    }

    public UUID getToken() {
        return token;
    }

    @Override
    public String toString() {
        return "LoginResponse{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", online=" + online +
                ", token=" + token +
                '}';
    }
}
