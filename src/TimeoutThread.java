import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

public class TimeoutThread extends MulticastThread{
    public TimeoutThread(MulticastSocket socketObj, InetAddress groupObj) {
        super(socketObj, groupObj);
    }

    @Override
    public synchronized void start() {
        super.start();

        while(true){
            // Timer para controle de timeout
            Timer timer = new Timer();
            // A cada 4 segundos, caso o peer não receber as mensagens, o peer será desconectado
            timer.schedule(new TimerTask() {
                @Override
                public void run() {

                    // Verifica se todos os peers que conhece enviaram um ack
                    HashMap<String, String> publickKeys = MulticastPeer.user.getKeys();
                    for (String resourceKey : publickKeys.keySet()) {
                        if (!MulticastPeer.user.getAckPeers().containsKey(resourceKey)){
                            String username = MulticastPeer.user.getUsernames().get(resourceKey);
                            System.out.println("Usuário " + username + " desconectado");
                            MulticastPeer.user.getKeys().remove(resourceKey);
                            MulticastPeer.user.getResourcesQueues().remove(resourceKey);
                            break;
                        }
                    }
                    MulticastPeer.timeOutLatch.countDown();
                }
            }, 4000);

            // Envia requisição de ack para todos
            EventPackage sendAck = new EventPackage(EventType.REQUEST_ACK, MulticastPeer.user.getPeerName(), MulticastPeer.user.getPublicKey(), MulticastPeer.user.getPrivateKey());
            try {
                sendUserEventPackage(socket, group, sendAck);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                MulticastPeer.timeOutLatch.await();
                MulticastPeer.timeOutLatch = new CountDownLatch(1);
                MulticastPeer.user.setAckPeers(new HashMap<String, String>());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
