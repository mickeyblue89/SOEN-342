import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ActivityEntry implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final AtomicInteger SEQUENCE = new AtomicInteger(1);

    private int ae_id;
    private Date timestamp;
    private String ae_description;

    public ActivityEntry(int ae_id, Date timestamp, String ae_description) {
        this.ae_id = ae_id;
        this.timestamp = timestamp;
        this.ae_description = ae_description;
    }

    public static ActivityEntry createEntry(Date timestamp, String description) {
        return new ActivityEntry(SEQUENCE.getAndIncrement(), timestamp, description);
    }

    public List<ActivityEntry> getEntries(int ae_id) {
        List<ActivityEntry> entries = new ArrayList<>();
        if (this.ae_id == ae_id) {
            entries.add(this);
        }
        return entries;
    }

    public int getAe_id() {
        return ae_id;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getAe_description() {
        return ae_description;
    }
}
