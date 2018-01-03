package model.message;

import model.entity.User;

/**
 * Created by Alexander on 23/11/2017.
 */
public class UserResponse {
    private User user;

    public UserResponse(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
