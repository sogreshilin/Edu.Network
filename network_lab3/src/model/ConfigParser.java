package model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;


/**
 * Created by Alexander on 06/10/2017.
 */
class ConfigParser {

    private static final Logger LOG = LogManager.getLogger(ConfigParser.class);

    static Config parse(String[] args) {
        Config config = new Config();
        try {
            if (args.length >= 3) {
                config.setName(args[0]);
                config.setLoss(Integer.parseInt(args[1]));
                config.setPort(Integer.parseInt(args[2]));
//                config.setMyself(new InetSocketAddress(InetAddress.getByName("localhost"),
//                        config.getPort()));
                if (args.length == 5) {
                    InetSocketAddress address = new InetSocketAddress(
                            InetAddress.getByName(args[3]),
                            Integer.parseInt(args[4]));
                    config.setParent(address);
                }
            } else {
                throw new IllegalArgumentException("Too few arguments provided");
            }
        } catch (IllegalArgumentException | UnknownHostException e) {
            LOG.error("Invalid input", e);
            Usage.print();
            System.exit(-1);
        }
        return config;
    }
}
