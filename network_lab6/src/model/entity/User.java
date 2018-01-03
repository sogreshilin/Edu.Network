package model.entity;

import com.sun.istack.internal.NotNull;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Alexander on 23/11/2017.
 */
public class User {
    private int id;
    private String username;
    private boolean online;

    private static AtomicInteger idGenerator = new AtomicInteger(0);

    public User(String username) {
        this.username = username;
        this.online = true;
    }

    public void setId() {
        this.id = idGenerator.getAndIncrement();
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public boolean isOnline() {
        return online;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        if (id != user.id) return false;
        return username.equals(user.username);
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + username.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", online=" + online +
                '}';
    }
}
