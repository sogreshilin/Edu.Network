package view;

import model.Controller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Console;

/**
 * Created by Alexander on 02/10/2017.
 */
public class ConsoleView implements Runnable {

    private static final Logger LOG = LogManager.getLogger(ConsoleView.class);

    private Console console;
    private Controller controller;

    public ConsoleView(Controller controller) {
        this.console = System.console();
        if (console == null) {
            LOG.error("system console was not found");
            System.exit(-1);
        }
        this.controller = controller;
        controller.addSubscriber((name, text) -> {
            System.out.format("[%s]: %s\n", name, text);
        });
    }

    @Override
    public void run() {
        System.out.println("Type your messages here. Print \"exit\" to leave the chat");
        while (!Thread.interrupted()) {
            String line = console.readLine();
            if (!line.isEmpty()) {
                controller.handle(line);
            }
        }
    }
}
