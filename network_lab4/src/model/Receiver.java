package model;

import model.message.AbstractMessage;
import model.serializing.MessageDeserializer;
import model.serializing.XmlMessageDeserializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.*;

/**
 * Created by Alexander on 15/11/2017.
 */
public class Receiver implements Runnable {
    private static final int PACKAGE_LENGTH = 2048;
    private final Logger LOG = LogManager.getLogger(Receiver.class);
    private MessageDeserializer deserializer = new XmlMessageDeserializer();
    private DatagramSocket datagramSocket;
    private DatagramSocketHelper datagramHelper;

    public Receiver(DatagramSocketHelper helper, DatagramSocket socket) {
        datagramSocket = socket;
        datagramHelper = helper;
    }

    @Override
    public void run() {
        try {
            datagramSocket.setSoTimeout(1000);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        while (!Thread.interrupted()) {
            DatagramPacket packet = new DatagramPacket(new byte[PACKAGE_LENGTH], PACKAGE_LENGTH);
            try {
                try {
                    datagramSocket.receive(packet);
                } catch (SocketTimeoutException e) {
                    if (datagramHelper.getState() == SocketState.FINISHED) {
                        LOG.trace("receiver was closed");
                        return;
                    } else {
                        continue;
                    }
                }

                InetAddress ip = packet.getAddress();
                int port = packet.getPort();
                AbstractMessage message = deserializer.deserialize(packet.getData(), packet.getLength());
                message.setSender(new InetSocketAddress(ip, port));

                message.process(datagramHelper);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}