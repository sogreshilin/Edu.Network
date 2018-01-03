package model.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import model.InvalidTokenException;
import model.NoSuchUserException;
import model.RestMethod;
import model.entity.Message;
import model.entity.User;
import model.server.handler.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;


/**
 * Created by Alexander on 24/11/2017.
 */
public class Server {

    public final static int PORT = 5000;
    private final Logger LOG = LogManager.getLogger(Server.class);
    private final Object lock = new Object();
    private HttpServer server;
    private Map<UUID, User> users = new ConcurrentHashMap<>();
    private ArrayList<Message> messages = new ArrayList<>();

    public Server() {
        try {
            server = HttpServer.create();
            server.bind(new InetSocketAddress(PORT), 0);
            server.createContext(RestMethod.LOGIN, new LoginHandler(this));
            server.createContext(RestMethod.LOGOUT, new LogoutHandler(this));
            server.createContext(RestMethod.GET_USERS, new UsersHandler(this));
            server.createContext(RestMethod.GET_USER, new UserHandler(this));
            server.createContext(RestMethod.POST_MESSAGE, new MessageHandler(this));
            server.setExecutor(Executors.newCachedThreadPool());

            new Thread(() -> {
                final int TIMEOUT = 10000;
                try {
                    while (!Thread.interrupted()) {
                        Thread.sleep(TIMEOUT);
                        long time = System.currentTimeMillis();
                        List<UUID> disconnected = lastQueries.entrySet().stream()
                                .filter(entry -> time - entry.getValue() > TIMEOUT)
                                .map(Map.Entry::getKey)
                                .collect(Collectors.toList());
                        LOG.trace("dead: {}", disconnected);
                        disconnected.forEach(user -> users.remove(user));
                        disconnected.forEach(user -> lastQueries.remove(user));
                    }
                } catch (InterruptedException e) {
                    LOG.trace("deadCollector finished");
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        new Server().start();
    }

    public Map<UUID, User> getUsers() {
        return users;
    }

    public User getUser(int id) throws NoSuchUserException {
        return users.values().stream()
                .filter(element -> element.getId() == id)
                .findFirst()
                .orElseThrow(NoSuchUserException::new);
    }

    private void start() {
        server.start();
    }

    public boolean addUser(UUID token, User user) {
        boolean isUsernameNonUnique = users.values().stream()
                .anyMatch(element -> element.getUsername().equals(user.getUsername()));
        if (isUsernameNonUnique) {
            return false;
        }
        users.put(token, user);
        user.setId();
        return true;
    }


    public void sendResponse(HttpExchange httpExchange, int code, byte[] bytes) throws IOException {
        httpExchange.sendResponseHeaders(code, bytes.length);
        OutputStream os = httpExchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    public void addMessage(Message message) {
        synchronized (lock) {
            message.setId();
            messages.add(message);
        }
    }

    public List<Message> getMessages(int offset, int count) {
        List<Message> list;
        synchronized (lock) {
            int size = messages.size();
            long skipCount = (offset < 0) ? ((size < -offset) ? 0 : size + offset) : offset;
            list = messages.stream().skip(skipCount)
                    .filter(element -> element.getId() < Math.min(size, skipCount + count))
                    .collect(Collectors.toList());
        }
        return list;
    }

    public UUID deserializeToken(String stringToken) throws InvalidTokenException {
        if (stringToken == null) {
            throw new InvalidTokenException();
        }

        String[] ss = stringToken.split(" ");
        if (ss.length != 2 || !ss[0].equals("Token")) {
            throw new InvalidTokenException();
        }

        stringToken = ss[1];

        UUID token;
        try {
            token = UUID.fromString(stringToken);
        } catch (IllegalArgumentException e) {
            throw new InvalidTokenException(e);
        }

        return token;
    }

    private Map<UUID, Long> lastQueries = new ConcurrentHashMap<>();

    public UUID authorize(HttpExchange httpExchange) throws IOException, InvalidTokenException {
        String stringToken = httpExchange.getRequestHeaders().getFirst("authorization");
        UUID token;
        try {
            token = deserializeToken(stringToken);
        } catch (InvalidTokenException e) {
            LOG.trace("token not found");
            throw e;
        }

        if (!users.containsKey(token)) {
            LOG.trace("unknown token");
            throw new InvalidTokenException();
        }
        lastQueries.put(token, System.currentTimeMillis());
        return token;
    }


}
