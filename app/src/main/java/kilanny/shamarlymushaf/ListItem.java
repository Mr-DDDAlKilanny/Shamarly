package kilanny.shamarlymushaf;

import java.io.Serializable;

public class ListItem implements Serializable {
    public String name;
    public Object value;

    public ListItem() {
    }

    public ListItem(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public String toString() {
        return name;
    }
}
