package model.server.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import model.HttpMethod;
import model.InvalidTokenException;
import model.NoSuchUserException;
import model.server.Server;
import model.entity.User;
import model.message.UserResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

/**
 * Created by Alexander on 24/11/2017.
 */
public class UserHandler implements HttpHandler {
    private final Logger LOG = LogManager.getLogger(UserHandler.class);
    private Gson gson = new Gson();
    private Server server;

    public UserHandler(Server server) {
        this.server = server;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        if (httpExchange.getRequestMethod().equals(HttpMethod.GET)) {
            try {
                server.authorize(httpExchange);

                String stringId = httpExchange.getRequestURI().toString().split("/")[2];
                int id = Integer.parseInt(stringId);

                User user = server.getUser(id);
                UserResponse response = new UserResponse(user);

                byte[] body = gson.toJson(response).getBytes(StandardCharsets.UTF_8);
                httpExchange.getResponseHeaders().add("Content-Type", "application/json");
                server.sendResponse(httpExchange, HttpURLConnection.HTTP_OK, body);
                LOG.trace("send users {}", response.getUser());

            } catch (NumberFormatException | NoSuchUserException e) {
                server.sendResponse(httpExchange, HttpURLConnection.HTTP_NOT_FOUND, new byte[]{});
            } catch (InvalidTokenException e) {
                server.sendResponse(httpExchange, HttpURLConnection.HTTP_UNAUTHORIZED, new byte[]{});
            }
        } else {
            server.sendResponse(httpExchange, HttpURLConnection.HTTP_BAD_METHOD, new byte[]{});
        }
    }
}


