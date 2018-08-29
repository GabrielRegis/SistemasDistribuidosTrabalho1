import java.io.Serializable;
import java.util.UUID;

// Representa o estado de um usuário em relação ao recurso
enum ResourceState {

    RELEASED(0), WANTED(1), HELD(2);

    public int userEventIndex;
    ResourceState(int index) {
        userEventIndex = index;
    }
}

public class Resource implements Serializable {
    private String resourceName;
    private Integer resourceKey;
    private ResourceState resourceState;

    public Resource(String resourceName) {
        this.resourceName = resourceName;
        this.resourceState = ResourceState.RELEASED;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public Integer getResourceKey() {
        return resourceKey;
    }

    public void setResourceKey(Integer resourceKey) {
        this.resourceKey = resourceKey;
    }

    public ResourceState getResourceState() {
        return resourceState;
    }

    public void setResourceState(ResourceState resourceState) {
        this.resourceState = resourceState;
    }
}
