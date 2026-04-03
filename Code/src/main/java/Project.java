import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Project implements Serializable {
    private static final long serialVersionUID = 1L;

    private int p_id;
    private String p_name;
    private String p_description;
    private final List<Task> tasks;
    private final List<Collaborator> collaborators;

    public Project(int p_id, String p_name, String p_description) {
        this.p_id = p_id;
        this.p_name = p_name;
        this.p_description = p_description == null ? "" : p_description;
        this.tasks = new ArrayList<>();
        this.collaborators = new ArrayList<>();
    }

    public static Project createProject(String name, String description) {
        return new Project(0, name, description);
    }

    public Task addTask(Task task) {
        if (task.getProject() != null && task.getProject() != this) {
            task.getProject().removeTask(task);
        }
        if (!tasks.contains(task)) {
            tasks.add(task);
        }
        task.setProject(this);
        task.addActivity("Assigned to project " + p_name);
        return task;
    }

    public void removeTask(Task task) {
        tasks.remove(task);
        if (task.getProject() == this) {
            task.setProject(null);
            task.addActivity("Removed from project " + p_name);
        }
    }

    public void addCollaborator(Collaborator collaborator) {
        if (!collaborators.contains(collaborator)) {
            collaborators.add(collaborator);
            collaborator.addToProject(p_id);
        }
    }

    public int getP_id() {
        return p_id;
    }

    public void setP_id(int p_id) {
        this.p_id = p_id;
    }

    public String getP_name() {
        return p_name;
    }

    public String getP_description() {
        return p_description;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public List<Collaborator> getCollaborators() {
        return collaborators;
    }
}
