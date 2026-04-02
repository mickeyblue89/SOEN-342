import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

public class Tag implements Serializable {
    private static final long serialVersionUID = 1L;

    private String keyword;
    private final Set<Task> tasks;

    public Tag(String keyword) {
        this.keyword = keyword;
        this.tasks = new LinkedHashSet<>();
    }

    public void addTask(Task task) {
        tasks.add(task);
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Set<Task> getTasks() {
        return tasks;
    }
}
