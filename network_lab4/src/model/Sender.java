package model;

import model.message.AbstractMessage;
import model.message.RegMessage;
import model.serializing.MessageSerializer;
import model.serializing.XmlMessageSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;

/**
 * Created by Alexander on 15/11/2017.
 */
class Sender implements Runnable {

    private final Logger LOG = LogManager.getLogger(Sender.class);
    private final Object senderAwaker;
    private DatagramSocketHelper datagramHelper;
    private DatagramSocket datagramSocket;
    private MessageSerializer serializer = new XmlMessageSerializer();

    Sender(DatagramSocketHelper helper, DatagramSocket socket) {
        datagramHelper = helper;
        datagramSocket = socket;
        senderAwaker = helper.getSenderAwaker();
    }

    void send(AbstractMessage message) {
        byte[] bytes = serializer.serialize(message);
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, message.getReceiver());
        try {
            datagramSocket.send(packet);
        } catch (IOException e) {
            LOG.error("input/output error");
        }
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                ArrayList<MySocket> clients = datagramHelper.getClients();
                long minTimeWait = Long.MAX_VALUE;
                for (MySocket socket : clients) {
                    long timeWait;
                    while ((timeWait = socket.leftToWait()) <= 0) {
                        AbstractMessage message = socket.getMessageToSend();
                        if (message instanceof RegMessage) {
                            System.out.println("message size: " + ((RegMessage) message).getBody().length);
                        }
                        send(message);
                    }
                    minTimeWait = Long.min(minTimeWait, timeWait);
                }
                synchronized (senderAwaker) {
                    senderAwaker.wait(minTimeWait);
                }
            }
        } catch (InterruptedException e) {
            LOG.trace("sender was interrupted");
        }
    }
}