import java.io.Serializable;
import java.util.HashMap;
import java.util.UUID;

public class User implements Serializable {

    public String peerName;
    public String publicKey;
    HashMap<String,String> keys = new HashMap<String,String>();
    HashMap<String,String> usernames = new HashMap<String,String>();

    public User() {
        peerName = "";
        usernames.put("Banana", "");
        usernames.put("Maçã", "");
        usernames.put("Pera", "");
        usernames.put("Uva", "");
        usernames.put("Laranja", "");
        usernames.put("Morango", "");

        String publicKey = UUID.randomUUID().toString();
        String privateKey = UUID.randomUUID().toString();

        keys.put(publicKey, privateKey);
        this.publicKey = publicKey;
    }

    public void setUsername(){
        for(HashMap.Entry<String, String> username : usernames.entrySet()){
            if (username.getValue() == ""){

                this.peerName = username.getKey();

                break;
            }
        }
    }

    public String getPeerName() {
        return peerName;
    }

    public void setPeerName(String peerName) {
        this.peerName = peerName;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public HashMap<String, String> getKeys() {
        return keys;
    }

    public void setKeys(HashMap<String, String> keys) {
        this.keys = keys;
    }

    public HashMap<String, String> getUsernames() {
        return usernames;
    }

    public void setUsernames(HashMap<String, String> usernames) {
        this.usernames = usernames;
    }
}
