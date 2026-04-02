import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TaskDB implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Path STORAGE_PATH = Path.of("taskdb.ser");
    private static TaskDB INSTANCE;

    private final Map<Integer, Task> tasks;
    private final Map<Integer, Project> projects;
    private final Map<String, Project> projectsByName;
    private final Map<String, Tag> tags;
    private final Map<Integer, Collaborator> collaborators;
    private int nextTaskId;
    private int nextProjectId;
    private int nextCollaboratorId;
    private int nextSubtaskId;

    public TaskDB() {
        TaskDB loaded = load();
        if (loaded == null) {
            this.tasks = new LinkedHashMap<>();
            this.projects = new LinkedHashMap<>();
            this.projectsByName = new HashMap<>();
            this.tags = new LinkedHashMap<>();
            this.collaborators = new LinkedHashMap<>();
            this.nextTaskId = 1;
            this.nextProjectId = 1;
            this.nextCollaboratorId = 1;
            this.nextSubtaskId = 1;
        } else {
            this.tasks = loaded.tasks;
            this.projects = loaded.projects;
            this.projectsByName = loaded.projectsByName;
            this.tags = loaded.tags;
            this.collaborators = loaded.collaborators;
            this.nextTaskId = loaded.nextTaskId;
            this.nextProjectId = loaded.nextProjectId;
            this.nextCollaboratorId = loaded.nextCollaboratorId;
            this.nextSubtaskId = loaded.nextSubtaskId;
        }
        INSTANCE = this;
    }

    public static TaskDB getInstance() {
        return INSTANCE;
    }

    public Task addTask(Task task) {
        Objects.requireNonNull(task, "Task is required");
        List<Task> storedTasks = new ArrayList<>();

        if (task.getRecurrence() != null) {
            storedTasks.addAll(task.getRecurrence().createOccurence(task, task.getRecurrence().getR_type()));
        } else {
            storedTasks.add(task);
        }

        Task first = null;
        for (Task entry : storedTasks) {
            validateUniqueNameAndDueDate(entry);
            entry.setId(nextTaskId++);
            tasks.put(entry.getId(), entry);
            if (entry.getProject() != null) {
                entry.getProject().addTask(entry);
            }
            validateOpenTasksWithoutDueDate();
            if (first == null) {
                first = entry;
            }
        }
        save();
        return first;
    }

    public Task getTask(int t_id) {
        Task task = tasks.get(t_id);
        if (task == null) {
            throw new IllegalArgumentException("Task not found: " + t_id);
        }
        return task;
    }

    public void updateTask(int t_id, String attribute, Object value) {
        Task task = getTask(t_id);
        task.updateTask(attribute, value);
        validateOpenTasksWithoutDueDate();
        refreshCollaboratorLoads();
        save();
    }

    public Project addProject(Project project) {
        if (projectsByName.containsKey(project.getP_name().toLowerCase())) {
            throw new IllegalArgumentException("Project name must be unique.");
        }
        project.setP_id(nextProjectId++);
        projects.put(project.getP_id(), project);
        projectsByName.put(project.getP_name().toLowerCase(), project);
        save();
        return project;
    }

    public Project getProject(int p_id) {
        Project project = projects.get(p_id);
        if (project == null) {
            throw new IllegalArgumentException("Project not found: " + p_id);
        }
        return project;
    }

    public Tag findOrCreateTag(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("Tag keyword is required.");
        }
        return tags.computeIfAbsent(keyword.toLowerCase(), key -> new Tag(keyword));
    }

    public Collaborator createCollaborator(String name, String category, int projectId) {
        Project project = getProject(projectId);
        Collaborator collaborator = switch (category.toLowerCase()) {
            case "junior" -> new JuniorCollaborator(nextCollaboratorId++, name);
            case "intermediate" -> new IntermediateCollaborator(nextCollaboratorId++, name);
            case "senior" -> new SeniorCollaborator(nextCollaboratorId++, name);
            default -> throw new IllegalArgumentException("Unknown collaborator category: " + category);
        };
        project.addCollaborator(collaborator);
        collaborators.put(collaborator.getC_id(), collaborator);
        save();
        return collaborator;
    }

    public void assignCollaboratorToTask(int taskId, int collaboratorId) {
        Collaborator collaborator = collaborators.get(collaboratorId);
        if (collaborator == null) {
            throw new IllegalArgumentException("Collaborator not found: " + collaboratorId);
        }
        getTask(taskId).assignCollaborator(collaborator);
        refreshCollaboratorLoads();
        ensureNoCollaboratorOverloaded();
        save();
    }

    public List<Task> searchTasks(Map<String, Object> criteria) {
        Map<String, Object> searchCriteria = criteria == null ? Map.of() : criteria;
        return tasks.values().stream()
                .filter(task -> task.matches(searchCriteria))
                .sorted(Comparator.comparing(Task::getDueDateAsLocalDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public void importCSV(String fileName) throws IOException {
        List<String> lines = Files.readAllLines(Path.of(fileName));
        boolean first = true;
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            if (first) {
                first = false;
                if (line.toLowerCase().contains("taskname")) {
                    continue;
                }
            }

            String[] values = line.split(",", -1);
            if (values.length < 10) {
                throw new IllegalArgumentException("Each CSV row must contain 10 columns.");
            }

            String taskName = values[0].trim();
            String description = values[1].trim();
            String subtaskName = values[2].trim();
            String status = values[3].trim().isBlank() ? "open" : values[3].trim();
            int priority = values[4].trim().isBlank() ? 1 : Integer.parseInt(values[4].trim());
            LocalDate dueDate = values[5].trim().isBlank() ? null : LocalDate.parse(values[5].trim());
            String projectName = values[6].trim();
            String projectDescription = values[7].trim();
            String collaboratorName = values[8].trim();
            String collaboratorCategory = values[9].trim();

            Task task = new Task(0, taskName, description, priority);
            task.setStatus(status);
            if (dueDate != null) {
                task.setDueDate(java.sql.Date.valueOf(dueDate));
            }

            if (!projectName.isBlank()) {
                Project project = projectsByName.get(projectName.toLowerCase());
                if (project == null) {
                    project = addProject(Project.createProject(projectName, projectDescription));
                }
                project.addTask(task);
            }

            Task storedTask = addTask(task);

            if (!subtaskName.isBlank()) {
                Subtask subtask = storedTask.createSubtask(subtaskName, "");
                subtask.setId(nextSubtaskId++);
            }

            if (!collaboratorName.isBlank()) {
                if (storedTask.getProject() == null) {
                    throw new IllegalArgumentException("Collaborators can only be imported for project tasks.");
                }
                Collaborator collaborator = findOrCreateCollaborator(storedTask.getProject(), collaboratorName, collaboratorCategory);
                storedTask.assignCollaborator(collaborator);
            }
        }
        refreshCollaboratorLoads();
        ensureNoCollaboratorOverloaded();
        save();
    }

    public void exportCSV(String fileName) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("TaskName,Description,Subtask,Status,Priority,DueDate,ProjectName,ProjectDescription,Collaborator,CollaboratorCategory");
        for (Task task : tasks.values()) {
            if (task.getSubtasks().isEmpty()) {
                lines.add(toCsvLine(task, null));
                continue;
            }
            for (Subtask subtask : task.getSubtasks()) {
                lines.add(toCsvLine(task, subtask));
            }
        }
        Files.write(Path.of(fileName), lines);
    }

    public List<Collaborator> getAllCollaborators() {
        refreshCollaboratorLoads();
        return new ArrayList<>(collaborators.values());
    }

    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    public void save() {
        try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(STORAGE_PATH.toFile()))) {
            outputStream.writeObject(this);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to persist TaskDB: " + exception.getMessage(), exception);
        }
    }

    private TaskDB load() {
        if (!Files.exists(STORAGE_PATH)) {
            return null;
        }
        try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(STORAGE_PATH.toFile()))) {
            return (TaskDB) inputStream.readObject();
        } catch (IOException | ClassNotFoundException exception) {
            return null;
        }
    }

    private void validateUniqueNameAndDueDate(Task task) {
        LocalDate dueDate = task.getDueDateAsLocalDate();
        if (dueDate == null) {
            return;
        }
        boolean duplicate = tasks.values().stream()
                .anyMatch(existing -> existing.getTitle().equalsIgnoreCase(task.getTitle())
                        && dueDate.equals(existing.getDueDateAsLocalDate()));
        if (duplicate) {
            throw new IllegalStateException("The combination of task name and due-date must be unique.");
        }
    }

    private void validateOpenTasksWithoutDueDate() {
        long count = tasks.values().stream().filter(Task::isOpenWithoutDueDate).count();
        if (count > 50) {
            throw new IllegalStateException("The number of open tasks without a due date should not exceed 50.");
        }
    }

    private void ensureNoCollaboratorOverloaded() {
        refreshCollaboratorLoads();
        collaborators.values().forEach(collaborator -> {
            if (collaborator.isOverloaded()) {
                throw new IllegalStateException("No collaborator must be overloaded: " + collaborator.getC_name());
            }
        });
    }

    private void refreshCollaboratorLoads() {
        collaborators.values().forEach(Collaborator::refreshOpenTaskCount);
    }

    private Collaborator findOrCreateCollaborator(Project project, String collaboratorName, String collaboratorCategory) {
        for (Collaborator collaborator : project.getCollaborators()) {
            if (collaborator.getC_name().equalsIgnoreCase(collaboratorName)) {
                return collaborator;
            }
        }
        return createCollaborator(collaboratorName, collaboratorCategory.isBlank() ? "Junior" : collaboratorCategory, project.getP_id());
    }

    private String toCsvLine(Task task, Subtask subtask) {
        String projectName = task.getProject() == null ? "" : task.getProject().getP_name();
        String projectDescription = task.getProject() == null ? "" : task.getProject().getP_description();
        String collaboratorName = "";
        String collaboratorCategory = "";

        if (subtask != null && subtask.getCollaborator() != null) {
            collaboratorName = subtask.getCollaborator().getC_name();
            collaboratorCategory = subtask.getCollaborator().getClass().getSimpleName().replace("Collaborator", "");
        }

        return String.join(",",
                escape(task.getTitle()),
                escape(task.getDescription()),
                escape(subtask == null ? "" : subtask.getTitle()),
                escape(task.getStatus()),
                String.valueOf(task.getPriority()),
                escape(String.valueOf(task.getDueDateAsLocalDate() == null ? "" : task.getDueDateAsLocalDate())),
                escape(projectName),
                escape(projectDescription),
                escape(collaboratorName),
                escape(collaboratorCategory));
    }

    private String escape(String value) {
        return value == null ? "" : value.replace(",", " ");
    }
}
