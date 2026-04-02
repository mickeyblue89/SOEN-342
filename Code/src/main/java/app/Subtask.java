package app;

public class Subtask extends Task {

    private Collaborator collaborator;

    public Subtask(int id, String title, String description, int priority) {
        super(id, title, description, priority);
    }


    public void addCollaborator(Collaborator c) {
        if (!c.canAcceptTask()) {
            throw new RuntimeException("Collaborator overloaded");
        }

        this.collaborator = c;
        c.addTask(this);
    }
    
    public void removeCollaborator(Collaborator c) { //to check
    	this.collaborator = null;
    }

    public Collaborator getCollaborator() {
        return collaborator;
    }
}
