package model.server.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import model.HttpMethod;
import model.InvalidTokenException;
import model.entity.Message;
import model.server.Server;
import model.message.MessageRequest;
import model.message.MessageResponse;
import model.message.MessagesResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Created by Alexander on 24/11/2017.
 */
public class MessageHandler implements HttpHandler {
    private final static int DEFAULT_OFFSET = -10;
    private final static int DEFAULT_COUNT = 10;
    private final Logger LOG = LogManager.getLogger(MessageHandler.class);
    private Server server;
    private Gson gson = new Gson();

    public MessageHandler(Server server) {
        this.server = server;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        try {
            UUID token = server.authorize(httpExchange);
            if (httpExchange.getRequestMethod().equals(HttpMethod.POST)) {
                Reader reader = new InputStreamReader(httpExchange.getRequestBody(), StandardCharsets.UTF_8);
                MessageRequest request = gson.fromJson(reader, MessageRequest.class);

                Message message = new Message(request.getMessage(), server.getUsers().get(token).getId());
                server.addMessage(message);
                LOG.trace("[{}-{}]", message.getAuthor(), message.getMessage());
                MessageResponse response = new MessageResponse(message.getId(), message.getMessage());

                byte[] body = gson.toJson(response).getBytes(StandardCharsets.UTF_8);
                httpExchange.getResponseHeaders().add("Content-Type", "application/json");
                server.sendResponse(httpExchange, HttpURLConnection.HTTP_OK, body);

            } else if (httpExchange.getRequestMethod().equals(HttpMethod.GET)) {
                String offsetString = httpExchange.getRequestHeaders().getFirst("offset");
                int offset = (offsetString == null) ? DEFAULT_OFFSET : Integer.parseInt(offsetString);

                String countString = httpExchange.getRequestHeaders().getFirst("count");
                int count = (countString == null) ? DEFAULT_COUNT : Integer.parseInt(countString);

                List<Message> messages = server.getMessages(offset, count);
                MessagesResponse response = new MessagesResponse(messages);
                byte[] body = gson.toJson(response).getBytes(StandardCharsets.UTF_8);
                httpExchange.getResponseHeaders().add("Content-Type", "application/json");
                server.sendResponse(httpExchange, HttpURLConnection.HTTP_OK, body);

            } else {
                server.sendResponse(httpExchange, HttpURLConnection.HTTP_BAD_METHOD, new byte[]{});
            }

        } catch (InvalidTokenException e) {
            server.sendResponse(httpExchange, HttpURLConnection.HTTP_UNAUTHORIZED, new byte[]{});
        } catch (NumberFormatException e) {
            server.sendResponse(httpExchange, HttpURLConnection.HTTP_BAD_REQUEST, new byte[]{});
        }
    }
}
