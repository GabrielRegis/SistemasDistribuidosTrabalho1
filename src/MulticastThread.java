import org.apache.commons.lang3.SerializationUtils;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class MulticastThread extends Thread{

    public static MulticastSocket socket;
    public static InetAddress group;

    // Thread que representa processos de multicast
    public MulticastThread(MulticastSocket socketObj, InetAddress groupObj) {
        socket = socketObj;
        group = groupObj;
    }


    // Método utilizado para enviar um pacote para todos os peers conectados à rede.
    public static void sendUserEventPackage(MulticastSocket socket, InetAddress group, EventPackage eventPackage) throws IOException {
        byte[] m = SerializationUtils.serialize(eventPackage);

        DatagramPacket messageOut = new DatagramPacket(m, m.length, group, MulticastPeer.socket.getLocalPort());
        socket.send(messageOut);

    }

}
