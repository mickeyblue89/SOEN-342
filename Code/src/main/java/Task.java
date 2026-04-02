import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Task implements Serializable {
    private static final long serialVersionUID = 1L;

    private int t_id;
    private String t_title;
    private String t_description;
    private Date creationDate;
    private int priorityLevel;
    private String status;
    private Date dueDate;
    private boolean t_completed;
    private Project project;
    private final List<Subtask> subtasks;
    private final List<ActivityEntry> activityEntries;
    private final Set<Tag> tags;
    private Recurrence recurrence;

    public Task(int t_id, String t_title, String t_description, int priorityLevel) {
        this.t_id = t_id;
        this.t_title = t_title;
        this.t_description = t_description == null ? "" : t_description;
        this.creationDate = new Date();
        this.priorityLevel = priorityLevel;
        this.status = "open";
        this.t_completed = false;
        this.subtasks = new ArrayList<>();
        this.activityEntries = new ArrayList<>();
        this.tags = new LinkedHashSet<>();
        addActivity("Task created");
    }

    public static Task createTask(String title, String description, int priority, LocalDate dueDate, Recurrence recurrence) {
        Task task = new Task(0, title, description, priority);
        if (dueDate != null) {
            task.setDueDate(java.sql.Date.valueOf(dueDate));
        }
        task.setRecurrence(recurrence);
        return task;
    }

    public void updateTask(String attribute, Object value) {
        String normalized = attribute == null ? "" : attribute.trim().toLowerCase();
        switch (normalized) {
            case "title":
                this.t_title = Objects.toString(value, "").trim();
                addActivity("Title updated");
                break;
            case "description":
                this.t_description = Objects.toString(value, "");
                addActivity("Description updated");
                break;
            case "priority":
            case "prioritylevel":
                this.priorityLevel = Integer.parseInt(String.valueOf(value));
                addActivity("Priority updated");
                break;
            case "status":
                setStatus(normalizeUpdatedStatus(value));
                break;
            case "duedate":
            case "due date":
                setDueDate(parseDate(value));
                break;
            case "project":
                if (value instanceof Project newProject) {
                    setProject(newProject);
                }
                addActivity("Project association updated");
                break;
            case "recurrence":
                if (value instanceof Recurrence recurrenceValue) {
                    setRecurrence(recurrenceValue);
                    addActivity("Recurrence updated");
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported task attribute: " + attribute);
        }
    }

    public void organizeTask(Tag tag) {
        if (tag != null && tags.add(tag)) {
            tag.addTask(this);
            addActivity("Tag added: " + tag.getKeyword());
        }
    }

    public Task createSubtask(Task task) {
        Subtask subtask = new Subtask(0, task.getTitle(), task.getDescription(), 0);
        addSubtask(subtask);
        return subtask;
    }

    public Subtask createSubtask(String title, String description) {
        Subtask subtask = new Subtask(0, title, description, 0);
        addSubtask(subtask);
        return subtask;
    }

    public void addSubtask(Subtask subtask) {
        if (subtasks.size() >= 20) {
            throw new IllegalStateException("A task cannot have more than 20 sub-tasks.");
        }
        subtasks.add(subtask);
        subtask.setParentTask(this);
        addActivity("Subtask added: " + subtask.getTitle());
    }

    public Task viewTask(Map<String, Object> criteria) {
        return matches(criteria) ? this : null;
    }

    public static List<Task> viewTasks(List<Task> tasks, String viewBy, Map<String, Object> criteria) {
        Map<String, Object> filters = criteria == null ? Map.of() : criteria;
        Comparator<LocalDate> dateOrder = Comparator.nullsLast(Comparator.naturalOrder());

        return tasks.stream()
                .filter(task -> task.matches(filters))
                .sorted(getViewComparator(viewBy, dateOrder))
                .toList();
    }

    public List<Task> searchTask(Map<String, Object> criteria) {
        TaskDB db = TaskDB.getInstance();
        if (db == null) {
            List<Task> self = new ArrayList<>();
            if (matches(criteria)) {
                self.add(this);
            }
            return self;
        }
        return db.searchTasks(criteria);
    }

    public boolean isCompleted() {
        return t_completed;
    }

    public void assignCollaborator(Collaborator collaborator) {
        if (project == null) {
            throw new IllegalStateException("Collaborators can only be linked to project tasks.");
        }
        if (collaborator == null) {
            throw new IllegalArgumentException("Collaborator is required.");
        }
        if (!collaborator.canAcceptTask()) {
            throw new IllegalStateException("Collaborator is overloaded: " + collaborator.getC_name());
        }

        String title = "Assigned to " + collaborator.getC_name();
        Subtask subtask = createSubtask(title, "Collaboration subtask for task " + getTitle());
        subtask.addCollaborator(collaborator.getC_id());
        subtask.linkCollaborator(collaborator);
        collaborator.assignSubtask(subtask);
        addActivity("Collaborator linked: " + collaborator.getC_name());
    }

    public boolean matches(Map<String, Object> criteria) {
        if (criteria == null || criteria.isEmpty()) {
            return "open".equalsIgnoreCase(status);
        }

        Object titleMatch = criteria.get("taskName");
        if (titleMatch == null) {
            titleMatch = criteria.get("title");
        }
        if (titleMatch != null) {
            String keyword = String.valueOf(titleMatch).toLowerCase();
            String haystack = (t_title + " " + t_description).toLowerCase();
            if (!haystack.contains(keyword)) {
                return false;
            }
        }

        Object statusMatch = criteria.get("status");
        if (statusMatch != null && !status.equalsIgnoreCase(String.valueOf(statusMatch))) {
            return false;
        }

        Object priorityMatch = criteria.get("priority");
        if (priorityMatch != null && priorityLevel != Integer.parseInt(String.valueOf(priorityMatch))) {
            return false;
        }

        Object projectMatch = criteria.get("project");
        if (projectMatch != null) {
            String projectName = project == null ? "" : project.getP_name();
            if (!projectName.equalsIgnoreCase(String.valueOf(projectMatch))) {
                return false;
            }
        }

        Object tagMatch = criteria.get("tag");
        if (tagMatch != null) {
            boolean found = tags.stream().anyMatch(tag -> tag.getKeyword().equalsIgnoreCase(String.valueOf(tagMatch)));
            if (!found) {
                return false;
            }
        }

        LocalDate localDueDate = getDueDateAsLocalDate();
        Object from = criteria.get("from");
        if (from != null) {
            LocalDate fromDate = (LocalDate) from;
            if (localDueDate == null || localDueDate.isBefore(fromDate)) {
                return false;
            }
        }

        Object to = criteria.get("to");
        if (to != null) {
            LocalDate toDate = (LocalDate) to;
            if (localDueDate == null || localDueDate.isAfter(toDate)) {
                return false;
            }
        }

        Object date = criteria.get("date");
        if (date != null) {
            LocalDate target = (LocalDate) date;
            if (!target.equals(localDueDate)) {
                return false;
            }
        }

        Object dayOfWeek = criteria.get("dayOfWeek");
        if (dayOfWeek != null) {
            if (localDueDate == null || localDueDate.getDayOfWeek() != DayOfWeek.valueOf(String.valueOf(dayOfWeek).toUpperCase())) {
                return false;
            }
        }

        return true;
    }

    public void addActivity(String description) {
        activityEntries.add(ActivityEntry.createEntry(new Date(), description));
    }

    public int getId() {
        return t_id;
    }

    public void setId(int t_id) {
        this.t_id = t_id;
    }

    public String getTitle() {
        return t_title;
    }

    public void setTitle(String title) {
        this.t_title = title;
    }

    public String getDescription() {
        return t_description;
    }

    public void setDescription(String description) {
        this.t_description = description == null ? "" : description;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public int getPriority() {
        return priorityLevel;
    }

    public void setPriority(int priorityLevel) {
        this.priorityLevel = priorityLevel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status == null ? "open" : status.toLowerCase();
        this.t_completed = "completed".equalsIgnoreCase(this.status);
        addActivity("Status changed to " + this.status);
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
        addActivity(dueDate == null ? "Due date cleared" : "Due date updated");
    }

    public LocalDate getDueDateAsLocalDate() {
        if (dueDate == null) {
            return null;
        }
        return dueDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public boolean isOpenWithoutDueDate() {
        return "open".equalsIgnoreCase(status) && dueDate == null;
    }

    public List<Subtask> getSubtasks() {
        return subtasks;
    }

    public List<ActivityEntry> getActivityEntries() {
        return activityEntries;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Recurrence getRecurrence() {
        return recurrence;
    }

    public void setRecurrence(Recurrence recurrence) {
        this.recurrence = recurrence;
    }

    protected static Date parseDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Date dateValue) {
            return dateValue;
        }
        if (value instanceof LocalDate localDate) {
            return java.sql.Date.valueOf(localDate);
        }
        return java.sql.Date.valueOf(LocalDate.parse(String.valueOf(value)));
    }

    private static String normalizeUpdatedStatus(Object value) {
        String normalized = Objects.toString(value, "open").trim().toLowerCase();
        return switch (normalized) {
            case "open" -> "open";
            case "completed" -> "completed";
            default -> throw new IllegalArgumentException("Status must be either 'open' or 'completed'.");
        };
    }

    private static Comparator<Task> getViewComparator(String viewBy, Comparator<LocalDate> dateOrder) {
        String normalized = viewBy == null ? "" : viewBy.trim().toLowerCase();
        return switch (normalized) {
            case "priority" -> Comparator.comparingInt(Task::getPriority).reversed()
                    .thenComparing(Task::getDueDateAsLocalDate, dateOrder);
            case "status" -> Comparator.comparing(Task::getStatus, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(Task::getDueDateAsLocalDate, dateOrder);
            case "project" -> Comparator.comparing((Task task) -> task.getProject() == null ? "" : task.getProject().getP_name(),
                            String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(Task::getDueDateAsLocalDate, dateOrder);
            default -> Comparator.comparing(Task::getDueDateAsLocalDate, dateOrder);
        };
    }
}
