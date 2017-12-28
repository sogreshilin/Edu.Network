package model;

/**
 * Created by Alexander on 25/09/2017.
 */
public class Usage {
    @Override
    public String toString() {
        return "Usage: node_name loss port parent_ip parent_port";
    }

    static void print() {
        System.out.println(new Usage().toString());
    }
}
