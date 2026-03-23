import java.time.LocalDate;

public class ActivityEntry {

    private Task task;
    private int ae_id;
    private LocalDate timestamp;
    private String ae_description;

    public ActivityEntry(Task task, LocalDate timestamp, String ae_description) {
        this.task = task;
        this.timestamp = timestamp;
        this.ae_description = ae_description;
    }

    public Task getTask() {
        return task;
    }

    public LocalDate getTimestamp() {
        return timestamp;
    }

    public String getAe_description() {
        return ae_description;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public void setTimestamp(LocalDate timestamp) {
        this.timestamp = timestamp;
    }

    public void setAe_description(String ae_description) {
        this.ae_description = ae_description;
    }
  
}
