
public class Collaborator {
	private String name;
    private String category;
    private int maxTasks;
    private int currentTasks = 0;

    public Collaborator(String name, String category) {
        this.name = name;
        this.category = category;

        switch (category) {
            case "Junior": maxTasks = 10; break;
            case "Intermediate": maxTasks = 5; break;
            case "Senior": maxTasks = 2; break;
        }
    }

    public boolean canAcceptTask() {
        return currentTasks < maxTasks;
    }

    public void assignTask() {
        if (!canAcceptTask()) {
            throw new RuntimeException("Task limit reached for " + name);
        }
        currentTasks++;
    }

    public String getName() { return name; }
    public String getCategory() { return category; }
}
