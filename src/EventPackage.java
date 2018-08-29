import java.io.Serializable;

// Representa o tipo de evento que ocorreu
enum EventType {
    USER_CONNECTED(0), USER_DISCONNECTED(1), USER_DROPED(2), USER_CONNECTED_RESPONSE(3),
    REQUEST_CONNECTED_USERS(4), CONNECTED_USERS_RESPONSE(5), RESOURCE_REQUEST(6), RESOURCE_RESPONSE(7),
    RELEASE_RESOURCE(8), CREATE_RESOURCE(9), UPDATE_RESOURCE_QUEUE(10);

    public int userEventIndex;
    EventType(int index) {
        userEventIndex = index;
    }
}

public class EventPackage implements Serializable {

    private EventType type;
    private User user;
    private String destinationPublicKey;
    private String message;

    public EventPackage(EventType type, User user) {
        this.type = type;
        this.user = user;
        switch (type){
            case USER_CONNECTED:
                this.message = "Usuário: " + user.getPeerName() + " conectado.";
                break;
            case USER_DISCONNECTED:
                this.message = "Usuário: " + user.getPeerName() + " se desconectou.";
                break;
            case USER_DROPED:
                this.message = "Usuário: " + user.getPeerName() + " caiu.";
                break;
            default:
                this.message = "";
                break;
        }

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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
