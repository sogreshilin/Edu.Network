package model.server.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import model.HttpMethod;
import model.InvalidTokenException;
import model.server.Server;
import model.entity.User;
import model.message.LogoutResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Created by Alexander on 24/11/2017.
 */
public class LogoutHandler implements HttpHandler {
    private final Logger LOG = LogManager.getLogger(LogoutHandler.class);
    private Gson gson = new Gson();
    private Server server;

    public LogoutHandler(Server server) {
        this.server = server;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        if (httpExchange.getRequestMethod().equals(HttpMethod.GET)) {
            UUID token;
            try {
                token = server.authorize(httpExchange);
            } catch (InvalidTokenException e) {
                server.sendResponse(httpExchange, HttpURLConnection.HTTP_UNAUTHORIZED, new byte[]{});
                return;
            }

            User user = server.getUsers().remove(token);
            LOG.trace("user deleted: {}", user.getUsername());

            LogoutResponse response = new LogoutResponse();
            byte[] body = gson.toJson(response).getBytes(StandardCharsets.UTF_8);

            httpExchange.getResponseHeaders().add("Content-Type", "application/json");
            server.sendResponse(httpExchange, HttpURLConnection.HTTP_OK, body);
        } else {
            server.sendResponse(httpExchange, HttpURLConnection.HTTP_BAD_METHOD, new byte[]{});
        }
    }
}
