package model;

import lombok.Getter;
import lombok.Setter;

import java.net.InetSocketAddress;

/**
 * Created by Alexander on 06/10/2017.
 */
public class Config {
    private String name;
    private int loss;
    private int port;
    private InetSocketAddress parent;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLoss() {
        return loss;
    }

    public void setLoss(int loss) {
        this.loss = loss;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public InetSocketAddress getParent() {
        return parent;
    }

    public void setParent(InetSocketAddress parent) {
        this.parent = parent;
    }
}
