package model.client;


import com.google.gson.Gson;
import model.HttpMethod;
import model.NoSuchUserException;
import model.RestMethod;
import model.entity.Message;
import model.entity.User;
import model.message.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import view.ClientForm;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Alexander on 29/05/2017.
 */
public class Client {
    private static Gson gson = new Gson();
    private final Logger LOG = LogManager.getLogger(Client.class);
    private String serverURL;
    private UUID token;
    private String username;
    private Thread messagesRequestSender;
    private int offset = 0;
    private List<User> users = new ArrayList<>();

    private List<ServerListener> listeners = new ArrayList<>();

    public static void main(String[] args) throws IOException, InterruptedException {
        Client client = new Client();
        client.setServerURL("192.168.0.20", "5000");
        new ClientLogger(client);
        new ClientForm(client).setVisible(true);
    }

    private void start() {
        messagesRequestSender = new Thread(new RequestSender(), "MessagesRequests");
        messagesRequestSender.start();
    }

    private void stop() {
        messagesRequestSender.interrupt();
    }

    public void setServerURL(String ip, String port) {
        this.serverURL = "http://" + ip + ":" + port;
    }

    public String getUsername() {
        return username;
    }

    public User user(int id) throws NoSuchUserException {
        return users.stream().filter(element -> element.getId() == id)
                .findFirst().orElseThrow(NoSuchUserException::new);
    }

    public void login(String username) throws IOException {
        HttpURLConnection connection;
        URL url = new URL(serverURL + RestMethod.LOGIN);
        connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod(HttpMethod.POST);
        connection.setRequestProperty("content-type", "application/json");
        Writer writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8);
        writer.write(gson.toJson(new LoginRequest(username)));
        writer.close();

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            Reader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8);
            LoginResponse response = gson.fromJson(reader, LoginResponse.class);
            reader.close();
            this.username = response.getUsername();
            token = response.getToken();
            listeners.forEach(ServerListener::onLoginSucceeded);
            start();
        } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
            listeners.forEach(ServerListener::onLoginFailure);
        }
    }

    public void again() throws IOException {

    }

    public void logout() throws IOException {
        URL url = new URL(serverURL + RestMethod.LOGOUT);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(HttpMethod.GET);
        connection.setRequestProperty("authorization", "Token " + token);

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            Reader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8);
            // even though we don't need data from server,
            // we still have to read the entire body for the connection
            // to be reused (see Http Persistent Connections:
            // https://docs.oracle.com/javase/6/docs/technotes/guides/net/http-keepalive.html)
            gson.fromJson(reader, LogoutResponse.class);
            reader.close();
            listeners.forEach(ServerListener::onLogoutSucceeded);
            stop();
        } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
            listeners.forEach(ServerListener::onLogoutFailure);
        }
    }

    public void getUsers() throws IOException {
        URL url = new URL(serverURL + RestMethod.GET_USERS);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(HttpMethod.GET);
        connection.setRequestProperty("authorization", "Token " + token);

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            Reader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8);
            UsersResponse response = gson.fromJson(reader, UsersResponse.class);
            reader.close();
            users = response.getUsers();
            for (ServerListener listener : listeners) {
                listener.onGetUsersSucceeded(response.getUsers());
            }
        } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
            login(username);
            listeners.forEach(ServerListener::onGetUsersFailure);
        }
    }

    public void getUser(int id) throws IOException {
        URL url = new URL(serverURL + RestMethod.GET_USER + id);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(HttpMethod.GET);
        connection.setRequestProperty("authorization", "Token " + token);

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            Reader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8);
            UserResponse response = gson.fromJson(reader, UserResponse.class);
            reader.close();
            for (ServerListener listener : listeners) {
                listener.onGetUserSucceeded(response.getUser());
            }
        } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
            listeners.forEach(ServerListener::onGetUserFailure);
        }
    }

    public void getMessages() throws IOException {
        URL url = new URL(serverURL + RestMethod.GET_MESSAGES);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(false);
        connection.setRequestMethod(HttpMethod.GET);
        connection.setRequestProperty("authorization", "Token " + token);

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            Reader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8);
            MessagesResponse response = gson.fromJson(reader, MessagesResponse.class);
            reader.close();
            List<Message> messages = response.getMessages();
            if (!messages.isEmpty()) {
                offset = messages.get(messages.size() - 1).getId() + 1;
                for (ServerListener listener : listeners) {
                    listener.onGetMessagesSucceeded(messages);
                }
            }
        } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
            listeners.forEach(ServerListener::onGetMessagesFailure);
        }
    }

    public void getMessages(int offset, int count) throws IOException {
        URL url = new URL(serverURL + RestMethod.GET_MESSAGES);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(false);
        connection.setRequestMethod(HttpMethod.GET);
        connection.setRequestProperty("authorization", "Token " + token);
        connection.setRequestProperty("offset", String.valueOf(offset));
        connection.setRequestProperty("count", String.valueOf(count));

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            Reader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8);
            MessagesResponse response = gson.fromJson(reader, MessagesResponse.class);
            reader.close();
            List<Message> messages = response.getMessages();
            if (!messages.isEmpty()) {
                this.offset = messages.get(messages.size() - 1).getId() + 1;
                for (ServerListener listener : listeners) {
                    listener.onGetMessagesSucceeded(messages);
                }
            }
        } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
            listeners.forEach(ServerListener::onGetMessagesFailure);
        }
    }

    public void sendMessage(String content) throws IOException {
        URL url = new URL(serverURL + RestMethod.POST_MESSAGE);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod(HttpMethod.POST);
        connection.setRequestProperty("authorization", "Token " + token);
        connection.setRequestProperty("content-type", "application/json");
        Writer writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8);
        writer.write(gson.toJson(new MessageRequest(content)));
        writer.close();

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            Reader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8);
            MessageResponse response = gson.fromJson(reader, MessageResponse.class);
            reader.close();
            for (ServerListener listener : listeners) {
                listener.onSendMessageSucceeded(response.getMessage());
            }
        } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
            listeners.forEach(ServerListener::onSendMessageFailure);
        }
    }

    public void addListener(ServerListener listener) {
        listeners.add(listener);
    }

    private class RequestSender implements Runnable {
        @Override
        public void run() {
            try {
                offset = 0;
                LOG.trace("request sender started");
                getUsers();
                getMessages();
                while (!Thread.interrupted()) {
                    getUsers();
                    int count = 10;
                    getMessages(offset, count);
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                LOG.trace("request sender stopped");
            } catch (IOException e) {
                listeners.forEach(ServerListener::onConnectionError);
            }
        }
    }
}
