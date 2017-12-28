package test;

import model.message.*;
import model.serializing.MessageDeserializer;
import model.serializing.MessageSerializer;
import model.serializing.XmlMessageDeserializer;
import model.serializing.XmlMessageSerializer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Created by Alexander on 30/09/2017.
 */
public class MessageSerializerTest {

    private DatagramSocket socket;
    private MessageSerializer serializer;
    private MessageDeserializer deserializer;
    private JoinMessage joinMessage;
    private TextMessage textMessage;
    private ChangeParentMessage changeParentMessage;
    private LeaveMessage leaveMessage;
    private AckMessage ackMessage;
    private static final int PORT = 5000;
    private static final int BUFFER_SIZE = 1024 * 1024;
    private static final String NAME = "user_name";
    private static final String TEXT = "hello world!";

    @Before
    public void setUp() throws Exception {
        socket = new DatagramSocket(PORT);
        serializer = new XmlMessageSerializer();
        deserializer = new XmlMessageDeserializer();

        joinMessage = new JoinMessage();
        joinMessage.setUuid(UUID.randomUUID());

        textMessage = new TextMessage();
        textMessage.setUuid(UUID.randomUUID());
        textMessage.setName(NAME);
        textMessage.setText(TEXT);

        changeParentMessage = new ChangeParentMessage();
        changeParentMessage.setUuid(UUID.randomUUID());
        changeParentMessage.setParentIP("localhost");
        changeParentMessage.setParentPort(PORT);

        leaveMessage = new LeaveMessage();
        leaveMessage.setUuid(UUID.randomUUID());

        ackMessage = new AckMessage();
        ackMessage.setUuid(leaveMessage.getUuid());
    }

    private void send(Message message) throws IOException {
        byte[] data = serializer.serialize(message);
        System.out.println(new String(data, StandardCharsets.UTF_8));
        InetAddress ipAddress = InetAddress.getByName("localhost");
        DatagramPacket packet = new DatagramPacket(data, data.length, ipAddress, PORT);
        socket.send(packet);
    }

    private Message recv() throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket recvPacket = new DatagramPacket(buffer, buffer.length);
        socket.receive(recvPacket);
        return deserializer.deserialize(recvPacket.getData(), recvPacket.getLength());
    }

    @Test
    public void serialize() throws Exception {
        Message message;

        send(joinMessage);
        message = recv();
        Assert.assertEquals(joinMessage.getUuid(), ((JoinMessage) message).getUuid());

        send(textMessage);
        message = recv();
        Assert.assertEquals(textMessage.getText(), ((TextMessage) message).getText());
        Assert.assertEquals(textMessage.getUuid(), ((TextMessage) message).getUuid());
        Assert.assertEquals(textMessage.getName(), ((TextMessage) message).getName());

        send(changeParentMessage);
        message = recv();
        Assert.assertEquals(changeParentMessage.getUuid(), ((ChangeParentMessage) message).getUuid());
        Assert.assertEquals(changeParentMessage.getParentIP(), ((ChangeParentMessage) message).getParentIP());
        Assert.assertEquals(changeParentMessage.getParentPort(), ((ChangeParentMessage) message).getParentPort());


        send(ackMessage);
        message = recv();
        Assert.assertEquals(ackMessage.getUuid(), ((AckMessage) message).getUuid());

        send(leaveMessage);
        message = recv();
        Assert.assertEquals(leaveMessage.getUuid(), ((LeaveMessage) message).getUuid());
    }

}