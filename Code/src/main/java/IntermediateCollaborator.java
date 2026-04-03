public class IntermediateCollaborator extends Collaborator {
    private static final long serialVersionUID = 1L;

    private int i_Max_Tasks = 5;

    public IntermediateCollaborator(int c_id, String c_name) {
        super(c_id, c_name, 5);
    }

    public void changeMaxTasks(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("The limit for open tasks for each collaborator category is a positive integer.");
        }
        this.i_Max_Tasks = limit;
        this.maxTasks = limit;
    }
}
