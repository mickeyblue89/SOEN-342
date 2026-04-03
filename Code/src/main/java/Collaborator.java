import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public abstract class Collaborator implements Serializable {
    private static final long serialVersionUID = 1L;

    protected int c_id;
    protected String c_name;
    protected List<Subtask> c_tasks;
    protected int num_open_tasks;
    protected int maxTasks;
    protected Integer p_id;

    protected Collaborator(int c_id, String c_name, int maxTasks) {
        this.c_id = c_id;
        this.c_name = c_name;
        this.maxTasks = maxTasks;
        this.c_tasks = new ArrayList<>();
        this.num_open_tasks = 0;
    }

    public void addToProject(int p_id) {
        this.p_id = p_id;
    }

    public boolean canAcceptTask() {
        refreshOpenTaskCount();
        return num_open_tasks < maxTasks;
    }

    public void assignSubtask(Subtask subtask) {
        c_tasks.add(subtask);
        refreshOpenTaskCount();
    }

    public void refreshOpenTaskCount() {
        this.num_open_tasks = (int) c_tasks.stream().filter(task -> !task.isCompleted()).count();
    }

    public boolean isOverloaded() {
        refreshOpenTaskCount();
        return num_open_tasks > maxTasks;
    }

    public int getC_id() {
        return c_id;
    }

    public String getC_name() {
        return c_name;
    }

    public List<Subtask> getC_tasks() {
        return c_tasks;
    }

    public int getNum_open_tasks() {
        refreshOpenTaskCount();
        return num_open_tasks;
    }

    public int getMaxTasks() {
        return maxTasks;
    }

    public Integer getProjectId() {
        return p_id;
    }
}
