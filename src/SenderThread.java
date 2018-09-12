import com.sun.org.apache.xpath.internal.operations.Mult;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

public class SenderThread extends MulticastThread{

    public SenderThread(MulticastSocket socketObj, InetAddress groupObj) {
        super(socketObj, groupObj);
    }

    @Override
    public synchronized void start() {
        super.start();
        super.run();
        System.out.println("Digite um nome de usuário: ");
        Scanner scanner = new Scanner(System.in);
        String userName = scanner.nextLine();
        MulticastPeer.user.setPeerName(userName);

        User user = MulticastPeer.user;
        EventPackage newUserPackage = new EventPackage(EventType.USER_CONNECTED, user.getPeerName(), user.getPublicKey());
        try {
            sendUserEventPackage(socket, group, newUserPackage);
        } catch (IOException e) {
            e.printStackTrace();
        }

        new Thread(()->{
            new TimeoutThread(socket, group).start();
        }).start();

        while (true) {


            // Espera o limite máximo de todos os peers responderem
            try {
                MulticastPeer.latch.await();
                if(MulticastPeer.responseLatch != null){
                    MulticastPeer.responseLatch.await();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Limpa console
            try {
                Runtime.getRuntime().exec("clear");
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Verifica se está utilizando algum recurso (Bloqueia menu)
            if(MulticastPeer.isUsingSharedResource){
                System.out.println("Utilizando recurso, digite 0 para liberar recurso.");
                String nextMessage = scanner.next();
                MulticastPeer.isUsingSharedResource = false;

                // Libera recurso
                user.getResources().get(MulticastPeer.currentResourceBeingUsed.getResourceKey()).setResourceState(ResourceState.RELEASED);

                // Verifica se existem elementos na fila de recursos
                if (user.getResourcesQueues().get(MulticastPeer.currentResourceBeingUsed.getResourceKey()).size() > 0){

                    // Chama próximo da fila
                    // Envia a resposta FREE que estava faltando para o peer assumir o uso do recurso
                    ResourceEventPackage nextPeer = MulticastPeer.user.getResourcesQueues().get(MulticastPeer.currentResourceBeingUsed.getResourceKey()).remove();
                    ResourceEventPackage callNextPeer = new ResourceEventPackage(EventType.RESOURCE_RESPONSE, MulticastPeer.user, MulticastPeer.currentResourceBeingUsed);
                    callNextPeer.setDestinationPublicKey(nextPeer.getSenderPublicKey());
                    callNextPeer.setResponse(ResourceResponse.FREE);
                    try {
                        sendUserEventPackage(socket, group, callNextPeer);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                // Envia pacote para todos atualizarem a fila de peers para o recurso liberado
                ResourceEventPackage updateQueuePackage = new ResourceEventPackage(EventType.UPDATE_RESOURCE_QUEUE, MulticastPeer.user, MulticastPeer.currentResourceBeingUsed);
                try {
                    sendUserEventPackage(socket, group, updateQueuePackage);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                MulticastPeer.currentResourceBeingUsed = null;
            }

            // Mostra menu
            System.out.println("Digite os seguintes números para as seguintes funções: ");
            System.out.println("0 - Sair");
            System.out.println("1 - Listar os usuários conectados");
            System.out.println("2 - Cadastrar recurso");
            System.out.println("3 - Solicitar recurso / Liberar recurso");

            int message = scanner.nextInt();

            MulticastPeer.isWaitingForAnswer = true;
            switch (message) {
                case 0:
                    EventPackage disconnectPackage = new EventPackage(EventType.USER_DISCONNECTED, user.getPeerName(), user.getPublicKey());
                    try {
                        sendUserEventPackage(socket, group, disconnectPackage);
                        //socket.leaveGroup(group);
                        //System.exit(0);
                        //return;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case 1:
                    EventPackage listConnectedUsersPackage = new EventPackage(EventType.REQUEST_CONNECTED_USERS, user.getPeerName(), user.getPublicKey());
                    try {
                        sendUserEventPackage(socket, group, listConnectedUsersPackage);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case 2:
                    System.out.println("Digite o nome do recurso:");
                    scanner.nextLine();
                    String resourceName = scanner.nextLine();

                    MulticastPeer.latch = new CountDownLatch(1);
                    Resource resource = new Resource(resourceName);
                    Integer resourceIndex = user.getResources().size();
                    resource.setResourceKey(resourceIndex);

                    ResourceEventPackage createResourcePackage = new ResourceEventPackage(EventType.CREATE_RESOURCE, user, resource);
                    try {
                        sendUserEventPackage(socket, group, createResourcePackage);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case 3:

                    System.out.println("Recursos cadastrados: ");

                    int index = 0;

                    for(Resource res : user.getResources().values()){
                        System.out.println(index + " - " + res.getResourceName());
                        index++;
                    }
                    int nextIndex = scanner.nextInt();

                    MulticastPeer.latch = new CountDownLatch(user.getKeys().size() + 1);
                    MulticastPeer.responseLatch = new CountDownLatch(user.getKeys().size());

                    //Mudança de estado do peer para o recurso selecionado
                    user.getResources().get(nextIndex).setResourceState(ResourceState.WANTED);
                    LocalTime timeStamp = LocalTime.now();
                    MulticastPeer.resourceRequestTimeStamp = timeStamp;
                    ResourceEventPackage requestResource = new ResourceEventPackage(EventType.RESOURCE_REQUEST, user, user.getResources().get(nextIndex));
                    requestResource.setTimeStamp(timeStamp);

                    try {
                        sendUserEventPackage(socket, group, requestResource);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;

            }
        }
    }
}
