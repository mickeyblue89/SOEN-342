import java.time.LocalDateTime;

public class ActivityEntry {

    private int ae_id;
    private int taskId;
    private LocalDateTime timestamp;
    private String description;

    public ActivityEntry(int taskId, String description) {
        this.taskId = taskId;
        this.description = description;
        this.timestamp = LocalDateTime.now();
    }
    
    public ActivityEntry getEntries() {// to implement
    	ActivityEntry a = null;
    	return a;
    }

    public int getTaskId() { return taskId; }
    public String getDescription() { return description; }
    public LocalDateTime getTimestamp() { return timestamp; }
}