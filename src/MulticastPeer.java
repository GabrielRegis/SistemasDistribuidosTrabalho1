import java.net.*;
import java.io.*;
import java.time.LocalTime;
import java.util.concurrent.CountDownLatch;

public class MulticastPeer {

    // Informações gerais do peer
    public static User user;

    // Controle de espera de resposta
    public static boolean isWaitingForAnswer = false;

    // Latch é utilizado para controlar quantas repostas o peer está esperando ao enviar um pacote,
    // para que possa prosseguir para a próxima tarefa
    public static CountDownLatch latch = new CountDownLatch(1);

    // Latch para controle de respostas do algoritmo utilizado para controle da fila de recursos
    public static CountDownLatch responseLatch = null;

    // Timestamp registrado no momento em que um recurso é solicitado, utilizado para controle da fila de recursos
    public static LocalTime resourceRequestTimeStamp = null;

    // Verifica se um peer está utilizando um recurso compartilhado
    public static Boolean isUsingSharedResource = false;

    // Armazena o recurso sendo utilizado pelo peer, em caso de uso
    public static Resource currentResourceBeingUsed = null;

    // Groupo do peer
    public static InetAddress group;

    // Socket do peer
    public static MulticastSocket socket;


    public static void main(String args[]) throws IOException {

        socket = new MulticastSocket(8000);

        try {
            user = new User(socket.getLocalPort());

            group = InetAddress.getByName("ff02::1");
            socket.setNetworkInterface(NetworkInterface.getByName("en0"));
            socket.joinGroup(group);

            // Inicia thread de envio de pacotes
            new Thread(()->{
                new SenderThread(socket, group).start();
            }).start();

            // Inicia thread de recepção de pacotes
            new ReceptorThread(socket, group).start();

        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO: " + e.getMessage());
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }

}
