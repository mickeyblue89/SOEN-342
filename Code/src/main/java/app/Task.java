package app;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Task {

    private int t_id;
    private String t_title;
    private String t_description;
    private LocalDate creationDate;
    private int priorityLevel;
    private String status;
    private LocalDate dueDate;
    private boolean t_completed;
    
    private Project project;
    private List<Subtask> subtasks;
    private List<ActivityEntry> activityLog;

    public Task(int id, String title, String description, int priority) {
        this.t_id = id;
        this.t_title = title;
        this.t_description = description;
        this.priorityLevel = priority;
        this.creationDate = LocalDate.now();
        this.status = "open";
        this.subtasks = new ArrayList<>();
        this.activityLog = new ArrayList<>();
        this.t_completed = false;

        addActivity("Task created");
    }
    
    public void updateTask(/*attribute*/) { //to implement
    	
    }
    
    public void updateTitle(String newTitle) {
        this.t_title = newTitle;
        addActivity("Title updated");
    }

    public void updateStatus(String newStatus) {
        this.status = newStatus;
        addActivity("Status changed to " + newStatus);
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
        addActivity("Due date set");
    }
    
    public void organizeTag(Tag tag) { //to implement
    	
    }
    
    public void createSubtask(String name, String desc) {
    	Subtask sub = new Subtask(0,name,desc,0);
    	this.addSubtask(sub);
    }

    public void addSubtask(Subtask subtask) {
        if (subtasks.size() >= 20) {
            throw new RuntimeException("A task cannot have more than 20 subtasks");
        }
        subtasks.add(subtask);
        addActivity("Subtask added: " + subtask.getTitle());
    }
    
    public Task viewTask(/*criteria*/) { //to implement
    	Task t = new Task(0,"","",0);
    	return  t;
    }
    
    public Task searchTask(/*criteria*/) { //to implement
    	Task t = new Task(0,"","",0);
    	return  t;
    }

    private void addActivity(String desc) {
        activityLog.add(new ActivityEntry(this.t_id, desc));
    }
    
    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }
    
    public boolean isCompleted() {
        return t_completed;
    }	

    public int getId() { return t_id; }
    public String getTitle() { return t_title; }
    public String getDescription() { return t_description; }
    public LocalDate getCreationDate() { return creationDate; }
    public int getPriority() { return priorityLevel; }
    public String getStatus() { return status; }
    public LocalDate getDueDate() { return dueDate; }
    public List<Subtask> getSubtasks() { return subtasks; }

}
