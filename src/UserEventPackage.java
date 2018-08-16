import java.io.Serializable;

// Representa o tipo de evento de usuário que ocorreu
enum UserEventType {
    USER_CONNECTED(0), USER_DISCONNECTED(1), USER_DROPED(2), NEW_USER_REQUEST(3), NEW_USER_RESPONSE(4);

    public int userEventIndex;
    UserEventType(int index) {
        userEventIndex = index;
    }
}

public class UserEventPackage implements Serializable {

    private UserEventType type;
    private User user;
    private String message;

    public UserEventPackage(UserEventType type, User user) {
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

    public UserEventType getType() {
        return type;
    }

    public void setType(UserEventType type) {
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
