package model.message;
import model.entity.User;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by Alexander on 23/11/2017.
 */
public class UsersResponse {
    private ArrayList<User> users;

    public UsersResponse(Collection<User> users) {
        this.users = new ArrayList<>(users);
    }

    public ArrayList<User> getUsers() {
        return users;
    }
}
