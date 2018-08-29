import java.time.LocalTime;

enum ResourceResponse {

    FREE(0), OCCUPIED(1);

    public int userEventIndex;
    ResourceResponse(int index) {
        userEventIndex = index;
    }
}

public class ResourceEventPackage extends EventPackage{

    private Resource resource;
    private LocalTime timeStamp;
    private ResourceResponse response;

    public ResourceEventPackage(EventType type, User user, Resource resource) {
        super(type, user);
        this.resource = resource;
        response = ResourceResponse.OCCUPIED;
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
