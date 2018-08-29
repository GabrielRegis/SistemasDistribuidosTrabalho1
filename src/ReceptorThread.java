import org.apache.commons.lang3.SerializationUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Timer;
import java.util.TimerTask;

public class ReceptorThread extends MulticastThread {

    public ReceptorThread(MulticastSocket socketObj, InetAddress groupObj, User userObj) {
        super(socketObj, groupObj, userObj);
    }

    @Override
    public void run() {
        super.run();
        while (true) {

            // Timer para controle de timeout
            Timer timer = new Timer();
            byte[] buffer = new byte[10000];
            DatagramPacket messageIn = new DatagramPacket(buffer, buffer.length);

            // A cada 4 segundos, caso o peer não receber as mensagens, o peer será desconectado
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (messageIn.getAddress() == null) {
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

            try {
                socket.receive(messageIn);
            } catch (IOException e) {
                e.printStackTrace();
            }
            EventPackage eventReceived = SerializationUtils.deserialize(messageIn.getData());
            latch.countDown();

            // Todos os peers que receberam a mensagem do peer recentemente conectado, enviarão para o mesmo seus estados.
            switch (eventReceived.getType()) {
                case USER_CONNECTED_RESPONSE:
                    String peerPublicKey = eventReceived.getSenderPublicKey();
                    if (!user.getKeys().containsKey(peerPublicKey)) {
                        user.getKeys().put(peerPublicKey, "");
                        HashMap<Integer, Resource> receivedResources = eventReceived.getResources();
                        for (Integer resourceKey : receivedResources.keySet()) {
                            user.getResources().put(resourceKey, receivedResources.get(resourceKey));
                        }

                        for (Resource resource : user.getResources().values()) {
                            user.getResourcesQueues().put(resource.getResourceKey(), new PriorityQueue<ResourceEventPackage>());
                        }
                    }
                    break;

                // Mensagem recebida de um usuário que acabou de se conectar
                case USER_CONNECTED:
                    if (!user.getKeys().containsKey(eventReceived.getSenderPublicKey())) {
                        user.getKeys().put(eventReceived.getSenderPublicKey(), "");
                    }
                    if (user.getKeys().get(eventReceived.getSenderPublicKey()) == "") {
                        EventPackage userResponsePackage = new EventPackage(EventType.USER_CONNECTED_RESPONSE, user.getPeerName(), user.getPublicKey());
                        userResponsePackage.setDestinationPublicKey(eventReceived.getSenderPublicKey());
                        userResponsePackage.setResources(user.getResources());
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
                    user.getResources().put(resourceEvent.getResource().getResourceKey(), resourceEvent.getResource());
                    user.getResourcesQueues().put(resourceEvent.getResource().getResourceKey(), new PriorityQueue<ResourceEventPackage>());
                    break;

                case RESOURCE_REQUEST:
                    resourceEvent = (ResourceEventPackage) eventReceived;
                    Resource resource = user.getResources().get(resourceEvent.getResource().getResourceKey());

                    if (resource.getResourceState() == ResourceState.HELD ||
                            (resource.getResourceState() == ResourceState.WANTED &&
                                    resourceRequestTimeStamp.isBefore(resourceEvent.getTimeStamp()))) {

                        user.getResourcesQueues().get(resource.getResourceKey()).add(resourceEvent);
                        ResourceEventPackage resourceResponse = new ResourceEventPackage(EventType.RESOURCE_RESPONSE, user, resource);
                        resourceResponse.setDestinationPublicKey(resourceEvent.getSenderPublicKey());
                        resourceResponse.setResponse(ResourceResponse.OCCUPIED);
                        try {
                            sendUserEventPackage(socket, group, resourceResponse);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
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
                                responseLatch.countDown();
                                if (responseLatch.getCount() == 0) {
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
    }
}
