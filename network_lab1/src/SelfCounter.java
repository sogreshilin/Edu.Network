import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.TimeoutException;

/**
 * Created by Alexander on 06/09/2017.
 */
public class SelfCounter {

    private static final int PORT = 9876;
    private static final int CYCLE_TIME = 1000;
    private static final int SO_TIMEOUT = 300;
    private static final int DEAD_TIME = 3000;

    private static Map<InetAddress, Long> map = new HashMap<>();

    public static void removeDead() {
        for (Iterator<Map.Entry<InetAddress, Long>> iterator = map.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<InetAddress, Long> entry = iterator.next();
            if (System.currentTimeMillis() - entry.getValue() > DEAD_TIME) {
                iterator.remove();
                System.out.println(map.size() + " copies : " + map.keySet());
            }
        }
    }

    public static void main(String[] args) {
        try (MulticastSocket socket = new MulticastSocket(PORT)) {
            InetAddress groupAddress = InetAddress.getByName(args[0]);
            socket.setInterface(InetAddress.getLocalHost());
            socket.joinGroup(groupAddress);
            socket.setSoTimeout(SO_TIMEOUT);

            while (true) {
                long enterTime = System.currentTimeMillis();
                DatagramPacket sendPacket = new DatagramPacket(new byte[]{}, 0, groupAddress, PORT);
                socket.send(sendPacket);

                while (System.currentTimeMillis() - enterTime < CYCLE_TIME) {
                    DatagramPacket recvPacket = new DatagramPacket(new byte[]{}, 0);
                    try {
                        socket.receive(recvPacket);
                        InetAddress recvFromAddress = recvPacket.getAddress();
                        if (!map.containsKey(recvFromAddress)) {
                            map.put(recvFromAddress, System.currentTimeMillis());
                            System.out.println(map.size() + " copies : " + map.keySet());
                        } else {
                            map.replace(recvFromAddress, System.currentTimeMillis());
                        }
                    } catch (SocketTimeoutException e) {
                    }
                }
                removeDead();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
