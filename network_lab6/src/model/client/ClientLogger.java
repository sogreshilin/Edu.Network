package model.client;

import model.entity.Message;
import model.entity.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Created by Alexander on 25/11/2017.
 */
public class ClientLogger implements ServerListener {

    private Client client;
    private final Logger LOG = LogManager.getLogger(ClientLogger.class);

    public ClientLogger(Client client) {
        this.client = client;
        client.addListener(this);
    }

    @Override
    public void onLoginSucceeded() {
        LOG.trace("login succeeded");
    }

    @Override
    public void onLoginFailure() {
        LOG.trace("login failure");
    }

    @Override
    public void onLogoutSucceeded() {
        LOG.trace("logout succeeded");
    }

    @Override
    public void onLogoutFailure() {
        LOG.trace("logout failure");
    }

    @Override
    public void onGetUsersSucceeded(List<User> users) {
        LOG.trace("users: {}", users);
    }

    @Override
    public void onGetUsersFailure() {
        LOG.trace("unable to get users");
    }

    @Override
    public void onGetUserSucceeded(User user) {
        LOG.trace("user[{}]: {}", user.getId(), user.getUsername());
    }

    @Override
    public void onGetUserFailure() {
        LOG.trace("unable to get user");
    }

    @Override
    public void onGetMessagesSucceeded(List<Message> messages) {
        LOG.trace("messages: {}", messages);
    }

    @Override
    public void onGetMessagesFailure() {
        LOG.trace("unable to get messages");
    }

    @Override
    public void onSendMessageSucceeded(String message) {
        LOG.trace("message \"{}\" sent", message);
    }

    @Override
    public void onSendMessageFailure() {
        LOG.trace("unable to send message");
    }

    @Override
    public void onConnectionError() {
        LOG.error("server unavailable");
    }

}
