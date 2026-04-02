import java.io.Serializable;

public class Subtask extends Task implements Serializable {
    private static final long serialVersionUID = 1L;

    private int s_id;
    private String s_title;
    private String s_description;
    private boolean s_completed;
    private Task parentTask;
    private int collaboratorId = -1;
    private Collaborator collaborator;

    public Subtask(int s_id, String s_title, String s_description, int priorityLevel) {
        super(s_id, s_title, s_description, priorityLevel);
        this.s_id = s_id;
        this.s_title = s_title;
        this.s_description = s_description == null ? "" : s_description;
        this.s_completed = false;
    }

    public void addCollaborator(int c_id) {
        this.collaboratorId = c_id;
    }

    public void removeCollaborator(int c_id) {
        if (this.collaboratorId == c_id) {
            this.collaboratorId = -1;
            this.collaborator = null;
        }
    }

    public void markCompleted() {
        this.s_completed = true;
        setStatus("completed");
    }

    public void reopen() {
        this.s_completed = false;
        setStatus("open");
    }

    public void linkCollaborator(Collaborator collaborator) {
        this.collaborator = collaborator;
        if (collaborator != null) {
            this.collaboratorId = collaborator.getC_id();
        }
    }

    public int getS_id() {
        return s_id;
    }

    @Override
    public void setId(int s_id) {
        this.s_id = s_id;
        super.setId(s_id);
    }

    public String getS_title() {
        return s_title;
    }

    @Override
    public String getTitle() {
        return s_title;
    }

    @Override
    public void setTitle(String title) {
        this.s_title = title;
        super.setTitle(title);
    }

    public String getS_description() {
        return s_description;
    }

    @Override
    public String getDescription() {
        return s_description;
    }

    @Override
    public void setDescription(String description) {
        this.s_description = description == null ? "" : description;
        super.setDescription(description);
    }

    public boolean isS_completed() {
        return s_completed;
    }

    @Override
    public boolean isCompleted() {
        return s_completed;
    }

    public Task getParentTask() {
        return parentTask;
    }

    public void setParentTask(Task parentTask) {
        this.parentTask = parentTask;
    }

    public int getCollaboratorId() {
        return collaboratorId;
    }

    public Collaborator getCollaborator() {
        return collaborator;
    }
}
