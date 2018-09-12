import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.*;

public class User implements Serializable {

    private String peerName;
    private String publicKey;
    private int port;

    //Hash que guarda a chave pública de todos os peers conectados
    HashMap<String,String> keys = new HashMap<String,String>();

    //Hash de todos os usernames dos peers conectados
    HashMap<String,String> usernames = new HashMap<String,String>();

    //Hash utilizado para controlar os peers que estão respondendo e identificar peers desconectados
    HashMap<String,String> ackPeers = new HashMap<String,String>();

    //Hash contendo todos os recursos compartilhados
    HashMap<Integer,Resource> resources = new HashMap<Integer, Resource>();

    //Fila atual para cada recurso
    HashMap<Integer,PriorityQueue<ResourceEventPackage>> resourcesQueues = new HashMap<Integer, PriorityQueue<ResourceEventPackage>>();

    public User(int port) {

        this.peerName = "";
        this.port = port;

        String publicKey = UUID.randomUUID().toString();
        String privateKey = UUID.randomUUID().toString();

        keys.put(publicKey, privateKey);

        this.resourcesQueues = new HashMap<Integer, PriorityQueue<ResourceEventPackage>>();
        this.publicKey = publicKey;
    }

    public HashMap<String, String> getUsernames() {
        return usernames;
    }

    public void setUsernames(HashMap<String, String> usernames) {
        this.usernames = usernames;
    }

    public HashMap<Integer, PriorityQueue<ResourceEventPackage>> getResourcesQueues() {
        return resourcesQueues;
    }

    public void setResourcesQueues(HashMap<Integer, PriorityQueue<ResourceEventPackage>> resourcesQueues) {
        this.resourcesQueues = resourcesQueues;
    }

    public HashMap<String, String> getAckPeers() {
        return ackPeers;
    }

    public void setAckPeers(HashMap<String, String> ackPeers) {
        this.ackPeers = ackPeers;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public HashMap<Integer, Resource> getResources() {
        return resources;
    }

    public void setResources(HashMap<Integer, Resource> resourcesState) {
        this.resources = resourcesState;
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

}
