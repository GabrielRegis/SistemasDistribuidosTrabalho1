import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.time.LocalTime;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

public class SenderThread extends MulticastThread{
    public SenderThread(MulticastSocket socketObj, InetAddress groupObj, User userObj) {
        super(socketObj, groupObj, userObj);
    }

    @Override
    public void run() {
        super.run();
        System.out.println("Digite um nome de usuário: ");
        Scanner scanner = new Scanner(System.in);
        String userName = scanner.nextLine();
        user.setPeerName(userName);

        EventPackage newUserPackage = new EventPackage(EventType.USER_CONNECTED, user.getPeerName(), user.getPublicKey());
        try {
            sendUserEventPackage(socket, group, newUserPackage);
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (true) {

            // Espera o limite máximo de todos os peers responderem
            try {
                latch.await();
                if(responseLatch != null){
                    responseLatch.await();
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
            if(isUsingSharedResource){
                System.out.println("Utilizando recurso, digite 0 para liberar recurso.");
                String nextMessage = scanner.next();
                isUsingSharedResource = false;

                // Libera recurso
                user.getResources().get(currentResourceBeingUsed.getResourceKey()).setResourceState(ResourceState.RELEASED);

                // Verifica se existem elementos na fila de recursos
                if (user.getResourcesQueues().get(currentResourceBeingUsed.getResourceKey()).size() > 0){

                    // Chama próximo da fila
                    ResourceEventPackage nextPeer = user.getResourcesQueues().get(currentResourceBeingUsed.getResourceKey()).remove();
                    ResourceEventPackage callNextPeer = new ResourceEventPackage(EventType.RESOURCE_RESPONSE, user, currentResourceBeingUsed);
                    callNextPeer.setDestinationPublicKey(nextPeer.getSenderPublicKey());
                    callNextPeer.setResponse(ResourceResponse.FREE);
                    try {
                        sendUserEventPackage(socket, group, callNextPeer);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                // Envia pacote para todos atualizarem a fila de peers para o recurso liberado
                ResourceEventPackage updateQueuePackage = new ResourceEventPackage(EventType.UPDATE_RESOURCE_QUEUE, user, currentResourceBeingUsed);
                try {
                    sendUserEventPackage(socket, group, updateQueuePackage);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                currentResourceBeingUsed = null;
            }

            // Mostra menu
            System.out.println("Digite os seguintes números para as seguintes funções: ");
            System.out.println("0 - Sair");
            System.out.println("1 - Listar os usuários conectados");
            System.out.println("2 - Cadastrar recurso");
            System.out.println("3 - Solicitar recurso / Liberar recurso");

            int message = scanner.nextInt();

            isWaitingForAnswer = true;
            switch (message) {
                case 0:
                    EventPackage disconnectPackage = new EventPackage(EventType.USER_DISCONNECTED, user.getPeerName(), user.getPublicKey());
                    try {
                        sendUserEventPackage(socket, group, disconnectPackage);
                        return;
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

                    latch = new CountDownLatch(1);
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

                    latch = new CountDownLatch(user.getKeys().size() + 1);
                    responseLatch = new CountDownLatch(user.getKeys().size());
                    user.getResources().get(nextIndex).setResourceState(ResourceState.WANTED);
                    LocalTime timeStamp = LocalTime.now();
                    resourceRequestTimeStamp = timeStamp;
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
