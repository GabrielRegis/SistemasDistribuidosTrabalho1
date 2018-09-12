import java.io.Serializable;
import java.util.Comparator;

public class ResourceComparator implements Serializable, Comparator<ResourceEventPackage> {
    @Override
    public int compare(ResourceEventPackage o1, ResourceEventPackage o2) {
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        return false;
    }
}
