package model.client;

import model.entity.Message;
import model.entity.User;

import java.util.List;

/**
 * Created by Alexander on 25/11/2017.
 */
public interface ServerListener {
    void onLoginSucceeded();
    void onLoginFailure();
    void onLogoutSucceeded();
    void onLogoutFailure();
    void onGetUsersSucceeded(List<User> users);
    void onGetUsersFailure();
    void onGetUserSucceeded(User user);
    void onGetUserFailure();
    void onGetMessagesSucceeded(List<Message> messages);
    void onGetMessagesFailure();
    void onSendMessageSucceeded(String message);
    void onSendMessageFailure();
    void onConnectionError();
}
