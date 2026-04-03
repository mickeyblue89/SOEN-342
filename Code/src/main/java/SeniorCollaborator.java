public class SeniorCollaborator extends Collaborator {
    private static final long serialVersionUID = 1L;

    private int s_max_Tasks = 2;

    public SeniorCollaborator(int c_id, String c_name) {
        super(c_id, c_name, 2);
    }

    public void changeMaxTasks(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("The limit for open tasks for each collaborator category is a positive integer.");
        }
        this.s_max_Tasks = limit;
        this.maxTasks = limit;
    }
}
