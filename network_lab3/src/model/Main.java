package model;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import view.ConsoleView;

import java.net.SocketException;

/**
 * Created by Alexander on 02/10/2017.
 */
public class Main {
    private static final Logger LOG = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            Config config = ConfigParser.parse(args);
            Controller controller = new Controller(config);
            controller.joinChat();
            new Thread(new ConsoleView(controller)).run();
        } catch (SocketException e) {
            LOG.error("socket creating or accessing error", e);
        }
    }
}
