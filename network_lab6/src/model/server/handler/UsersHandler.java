package model.server.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import model.HttpMethod;
import model.InvalidTokenException;
import model.message.UsersResponse;
import model.server.Server;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

/**
 * Created by Alexander on 24/11/2017.
 */
public class UsersHandler implements HttpHandler {
    private final Logger LOG = LogManager.getLogger(UsersHandler.class);
    private Gson gson = new Gson();
    private Server server;

    public UsersHandler(Server server) {
        this.server = server;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        if (httpExchange.getRequestMethod().equals(HttpMethod.GET)) {
            try {
                server.authorize(httpExchange);
            } catch (InvalidTokenException e) {
                server.sendResponse(httpExchange, HttpURLConnection.HTTP_UNAUTHORIZED, new byte[]{});
            }

            UsersResponse response = new UsersResponse(server.getUsers().values());
            byte[] body = gson.toJson(response).getBytes(StandardCharsets.UTF_8);

            httpExchange.getResponseHeaders().add("Content-Type", "application/json");
            server.sendResponse(httpExchange, HttpURLConnection.HTTP_OK, body);
            LOG.trace("send users {}", response.getUsers());
        } else {
            server.sendResponse(httpExchange, HttpURLConnection.HTTP_BAD_METHOD, new byte[]{});
        }

    }
}
