package model.server.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import model.HttpMethod;
import model.server.Server;
import model.entity.User;
import model.message.LoginRequest;
import model.message.LoginResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.UUID;


/**
 * Created by Alexander on 24/11/2017.
 */
public class LoginHandler implements HttpHandler {

    private final Logger LOG = LogManager.getLogger(LoginHandler.class);
    private Gson gson = new Gson();
    private Server server;

    public LoginHandler(Server server) {
        this.server = server;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        try {
            if (httpExchange.getRequestMethod().equals(HttpMethod.POST)) {
                InputStreamReader reader = new InputStreamReader(httpExchange.getRequestBody(), StandardCharsets.UTF_8);
                LoginRequest request = gson.fromJson(reader, LoginRequest.class);

                String username = request.getUsername();
                User user = new User(username);
                UUID token = UUID.randomUUID();

                boolean wasAdded = server.addUser(token, user);
                if (wasAdded) {
                    LOG.trace("add new user: {}", username);
                    LoginResponse response = new LoginResponse(user.getId(), user.getUsername(), user.isOnline(), token);
                    byte[] body = gson.toJson(response).getBytes(StandardCharsets.UTF_8);

                    httpExchange.getResponseHeaders().add("Content-Type", "application/json");
                    server.sendResponse(httpExchange, HttpURLConnection.HTTP_OK, body);
                } else {
                    LOG.trace("unable to add user: user with name \"{}\" already exists", username);
                    httpExchange.getResponseHeaders().add("WWW-Authenticate", "Token realm=\'Username is already in use\'");
                    server.sendResponse(httpExchange, HttpURLConnection.HTTP_UNAUTHORIZED, new byte[]{});
                }
            } else {
                server.sendResponse(httpExchange, HttpURLConnection.HTTP_BAD_METHOD, new byte[]{});
            }
        } catch (NullPointerException e) {
            server.sendResponse(httpExchange, HttpURLConnection.HTTP_BAD_REQUEST, new byte[]{});
        }
    }
}
