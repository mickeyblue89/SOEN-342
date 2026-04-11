import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TaskDB {
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/task_manager";
    private static final String JDBC_USER = "root";
    private static final String JDBC_PASSWORD = "admin";
    private static TaskDB INSTANCE;

    public TaskDB() {
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
        try (Connection connection = openConnection()) {
            for (Task entry : storedTasks) {
                validateUniqueNameAndDueDate(connection, entry, null);
                validateOpenTasksWithoutDueDate(connection, entry);
                int taskId = nextId(connection, "tasks", "task_id");
                entry.setId(taskId);
                insertTask(connection, entry);
                insertActivityEntries(connection, taskId, entry.getActivityEntries());
                if (entry.getProject() != null) {
                    assignTaskToProject(connection, taskId, entry.getProject().getP_id(), false);
                }
                if (first == null) {
                    first = getTask(connection, taskId);
                }
            }
            return first;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to add task: " + exception.getMessage(), exception);
        }
    }

    public Task getTask(int t_id) {
        try (Connection connection = openConnection()) {
            return getTask(connection, t_id);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load task: " + exception.getMessage(), exception);
        }
    }

    public void updateTask(int t_id, String attribute, Object value) {
        Task task = getTask(t_id);
        task.updateTask(attribute, value);
        try (Connection connection = openConnection()) {
            validateUniqueNameAndDueDate(connection, task, t_id);
            validateOpenTasksWithoutDueDateForUpdate(connection, task);
            String normalized = attribute == null ? "" : attribute.trim().toLowerCase();
            String sql = switch (normalized) {
                case "title" -> "UPDATE tasks SET title = ? WHERE task_id = ?";
                case "description" -> "UPDATE tasks SET description = ? WHERE task_id = ?";
                case "status" -> "UPDATE tasks SET status = ? WHERE task_id = ?";
                case "priority", "prioritylevel" -> "UPDATE tasks SET priority = ? WHERE task_id = ?";
                case "duedate", "due date" -> "UPDATE tasks SET due_date = ? WHERE task_id = ?";
                default -> throw new IllegalArgumentException("Unsupported task attribute: " + attribute);
            };
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                switch (normalized) {
                    case "title" -> statement.setString(1, task.getTitle());
                    case "description" -> statement.setString(1, task.getDescription());
                    case "status" -> statement.setString(1, task.getStatus());
                    case "priority", "prioritylevel" -> statement.setInt(1, task.getPriority());
                    case "duedate", "due date" -> {
                        if (task.getDueDateAsLocalDate() == null) {
                            statement.setNull(1, Types.DATE);
                        } else {
                            statement.setDate(1, Date.valueOf(task.getDueDateAsLocalDate()));
                        }
                    }
                    default -> {
                    }
                }
                statement.setInt(2, t_id);
                statement.executeUpdate();
            }
            clearActivityEntries(connection, t_id);
            insertActivityEntries(connection, t_id, task.getActivityEntries());
            ensureNoCollaboratorOverloaded(connection);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to update task: " + exception.getMessage(), exception);
        }
    }

    public Project addProject(Project project) {
        Objects.requireNonNull(project, "Project is required");
        try (Connection connection = openConnection()) {
            if (projectNameExists(connection, project.getP_name(), null)) {
                throw new IllegalArgumentException("Project name must be unique.");
            }
            int projectId = nextId(connection, "projects", "project_id");
            project.setP_id(projectId);
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO projects (project_id, name, description) VALUES (?, ?, ?)")) {
                statement.setInt(1, projectId);
                statement.setString(2, project.getP_name());
                statement.setString(3, project.getP_description());
                statement.executeUpdate();
            }
            return getProject(connection, projectId);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to add project: " + exception.getMessage(), exception);
        }
    }

    public Project getProject(int p_id) {
        try (Connection connection = openConnection()) {
            return getProject(connection, p_id);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load project: " + exception.getMessage(), exception);
        }
    }

    public void assignTaskToProject(int taskId, int projectId) {
        try (Connection connection = openConnection()) {
            assignTaskToProject(connection, taskId, projectId, true);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to assign task to project: " + exception.getMessage(), exception);
        }
    }

    public Tag findOrCreateTag(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("Tag keyword is required.");
        }
        try (Connection connection = openConnection()) {
            return findOrCreateTag(connection, keyword);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load tag: " + exception.getMessage(), exception);
        }
    }

    public void addTagToTask(int taskId, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("Tag keyword is required.");
        }
        try (Connection connection = openConnection()) {
            getTask(connection, taskId);
            Tag tag = findOrCreateTag(connection, keyword);
            int tagId = getTagId(connection, tag.getKeyword());
            try (PreparedStatement exists = connection.prepareStatement("SELECT 1 FROM task_tags WHERE task_id = ? AND tag_id = ?")) {
                exists.setInt(1, taskId);
                exists.setInt(2, tagId);
                try (ResultSet resultSet = exists.executeQuery()) {
                    if (!resultSet.next()) {
                        try (PreparedStatement insert = connection.prepareStatement("INSERT INTO task_tags (task_id, tag_id) VALUES (?, ?)")) {
                            insert.setInt(1, taskId);
                            insert.setInt(2, tagId);
                            insert.executeUpdate();
                        }
                    }
                }
            }
            insertActivityEntry(connection, taskId, "Tag added: " + tag.getKeyword());
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to add tag to task: " + exception.getMessage(), exception);
        }
    }

    public Collaborator createCollaborator(String name, String category, int projectId) {
        try (Connection connection = openConnection()) {
            getProject(connection, projectId);
            int collaboratorId = nextId(connection, "collaborators", "collaborator_id");
            Collaborator collaborator = createCollaboratorInstance(collaboratorId, name, category);
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO collaborators (collaborator_id, name, category, max_tasks, project_id) VALUES (?, ?, ?, ?, ?)")) {
                statement.setInt(1, collaboratorId);
                statement.setString(2, collaborator.getC_name());
                statement.setString(3, normalizeCategory(category));
                statement.setInt(4, collaborator.getMaxTasks());
                statement.setInt(5, projectId);
                statement.executeUpdate();
            }
            return getCollaborator(connection, collaboratorId);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to create collaborator: " + exception.getMessage(), exception);
        }
    }

    public void assignCollaboratorToTask(int taskId, int collaboratorId) {
        try (Connection connection = openConnection()) {
            Task task = getTask(connection, taskId);
            Collaborator collaborator = getCollaborator(connection, collaboratorId);
            if (task.getProject() == null) {
                throw new IllegalStateException("Collaborators can only be linked to project tasks.");
            }
            if (!canCollaboratorAcceptTask(connection, collaboratorId, collaborator.getMaxTasks())) {
                throw new IllegalStateException("Collaborator is overloaded: " + collaborator.getC_name());
            }
            if (task.getSubtasks().size() >= 20) {
                throw new IllegalStateException("A task cannot have more than 20 sub-tasks.");
            }
            int subtaskId = nextId(connection, "subtasks", "subtask_id");
            String subtaskTitle = "Assigned to " + collaborator.getC_name();
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO subtasks (subtask_id, parent_task_id, title, description, status, collaborator_id) VALUES (?, ?, ?, ?, ?, ?)")) {
                statement.setInt(1, subtaskId);
                statement.setInt(2, taskId);
                statement.setString(3, subtaskTitle);
                statement.setString(4, "Collaboration subtask for task " + task.getTitle());
                statement.setString(5, "open");
                statement.setInt(6, collaboratorId);
                statement.executeUpdate();
            }
            insertActivityEntry(connection, taskId, "Subtask added: " + subtaskTitle);
            insertActivityEntry(connection, taskId, "Collaborator linked: " + collaborator.getC_name());
            ensureNoCollaboratorOverloaded(connection);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to assign collaborator to task: " + exception.getMessage(), exception);
        }
    }

    public List<Task> searchTasks(Map<String, Object> criteria) {
        StringBuilder sql = new StringBuilder("SELECT DISTINCT t.task_id, t.title, t.description, t.creation_date, t.priority, t.status, t.due_date, t.project_id FROM tasks t LEFT JOIN projects p ON p.project_id = t.project_id LEFT JOIN task_tags tt ON tt.task_id = t.task_id LEFT JOIN tags tg ON tg.tag_id = tt.tag_id WHERE 1 = 1");
        List<Object> parameters = new ArrayList<>();
        Map<String, Object> searchCriteria = criteria == null ? Map.of() : criteria;
        Object titleMatch = searchCriteria.get("taskName");
        if (titleMatch == null) {
            titleMatch = searchCriteria.get("title");
        }
        if (titleMatch != null) {
            sql.append(" AND (LOWER(t.title) LIKE ? OR LOWER(t.description) LIKE ?)");
            String value = "%" + String.valueOf(titleMatch).toLowerCase() + "%";
            parameters.add(value);
            parameters.add(value);
        }
        if (searchCriteria.get("status") != null) {
            sql.append(" AND LOWER(t.status) = ?");
            parameters.add(String.valueOf(searchCriteria.get("status")).toLowerCase());
        }
        if (searchCriteria.get("priority") != null) {
            sql.append(" AND t.priority = ?");
            parameters.add(Integer.parseInt(String.valueOf(searchCriteria.get("priority"))));
        }
        if (searchCriteria.get("project") != null) {
            sql.append(" AND LOWER(COALESCE(p.name, '')) = ?");
            parameters.add(String.valueOf(searchCriteria.get("project")).toLowerCase());
        }
        if (searchCriteria.get("tag") != null) {
            sql.append(" AND LOWER(tg.keyword) = ?");
            parameters.add(String.valueOf(searchCriteria.get("tag")).toLowerCase());
        }
        if (searchCriteria.get("from") != null) {
            sql.append(" AND t.due_date >= ?");
            parameters.add(Date.valueOf((LocalDate) searchCriteria.get("from")));
        }
        if (searchCriteria.get("to") != null) {
            sql.append(" AND t.due_date <= ?");
            parameters.add(Date.valueOf((LocalDate) searchCriteria.get("to")));
        }
        if (searchCriteria.get("date") != null) {
            sql.append(" AND t.due_date = ?");
            parameters.add(Date.valueOf((LocalDate) searchCriteria.get("date")));
        }
        if (searchCriteria.get("dayOfWeek") != null) {
            sql.append(" AND DAYNAME(t.due_date) = ?");
            parameters.add(toSqlDayName(String.valueOf(searchCriteria.get("dayOfWeek"))));
        }
        if (searchCriteria.isEmpty()) {
            sql.append(" AND LOWER(t.status) = 'open'");
        }
        sql.append(" ORDER BY t.due_date IS NULL, t.due_date, t.task_id");

        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bindParameters(statement, parameters);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Task> tasks = new ArrayList<>();
                while (resultSet.next()) {
                    tasks.add(mapTask(connection, resultSet, null));
                }
                return tasks;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to search tasks: " + exception.getMessage(), exception);
        }
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
                task.setDueDate(Date.valueOf(dueDate));
            }
            if (!projectName.isBlank()) {
                Project project = findProjectByName(projectName);
                if (project == null) {
                    project = addProject(Project.createProject(projectName, projectDescription));
                }
                task.setProject(project);
            }

            Task storedTask = addTask(task);

            if (!subtaskName.isBlank()) {
                try (Connection connection = openConnection()) {
                    int subtaskId = nextId(connection, "subtasks", "subtask_id");
                    try (PreparedStatement statement = connection.prepareStatement("INSERT INTO subtasks (subtask_id, parent_task_id, title, description, status, collaborator_id) VALUES (?, ?, ?, ?, ?, ?)")) {
                        statement.setInt(1, subtaskId);
                        statement.setInt(2, storedTask.getId());
                        statement.setString(3, subtaskName);
                        statement.setString(4, "");
                        statement.setString(5, "open");
                        statement.setNull(6, Types.INTEGER);
                        statement.executeUpdate();
                    }
                    insertActivityEntry(connection, storedTask.getId(), "Subtask added: " + subtaskName);
                } catch (SQLException exception) {
                    throw new IllegalStateException("Failed to import subtask: " + exception.getMessage(), exception);
                }
            }

            if (!collaboratorName.isBlank()) {
                if (storedTask.getProject() == null) {
                    throw new IllegalArgumentException("Collaborators can only be imported for project tasks.");
                }
                Collaborator collaborator = findOrCreateCollaborator(storedTask.getProject(), collaboratorName, collaboratorCategory);
                assignCollaboratorToTask(storedTask.getId(), collaborator.getC_id());
            }
        }
    }

    public void exportCSV(String fileName) throws IOException {
        exportTasksToCSV(getAllTasks(), fileName);
    }

    public void exportTasksToCSV(List<Task> tasks, String fileName) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("TaskName,Description,Subtask,Status,Priority,DueDate,ProjectName,ProjectDescription,Collaborator,CollaboratorCategory");
        for (Task task : tasks) {
            if (task.getSubtasks().isEmpty()) {
                lines.add(toCsvLine(task, null));
            } else {
                for (Subtask subtask : task.getSubtasks()) {
                    lines.add(toCsvLine(task, subtask));
                }
            }
        }
        Files.write(Path.of(fileName), lines);
    }

    public List<Collaborator> getAllCollaborators() {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT collaborator_id FROM collaborators ORDER BY collaborator_id");
             ResultSet resultSet = statement.executeQuery()) {
            List<Collaborator> collaborators = new ArrayList<>();
            while (resultSet.next()) {
                collaborators.add(getCollaborator(connection, resultSet.getInt("collaborator_id")));
            }
            return collaborators;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load collaborators: " + exception.getMessage(), exception);
        }
    }

    public List<Task> getAllTasks() {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT task_id, title, description, creation_date, priority, status, due_date, project_id FROM tasks ORDER BY task_id");
             ResultSet resultSet = statement.executeQuery()) {
            List<Task> tasks = new ArrayList<>();
            while (resultSet.next()) {
                tasks.add(mapTask(connection, resultSet, null));
            }
            return tasks;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load tasks: " + exception.getMessage(), exception);
        }
    }

    public void save() {
        // Writes are performed directly in each SQL-backed method.
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
    }

    private Task getTask(Connection connection, int taskId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT task_id, title, description, creation_date, priority, status, due_date, project_id FROM tasks WHERE task_id = ?")) {
            statement.setInt(1, taskId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException("Task not found: " + taskId);
                }
                return mapTask(connection, resultSet, null);
            }
        }
    }

    private Project getProject(Connection connection, int projectId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT project_id, name, description FROM projects WHERE project_id = ?")) {
            statement.setInt(1, projectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException("Project not found: " + projectId);
                }
                Project project = new Project(resultSet.getInt("project_id"), resultSet.getString("name"), resultSet.getString("description"));
                loadProjectCollaborators(connection, project);
                loadProjectTasks(connection, project);
                return project;
            }
        }
    }

    private Project findProjectByName(String projectName) {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT project_id FROM projects WHERE LOWER(name) = ?")) {
            statement.setString(1, projectName.toLowerCase());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return getProject(connection, resultSet.getInt("project_id"));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to find project: " + exception.getMessage(), exception);
        }
    }

    private Collaborator getCollaborator(Connection connection, int collaboratorId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT collaborator_id, name, category, max_tasks, project_id FROM collaborators WHERE collaborator_id = ?")) {
            statement.setInt(1, collaboratorId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException("Collaborator not found: " + collaboratorId);
                }
                Collaborator collaborator = createCollaboratorInstance(resultSet.getInt("collaborator_id"), resultSet.getString("name"), resultSet.getString("category"));
                collaborator.maxTasks = resultSet.getInt("max_tasks");
                collaborator.addToProject(resultSet.getInt("project_id"));
                loadCollaboratorSubtasks(connection, collaborator);
                collaborator.refreshOpenTaskCount();
                return collaborator;
            }
        }
    }

    private Task mapTask(Connection connection, ResultSet resultSet, Project knownProject) throws SQLException {
        Task task = new Task(resultSet.getInt("task_id"), resultSet.getString("title"), resultSet.getString("description"), resultSet.getInt("priority"));
        task.setCreationDate(toUtilDate(resultSet.getTimestamp("creation_date")));
        task.setStatus(resultSet.getString("status"));
        task.setDueDate(resultSet.getDate("due_date"));
        task.getActivityEntries().clear();
        int projectId = resultSet.getInt("project_id");
        if (!resultSet.wasNull()) {
            task.setProject(knownProject != null && knownProject.getP_id() == projectId ? knownProject : loadProjectStub(connection, projectId));
        }
        loadSubtasks(connection, task);
        loadTaskTags(connection, task);
        loadActivityEntries(connection, task);
        return task;
    }

    private Project loadProjectStub(Connection connection, int projectId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT project_id, name, description FROM projects WHERE project_id = ?")) {
            statement.setInt(1, projectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return new Project(resultSet.getInt("project_id"), resultSet.getString("name"), resultSet.getString("description"));
            }
        }
    }

    private void loadProjectTasks(Connection connection, Project project) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT task_id, title, description, creation_date, priority, status, due_date, project_id FROM tasks WHERE project_id = ? ORDER BY task_id")) {
            statement.setInt(1, project.getP_id());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    project.getTasks().add(mapTask(connection, resultSet, project));
                }
            }
        }
    }

    private void loadProjectCollaborators(Connection connection, Project project) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT collaborator_id FROM collaborators WHERE project_id = ? ORDER BY collaborator_id")) {
            statement.setInt(1, project.getP_id());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    project.getCollaborators().add(getCollaborator(connection, resultSet.getInt("collaborator_id")));
                }
            }
        }
    }

    private void loadSubtasks(Connection connection, Task task) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT subtask_id, title, description, status, collaborator_id FROM subtasks WHERE parent_task_id = ? ORDER BY subtask_id")) {
            statement.setInt(1, task.getId());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Subtask subtask = new Subtask(resultSet.getInt("subtask_id"), resultSet.getString("title"), resultSet.getString("description"), 0);
                    if ("completed".equalsIgnoreCase(resultSet.getString("status"))) {
                        subtask.markCompleted();
                    } else {
                        subtask.reopen();
                    }
                    subtask.setParentTask(task);
                    subtask.getActivityEntries().clear();
                    int collaboratorId = resultSet.getInt("collaborator_id");
                    if (!resultSet.wasNull()) {
                        subtask.addCollaborator(collaboratorId);
                        subtask.linkCollaborator(getCollaborator(connection, collaboratorId));
                    }
                    task.getSubtasks().add(subtask);
                }
            }
        }
    }

    private void loadTaskTags(Connection connection, Task task) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT tg.keyword FROM task_tags tt JOIN tags tg ON tg.tag_id = tt.tag_id WHERE tt.task_id = ? ORDER BY tg.keyword")) {
            statement.setInt(1, task.getId());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Tag tag = new Tag(resultSet.getString("keyword"));
                    tag.addTask(task);
                    task.getTags().add(tag);
                }
            }
        }
    }

    private void loadActivityEntries(Connection connection, Task task) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT activity_id, activity_time, description FROM activity_entries WHERE task_id = ? ORDER BY activity_id")) {
            statement.setInt(1, task.getId());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    task.getActivityEntries().add(new ActivityEntry(resultSet.getInt("activity_id"), toUtilDate(resultSet.getTimestamp("activity_time")), resultSet.getString("description")));
                }
            }
        }
    }

    private void loadCollaboratorSubtasks(Connection connection, Collaborator collaborator) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT subtask_id, title, description, status FROM subtasks WHERE collaborator_id = ? ORDER BY subtask_id")) {
            statement.setInt(1, collaborator.getC_id());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Subtask subtask = new Subtask(resultSet.getInt("subtask_id"), resultSet.getString("title"), resultSet.getString("description"), 0);
                    if ("completed".equalsIgnoreCase(resultSet.getString("status"))) {
                        subtask.markCompleted();
                    } else {
                        subtask.reopen();
                    }
                    subtask.getActivityEntries().clear();
                    subtask.addCollaborator(collaborator.getC_id());
                    subtask.linkCollaborator(collaborator);
                    collaborator.getC_tasks().add(subtask);
                }
            }
        }
    }

    private void insertTask(Connection connection, Task task) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO tasks (task_id, title, description, creation_date, priority, status, due_date, project_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            statement.setInt(1, task.getId());
            statement.setString(2, task.getTitle());
            statement.setString(3, task.getDescription());
            statement.setTimestamp(4, new Timestamp(task.getCreationDate().getTime()));
            statement.setInt(5, task.getPriority());
            statement.setString(6, task.getStatus());
            if (task.getDueDateAsLocalDate() == null) {
                statement.setNull(7, Types.DATE);
            } else {
                statement.setDate(7, Date.valueOf(task.getDueDateAsLocalDate()));
            }
            if (task.getProject() == null) {
                statement.setNull(8, Types.INTEGER);
            } else {
                statement.setInt(8, task.getProject().getP_id());
            }
            statement.executeUpdate();
        }
    }

    private void assignTaskToProject(Connection connection, int taskId, int projectId, boolean addActivity) throws SQLException {
        getTask(connection, taskId);
        Project project = getProject(connection, projectId);
        try (PreparedStatement statement = connection.prepareStatement("UPDATE tasks SET project_id = ? WHERE task_id = ?")) {
            statement.setInt(1, projectId);
            statement.setInt(2, taskId);
            statement.executeUpdate();
        }
        if (addActivity) {
            insertActivityEntry(connection, taskId, "Assigned to project " + project.getP_name());
        }
    }

    private Tag findOrCreateTag(Connection connection, String keyword) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT keyword FROM tags WHERE LOWER(keyword) = ?")) {
            statement.setString(1, keyword.toLowerCase());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new Tag(resultSet.getString("keyword"));
                }
            }
        }
        int tagId = nextId(connection, "tags", "tag_id");
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO tags (tag_id, keyword) VALUES (?, ?)")) {
            statement.setInt(1, tagId);
            statement.setString(2, keyword);
            statement.executeUpdate();
        }
        return new Tag(keyword);
    }

    private int getTagId(Connection connection, String keyword) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT tag_id FROM tags WHERE LOWER(keyword) = ?")) {
            statement.setString(1, keyword.toLowerCase());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException("Tag not found: " + keyword);
                }
                return resultSet.getInt("tag_id");
            }
        }
    }

    private void clearActivityEntries(Connection connection, int taskId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM activity_entries WHERE task_id = ?")) {
            statement.setInt(1, taskId);
            statement.executeUpdate();
        }
    }

    private void insertActivityEntries(Connection connection, int taskId, List<ActivityEntry> entries) throws SQLException {
        for (ActivityEntry entry : entries) {
            insertActivityEntry(connection, taskId, entry.getTimestamp(), entry.getAe_description());
        }
    }

    private void insertActivityEntry(Connection connection, int taskId, String description) throws SQLException {
        insertActivityEntry(connection, taskId, new java.util.Date(), description);
    }

    private void insertActivityEntry(Connection connection, int taskId, java.util.Date timestamp, String description) throws SQLException {
        int activityId = nextId(connection, "activity_entries", "activity_id");
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO activity_entries (activity_id, task_id, activity_time, description) VALUES (?, ?, ?, ?)")) {
            statement.setInt(1, activityId);
            statement.setInt(2, taskId);
            statement.setTimestamp(3, new Timestamp(timestamp.getTime()));
            statement.setString(4, description);
            statement.executeUpdate();
        }
    }

    private int nextId(Connection connection, String tableName, String columnName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COALESCE(MAX(" + columnName + "), 0) + 1 FROM " + tableName);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private boolean projectNameExists(Connection connection, String projectName, Integer ignoreProjectId) throws SQLException {
        String sql = "SELECT 1 FROM projects WHERE LOWER(name) = ?";
        if (ignoreProjectId != null) {
            sql += " AND project_id <> ?";
        }
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, projectName.toLowerCase());
            if (ignoreProjectId != null) {
                statement.setInt(2, ignoreProjectId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void validateUniqueNameAndDueDate(Connection connection, Task task, Integer ignoreTaskId) throws SQLException {
        LocalDate dueDate = task.getDueDateAsLocalDate();
        if (dueDate == null) {
            return;
        }
        String sql = "SELECT 1 FROM tasks WHERE LOWER(title) = ? AND due_date = ?";
        if (ignoreTaskId != null) {
            sql += " AND task_id <> ?";
        }
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, task.getTitle().toLowerCase());
            statement.setDate(2, Date.valueOf(dueDate));
            if (ignoreTaskId != null) {
                statement.setInt(3, ignoreTaskId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    throw new IllegalStateException("The combination of task name and due-date must be unique.");
                }
            }
        }
    }

    private void validateOpenTasksWithoutDueDate(Connection connection, Task newTask) throws SQLException {
        if (!newTask.isOpenWithoutDueDate()) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM tasks WHERE LOWER(status) = 'open' AND due_date IS NULL");
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            if (resultSet.getInt(1) >= 50) {
                throw new IllegalStateException("The number of open tasks without a due date should not exceed 50.");
            }
        }
    }

    private void validateOpenTasksWithoutDueDateForUpdate(Connection connection, Task task) throws SQLException {
        if (!task.isOpenWithoutDueDate()) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM tasks WHERE LOWER(status) = 'open' AND due_date IS NULL AND task_id <> ?")) {
            statement.setInt(1, task.getId());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                if (resultSet.getInt(1) >= 50) {
                    throw new IllegalStateException("The number of open tasks without a due date should not exceed 50.");
                }
            }
        }
    }

    private boolean canCollaboratorAcceptTask(Connection connection, int collaboratorId, int maxTasks) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM subtasks WHERE collaborator_id = ? AND LOWER(status) <> 'completed'")) {
            statement.setInt(1, collaboratorId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) < maxTasks;
            }
        }
    }

    private void ensureNoCollaboratorOverloaded(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT c.name FROM collaborators c LEFT JOIN subtasks s ON s.collaborator_id = c.collaborator_id AND LOWER(s.status) <> 'completed' GROUP BY c.collaborator_id, c.name, c.max_tasks HAVING COUNT(s.subtask_id) > c.max_tasks");
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                throw new IllegalStateException("No collaborator must be overloaded: " + resultSet.getString("name"));
            }
        }
    }

    private Collaborator findOrCreateCollaborator(Project project, String collaboratorName, String collaboratorCategory) {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT collaborator_id FROM collaborators WHERE project_id = ? AND LOWER(name) = ?")) {
            statement.setInt(1, project.getP_id());
            statement.setString(2, collaboratorName.toLowerCase());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return getCollaborator(connection, resultSet.getInt("collaborator_id"));
                }
            }
            return createCollaborator(collaboratorName, collaboratorCategory.isBlank() ? "Junior" : collaboratorCategory, project.getP_id());
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load collaborator: " + exception.getMessage(), exception);
        }
    }

    private Collaborator createCollaboratorInstance(int id, String name, String category) {
        return switch (normalizeCategory(category).toLowerCase()) {
            case "junior" -> new JuniorCollaborator(id, name);
            case "intermediate" -> new IntermediateCollaborator(id, name);
            case "senior" -> new SeniorCollaborator(id, name);
            default -> throw new IllegalArgumentException("Unknown collaborator category: " + category);
        };
    }

    private String normalizeCategory(String category) {
        String normalized = category == null ? "" : category.trim().toLowerCase();
        return switch (normalized) {
            case "junior" -> "Junior";
            case "intermediate" -> "Intermediate";
            case "senior" -> "Senior";
            default -> throw new IllegalArgumentException("Unknown collaborator category: " + category);
        };
    }

    private void bindParameters(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            Object value = parameters.get(i);
            int index = i + 1;
            if (value instanceof Integer integer) {
                statement.setInt(index, integer);
            } else if (value instanceof Date date) {
                statement.setDate(index, date);
            } else {
                statement.setString(index, String.valueOf(value));
            }
        }
    }

    private String toSqlDayName(String dayOfWeek) {
        String normalized = dayOfWeek == null ? "" : dayOfWeek.trim().toLowerCase();
        return switch (normalized) {
            case "monday" -> "Monday";
            case "tuesday" -> "Tuesday";
            case "wednesday" -> "Wednesday";
            case "thursday" -> "Thursday";
            case "friday" -> "Friday";
            case "saturday" -> "Saturday";
            case "sunday" -> "Sunday";
            default -> throw new IllegalArgumentException("Invalid day of week: " + dayOfWeek);
        };
    }

    private java.util.Date toUtilDate(Timestamp timestamp) {
        return timestamp == null ? null : new java.util.Date(timestamp.getTime());
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
