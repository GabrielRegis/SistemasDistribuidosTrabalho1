import java.io.Serializable;
import java.util.HashMap;

// Representa o tipo de evento que ocorreu
enum EventType {
    USER_CONNECTED(0), USER_DISCONNECTED(1), USER_DROPED(2), USER_CONNECTED_RESPONSE(3),
    REQUEST_CONNECTED_USERS(4), CONNECTED_USERS_RESPONSE(5), RESOURCE_REQUEST(6), RESOURCE_RESPONSE(7),
    RELEASE_RESOURCE(8), CREATE_RESOURCE(9), UPDATE_RESOURCE_QUEUE(10), REQUEST_ACK(11), ACK_RESPONSE(12);

    public int userEventIndex;
    EventType(int index) {
        userEventIndex = index;
    }
}

// Pacote enviado para todos os peers conectados ao enviar mensagem
public class EventPackage implements Serializable {

    //Tipo do evento
    private EventType type;

    //Representa o username do peer que enviou o pacote
    private String senderUsername;

    //Representa a
    private String senderPrivateKey;

    //Representa a chave pública do peer que enviou o pacote
    private String senderPublicKey;

    //Representa a chave pública do peer receptor, em caso de mensagem específica
    private String destinationPublicKey;

    //Mensagem a ser impressa na tela ao receber o pacote
    private String message;

    //Lista de recursos que podem ser enviados junto com o pacote, não necessariamente obrigatório
    HashMap<Integer,Resource> resources = new HashMap<Integer, Resource>();


    public EventPackage(EventType type, String username, String senderPublicKey) {
        this.type = type;
        this.senderUsername = username;
        this.senderPublicKey = senderPublicKey;
        switch (type){
            case USER_CONNECTED:
                this.message = "Usuário: " + senderUsername + " conectado.";
                break;
            case USER_DISCONNECTED:
                this.message = "Usuário: " + senderUsername + " se desconectou.";
                break;
            case USER_DROPED:
                this.message = "Usuário: " + senderUsername + " caiu.";
                break;
            default:
                this.message = "";
                break;
        }

    }

    public HashMap<Integer, Resource> getResources() {
        return resources;
    }

    public void setResources(HashMap<Integer, Resource> resources) {
        this.resources = resources;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public String getSenderPublicKey() {
        return senderPublicKey;
    }

    public void setSenderPublicKey(String senderPublicKey) {
        this.senderPublicKey = senderPublicKey;
    }

    public String getDestinationPublicKey() {
        return destinationPublicKey;
    }

    public void setDestinationPublicKey(String destinationPublicKey) {
        this.destinationPublicKey = destinationPublicKey;
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
