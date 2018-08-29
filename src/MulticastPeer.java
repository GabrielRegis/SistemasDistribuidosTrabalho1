
import org.apache.commons.lang3.SerializationUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Array;
import java.net.*;
import java.io.*;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class MulticastPeer {

    public static User user;
    public static boolean isWaitingForAnswer = false;
    public static CountDownLatch latch = new CountDownLatch(1);
    public static CountDownLatch responseLatch = null;
    public static LocalTime resourceRequestTimeStamp = null;
    public static Boolean isUsingSharedResource = false;
    public static Resource currentResourceBeingUsed = null;


    public static void main(String args[]) throws IOException {

        final MulticastSocket socket = new MulticastSocket(8000);

        try {
            user = new User(socket.getLocalPort());

            InetAddress group = InetAddress.getByName("ff02::1");
            socket.setNetworkInterface(NetworkInterface.getByName("en0"));
            socket.joinGroup(group);

            new Thread(() -> {
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

            }).start();

            /////////////////////////////////
            /////////////////////////////////
            // Thread controladora de mensagens à serem recebidas
            while (true) {

                // Timer para controle de timeout
                Timer timer = new Timer();
                byte[] buffer = new byte[10000];
                DatagramPacket messageIn = new DatagramPacket(buffer, buffer.length);

                // A cada 4 segundos, caso o peer não receber as mensagens, o peer será desconectado
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (messageIn.getAddress() == null){
                            EventPackage userResponsePackage = new EventPackage(EventType.USER_DROPED, user.getPeerName(), user.getPublicKey());
                            try {
                                sendUserEventPackage(socket, group, userResponsePackage);
                                return;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                    }
                }, 4000);

                socket.receive(messageIn);
                EventPackage eventReceived = SerializationUtils.deserialize(messageIn.getData());
                latch.countDown();

                // Todos os peers que receberam a mensagem do peer recentemente conectado, enviarão para o mesmo seus estados.
                switch (eventReceived.getType()) {
                    case USER_CONNECTED_RESPONSE:
                        String peerPublicKey = eventReceived.getSenderPublicKey();
                        if (!user.getKeys().containsKey(peerPublicKey)) {
                            user.getKeys().put(peerPublicKey, "");
                            HashMap<Integer, Resource> receivedResources = eventReceived.getResources();
                            for (Integer resourceKey : receivedResources.keySet()){
                                user.getResources().put(resourceKey, receivedResources.get(resourceKey));
                            }

                            for(Resource resource : user.getResources().values()){
                                user.getResourcesQueues().put(resource.getResourceKey(), new PriorityQueue<ResourceEventPackage>());
                            }
                        }
                        break;

                    // Mensagem recebida de um usuário que acabou de se conectar
                    case USER_CONNECTED:
                        if (!user.getKeys().containsKey(eventReceived.getSenderPublicKey())){
                            user.getKeys().put(eventReceived.getSenderPublicKey(), "");
                        }
                        if (user.getKeys().get(eventReceived.getSenderPublicKey()) == "") {
                            EventPackage userResponsePackage = new EventPackage(EventType.USER_CONNECTED_RESPONSE, user.getPeerName(), user.getPublicKey());
                            userResponsePackage.setDestinationPublicKey(eventReceived.getSenderPublicKey());
                            userResponsePackage.setResources(user.getResources());
                            sendUserEventPackage(socket, group, userResponsePackage);
                        }
                        System.out.println(eventReceived.getMessage());
                        break;

                    // Mensagem recebida de um usuário que foi desconectado
                    case USER_DISCONNECTED:
                        System.out.println(eventReceived.getMessage());
                        user.getKeys().remove(eventReceived.getSenderPublicKey());
                        if (user.getKeys().get(eventReceived.getSenderPublicKey()) != "") {
                            return;
                        }
                        break;

                    // Mensagem recebida de um usuário que foi desconectado por inatividade ou conexão comprometida
                    case USER_DROPED:
                        System.out.println(eventReceived.getMessage());
                        user.getKeys().remove(eventReceived.getSenderPublicKey());
                        if (user.getKeys().get(eventReceived.getSenderPublicKey()) != "") {
                            return;
                        }
                        break;

                    // Recebe pedido de identificação de um peer (O peer que receber esta mensagem enviará uma resposta para o peer que
                    // requisitou a lista de usuários conectados).
                    case REQUEST_CONNECTED_USERS:
                        EventPackage userResponsePackage = new EventPackage(EventType.CONNECTED_USERS_RESPONSE, user.getPeerName(), user.getPublicKey());
                        userResponsePackage.setDestinationPublicKey(eventReceived.getSenderPublicKey());
                        sendUserEventPackage(socket, group, userResponsePackage);
                        break;

                    // Recebe respostas de peers conectados
                    case CONNECTED_USERS_RESPONSE:
                        if (user.getKeys().get(eventReceived.getDestinationPublicKey()) != "") {
                            System.out.println(eventReceived.getSenderUsername());
                        }
                        break;

                    // Recebe pacote de criação de recurso, atualiza sua lista de recursos
                    case CREATE_RESOURCE:
                        ResourceEventPackage resourceEvent = (ResourceEventPackage) eventReceived;
                        user.getResources().put(resourceEvent.getResource().getResourceKey(), resourceEvent.getResource());
                        user.getResourcesQueues().put(resourceEvent.getResource().getResourceKey(), new PriorityQueue<ResourceEventPackage>());
                        break;

                    case RESOURCE_REQUEST:
                        resourceEvent = (ResourceEventPackage) eventReceived;
                        Resource resource = user.getResources().get(resourceEvent.getResource().getResourceKey());

                        if(resource.getResourceState() == ResourceState.HELD ||
                                (resource.getResourceState() == ResourceState.WANTED &&
                                        resourceRequestTimeStamp.isBefore(resourceEvent.getTimeStamp()))){

                            user.getResourcesQueues().get(resource.getResourceKey()).add(resourceEvent);
                            ResourceEventPackage resourceResponse = new ResourceEventPackage(EventType.RESOURCE_RESPONSE, user, resource);
                            resourceResponse.setDestinationPublicKey(resourceEvent.getSenderPublicKey());
                            resourceResponse.setResponse(ResourceResponse.OCCUPIED);
                            sendUserEventPackage(socket, group, resourceResponse);
                        }else{
                            ResourceEventPackage resourceResponse = new ResourceEventPackage(EventType.RESOURCE_RESPONSE, user, resource);
                            resourceResponse.setDestinationPublicKey(resourceEvent.getSenderPublicKey());
                            resourceResponse.setResponse(ResourceResponse.FREE);
                            sendUserEventPackage(socket, group, resourceResponse);
                        }

                        break;

                    case RESOURCE_RESPONSE:
                        resourceEvent = (ResourceEventPackage) eventReceived;
                        if(deserialize(resourceEvent.getDestinationPublicKey())){
                            switch(resourceEvent.getResponse()){
                                case FREE:
                                    responseLatch.countDown();
                                    if(responseLatch.getCount() == 0){
                                        user.getResources().get(resourceEvent.getResource().getResourceKey()).setResourceState(ResourceState.HELD);
                                        isUsingSharedResource = true;
                                        currentResourceBeingUsed = resourceEvent.getResource();
                                    }
                                    break;
                                case OCCUPIED:

                                    System.out.println("Recurso sendo utilizado, por favor aguarde.");
                                    break;
                            }
                        }
                        break;

                    case UPDATE_RESOURCE_QUEUE:
                        resourceEvent = (ResourceEventPackage) eventReceived;
                        // Atualiza fila de peers esperando o recurso
                        user.setResourcesQueues(resourceEvent.getResourcesQueues());
                        break;

                }

            }

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
