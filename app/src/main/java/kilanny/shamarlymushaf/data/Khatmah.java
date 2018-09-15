package kilanny.shamarlymushaf.data;

import java.io.Serializable;
import java.util.Date;

public class Khatmah implements Serializable {
    static final long serialVersionUID = 0L;

    public Khatmah() {
        page = 2;
    }

    public String name;
    public int page;
    public Date lastReadDate;
}
