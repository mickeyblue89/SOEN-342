import java.time.LocalDate;

public class Task {
	private static int counter = 1;

    private int id;
    private String title;
    private String description;
    private String status;
    private int priority;
    private LocalDate dueDate;
    private Project project;
    private Collaborator collaborator;

    public Task(String title, int priority, String status) {
        this.id = counter++;
        this.title = title;
        this.priority = priority;
        this.status = status;
    }

    // Getters
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getStatus() { return status; }
    public int getPriority() { return priority; }
    public LocalDate getDueDate() { return dueDate; }
    public Project getProject() { return project; }
    public Collaborator getCollaborator() { return collaborator; }

    // Setters
    public void setDescription(String description) { this.description = description; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public void setProject(Project project) { this.project = project; }
    public void setCollaborator(Collaborator collaborator) { this.collaborator = collaborator; }
    public void setTitle(String title) {
    this.title = title;
}

public void setStatus(String status) {
    this.status = status;
}

public void setPriority(int priority) {
    this.priority = priority;
}

}
