import java.time.LocalTime;
import java.util.HashMap;
import java.util.PriorityQueue;

enum ResourceResponse {

    FREE(0), OCCUPIED(1);

    public int userEventIndex;
    ResourceResponse(int index) {
        userEventIndex = index;
    }
}

// Pacotes enviados para controle de recurso.
public class ResourceEventPackage extends EventPackage{

    // Recurso atrelado ao pacote
    private Resource resource;

    // Timestamp utilizado para controle de prioridade
    private LocalTime timeStamp;

    // Em caso de resposta, representa a resposta em relação à um pacote (Liberado ou Ocupado)
    private ResourceResponse response;

    // A fila de recursos é passada para alertar os peers do estado da fila de peers esperando pelo recurso
    HashMap<Integer,PriorityQueue<ResourceEventPackage>> resourcesQueues = new HashMap<Integer, PriorityQueue<ResourceEventPackage>>();

    public ResourceEventPackage(EventType type, User user, Resource resource) {
        super(type, user.getPeerName(), user.getPublicKey());
        this.resourcesQueues = user.getResourcesQueues();
        this.resource = resource;
        response = ResourceResponse.OCCUPIED;
    }

    public HashMap<Integer, PriorityQueue<ResourceEventPackage>> getResourcesQueues() {
        return resourcesQueues;
    }

    public void setResourcesQueues(HashMap<Integer, PriorityQueue<ResourceEventPackage>> resourcesQueues) {
        this.resourcesQueues = resourcesQueues;
    }

    public ResourceResponse getResponse() {
        return response;
    }

    public void setResponse(ResourceResponse response) {
        this.response = response;
    }

    public LocalTime getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(LocalTime timeStamp) {
        this.timeStamp = timeStamp;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }
}
