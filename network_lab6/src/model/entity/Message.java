package model.entity;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Alexander on 24/11/2017.
 */
public class Message {
    private int id;
    private String message;
    private int author;

    private static AtomicInteger idGenerator = new AtomicInteger(0);

    public Message(String message, int author) {
        this.message = message;
        this.author = author;
    }

    public void setId() {
        this.id = idGenerator.getAndIncrement();
    }

    public int getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public int getAuthor() {
        return author;
    }

    @Override
    public String toString() {
        return author + ": " + message;
    }
}
