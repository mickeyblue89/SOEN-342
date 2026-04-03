public class JuniorCollaborator extends Collaborator {
    private static final long serialVersionUID = 1L;

    private int j_Max_Tasks = 10;

    public JuniorCollaborator(int c_id, String c_name) {
        super(c_id, c_name, 10);
    }

    public void changeMaxTasks(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("The limit for open tasks for each collaborator category is a positive integer.");
        }
        this.j_Max_Tasks = limit;
        this.maxTasks = limit;
    }
}
