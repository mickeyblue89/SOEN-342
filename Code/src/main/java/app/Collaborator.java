package app;

import java.util.*;

public abstract class Collaborator {

    protected int id;
    protected String name;
    protected int maxTasks;
    protected List<Subtask> tasks = new ArrayList<>();

    public boolean canAcceptTask() {
        return getOpenTasks() < maxTasks;
    }

    public void addTask(Subtask task) {
        tasks.add(task);
    }

    public int getOpenTasks() {
        int count = 0;
        for (Subtask t : tasks) {
            if (!t.isCompleted()) count++;
        }
        return count;
    }

    public boolean isOverloaded() {
        return getOpenTasks() > maxTasks;
    }

    public String getName() { return name; }
}
