import org.apache.commons.lang3.SerializationUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.time.LocalTime;
import java.util.concurrent.CountDownLatch;

public class MulticastThread extends Thread{

    public static User user;
    public static boolean isWaitingForAnswer = false;
    public static CountDownLatch latch = new CountDownLatch(1);
    public static CountDownLatch responseLatch = null;
    public static LocalTime resourceRequestTimeStamp = null;
    public static Boolean isUsingSharedResource = false;
    public static Resource currentResourceBeingUsed = null;
    public static MulticastSocket socket;
    public static InetAddress group;


    public MulticastThread(MulticastSocket socketObj, InetAddress groupObj, User userObj) {
        user = userObj;
        socket = socketObj;
        group = groupObj;
    }

    // Método utilizado para verificar se o pacote recebido tem como peer destino o peer que disparar esta função.
    // Recebe uma public que, e se, a public key estiver vinculada à uma private key no hash de keys, então deserializa o pacote.
    public static boolean deserialize(String publicKey){
        return user.getKeys().get(publicKey) != "";
    }


    // Método utilizado para enviar um pacote para todos os peers conectados à rede.
    public static void sendUserEventPackage(MulticastSocket socket, InetAddress group, EventPackage eventPackage) throws IOException {
        byte[] m = SerializationUtils.serialize(eventPackage);

        DatagramPacket messageOut = new DatagramPacket(m, m.length, group, 8000);
        socket.send(messageOut);

    }

}
