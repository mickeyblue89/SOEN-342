package app;

import java.util.ArrayList;
import java.util.List;

public class Project {

    private int p_id;
    private String p_name;
    private String p_description;

    private List<Task> tasks;

    public Project(int id, String name, String description) {
        this.p_id = id;
        this.p_name = name;
        this.p_description = description;
        this.tasks = new ArrayList<>();
    }

    public void addTask(Task task) {

        if (task.getProject() != null) {
            throw new RuntimeException("Task already belongs to a project");
        }

        tasks.add(task);
        task.setProject(this);
    }

    public void removeTask(Task task) {
        tasks.remove(task);
        task.setProject(null);
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public String getName() {
        return p_name;
    }

    public String getDescription() {
        return p_description;
    }
}
