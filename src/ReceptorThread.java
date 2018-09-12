import org.apache.commons.lang3.SerializationUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.*;

// Thread receptora
public class ReceptorThread extends MulticastThread {

    public ReceptorThread(MulticastSocket socketObj, InetAddress groupObj) {
        super(socketObj, groupObj);
    }

    @Override
    public synchronized void start() {
        super.start();
        while (true) {


            byte[] buffer = new byte[10000];
            DatagramPacket messageIn = new DatagramPacket(buffer, buffer.length);
            User user = MulticastPeer.user;

            try {
                socket.receive(messageIn);
            } catch (IOException e) {
                e.printStackTrace();
            }

            EventPackage eventReceived = SerializationUtils.deserialize(messageIn.getData());
            MulticastPeer.latch.countDown();

            // Todos os peers que receberam a mensagem do peer recentemente conectado, enviarão para o mesmo seus estados.
            switch (eventReceived.getType()) {
                case USER_CONNECTED_RESPONSE:
                    String peerPublicKey = eventReceived.getSenderPublicKey();
                    if (!user.getKeys().containsKey(peerPublicKey)) {
                        user.getKeys().put(peerPublicKey, "");
                        user.getUsernames().put(peerPublicKey, eventReceived.getSenderUsername());

                        HashMap<Integer, Resource> receivedResources = eventReceived.getResources();
                        for (Integer resourceKey : receivedResources.keySet()) {
                            user.getResources().put(resourceKey, receivedResources.get(resourceKey));
                        }

                        for (Resource resource : user.getResources().values()) {
                            MulticastPeer.user.getResourcesQueues().put(resource.getResourceKey(), new PriorityQueue<ResourceEventPackage>());
                        }
                    }
                    break;

                // Mensagem recebida de um usuário que acabou de se conectar
                case USER_CONNECTED:
                    if (!user.getKeys().containsKey(eventReceived.getSenderPublicKey())) {
                        user.getKeys().put(eventReceived.getSenderPublicKey(), "");
                        user.getUsernames().put(eventReceived.getSenderPublicKey(), eventReceived.getSenderUsername());
                    }
                    if (user.getKeys().get(eventReceived.getSenderPublicKey()) == "") {
                        EventPackage userResponsePackage = new EventPackage(EventType.USER_CONNECTED_RESPONSE, user.getPeerName(), user.getPublicKey());
                        userResponsePackage.setDestinationPublicKey(eventReceived.getSenderPublicKey());
                        userResponsePackage.setResources(MulticastPeer.user.getResources());

                        try {
                            sendUserEventPackage(socket, group, userResponsePackage);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    System.out.println(eventReceived.getMessage());
                    break;

                // Mensagem recebida de um usuário que foi desconectado
                case USER_DISCONNECTED:
                    System.out.println(eventReceived.getMessage());
                    MulticastPeer.user.getKeys().remove(eventReceived.getSenderPublicKey());
                    MulticastPeer.user.getUsernames().remove(eventReceived.getSenderPublicKey());
                    if (user.getKeys().get(eventReceived.getSenderPublicKey()) != "") {
                        return;
                    }
                    break;

                // Mensagem recebida de um usuário que foi desconectado por inatividade ou conexão comprometida
                case USER_DROPED:
                    System.out.println(eventReceived.getMessage());
                    MulticastPeer.user.getKeys().remove(eventReceived.getSenderPublicKey());
                    MulticastPeer.user.getUsernames().remove(eventReceived.getSenderPublicKey());
                    if (user.getKeys().get(eventReceived.getSenderPublicKey()) != "") {
                        return;
                    }
                    break;

                // Recebe pedido de identificação de um peer (O peer que receber esta mensagem enviará uma resposta para o peer que
                // requisitou a lista de usuários conectados).
                case REQUEST_CONNECTED_USERS:
                    EventPackage userResponsePackage = new EventPackage(EventType.CONNECTED_USERS_RESPONSE, user.getPeerName(), user.getPublicKey());
                    userResponsePackage.setDestinationPublicKey(eventReceived.getSenderPublicKey());
                    try {
                        sendUserEventPackage(socket, group, userResponsePackage);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
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
                    MulticastPeer.user.getResources().put(resourceEvent.getResource().getResourceKey(), resourceEvent.getResource());
                    ResourceComparator comparator = new ResourceComparator();
                    PriorityQueue<ResourceEventPackage> queue = new PriorityQueue<ResourceEventPackage>(comparator);
                    MulticastPeer.user.getResourcesQueues().put(resourceEvent.getResource().getResourceKey(), queue);
                    break;

                // Mensagem recebida de um peer que gostaria de acessar um recurso específico
                case RESOURCE_REQUEST:
                    resourceEvent = (ResourceEventPackage) eventReceived;
                    Resource resource = MulticastPeer.user.getResources().get(resourceEvent.getResource().getResourceKey());

                    if (resource.getResourceState() == ResourceState.HELD) {

                        MulticastPeer.user.getResourcesQueues().get(resource.getResourceKey()).add(resourceEvent);
                        ResourceEventPackage resourceResponse = new ResourceEventPackage(EventType.RESOURCE_RESPONSE, user, resource);
                        resourceResponse.setDestinationPublicKey(resourceEvent.getSenderPublicKey());
                        resourceResponse.setResponse(ResourceResponse.OCCUPIED);
                        try {
                            sendUserEventPackage(socket, group, resourceResponse);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }else {
                        ResourceEventPackage resourceResponse = new ResourceEventPackage(EventType.RESOURCE_RESPONSE, user, resource);
                        resourceResponse.setDestinationPublicKey(resourceEvent.getSenderPublicKey());
                        resourceResponse.setResponse(ResourceResponse.FREE);
                        try {
                            sendUserEventPackage(socket, group, resourceResponse);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    break;

                case RESOURCE_RESPONSE:
                    resourceEvent = (ResourceEventPackage) eventReceived;
                    if (deserialize(resourceEvent.getDestinationPublicKey())) {
                        switch (resourceEvent.getResponse()) {
                            case FREE:
                                MulticastPeer.responseLatch.countDown();
                                if (MulticastPeer.responseLatch.getCount() == 0) {
                                    MulticastPeer.user.getResources().get(resourceEvent.getResource().getResourceKey()).setResourceState(ResourceState.HELD);
                                    MulticastPeer.isUsingSharedResource = true;
                                    MulticastPeer.currentResourceBeingUsed = resourceEvent.getResource();
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
                    MulticastPeer.user.setResourcesQueues(resourceEvent.getResourcesQueues());
                    break;

                case REQUEST_ACK:
                    EventPackage ackResponse = new EventPackage(EventType.ACK_RESPONSE, user.getPeerName(), user.getPublicKey());
                    ackResponse.setDestinationPublicKey(eventReceived.getDestinationPublicKey());
                    try {
                        sendUserEventPackage(socket, group, ackResponse);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;

                case ACK_RESPONSE:
                    MulticastPeer.user.getAckPeers().put(eventReceived.getSenderPublicKey(), "Ack");
                    break;

            }
        }
    }

    // Método utilizado para verificar se o pacote recebido tem como peer destino o peer que disparar esta função.
    // Recebe uma public que, e se, a public key estiver vinculada à uma private key no hash de keys, então deserializa o pacote.
    public static boolean deserialize(String publicKey){
        return MulticastPeer.user.getKeys().get(publicKey) != "";
    }

}
