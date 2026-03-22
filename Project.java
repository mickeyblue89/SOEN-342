import java.util.ArrayList;
import java.util.List;

public class Project {
	private static int counter = 1;

    private int id;
    private String name;
    private String description;

    private List<Task> tasks = new ArrayList<>();
    private List<Collaborator> collaborators = new ArrayList<>();

    public Project(String name, String description) {
        this.id = counter++;
        this.name = name;
        this.description = description;
    }

    public void addTask(Task task) {
        tasks.add(task);
        task.setProject(this);
    }
    
    public void addCollaborator(Collaborator c) {
        collaborators.add(c);
    }
    
    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<Task> getTasks() { return tasks; }
    public List<Collaborator> getCollaborators() { return collaborators; }

}
