import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.*;

public class User implements Serializable {

    private String peerName;
    private String publicKey;
    private int port;

    HashMap<String,String> keys = new HashMap<String,String>();
    HashMap<Integer,Resource> resources = new HashMap<Integer, Resource>();
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

    public HashMap<Integer, PriorityQueue<ResourceEventPackage>> getResourcesQueues() {
        return resourcesQueues;
    }

    public void setResourcesQueues(HashMap<Integer, PriorityQueue<ResourceEventPackage>> resourcesQueues) {
        this.resourcesQueues = resourcesQueues;
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
