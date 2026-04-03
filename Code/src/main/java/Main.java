import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Main {
    private static final Scanner SCANNER = new Scanner(System.in);

    public static void main(String[] args) {
        TaskDB taskDB = new TaskDB();        
        iCalGateway iCalGateway = new iCalGateway();

        while (true) {
            printMenu();
            String choice = SCANNER.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> createTask(taskDB);
                    case "2" -> createProject(taskDB);
                    case "3" -> createCollaborator(taskDB);
                    case "4" -> assignTaskToProject(taskDB);
                    case "5" -> assignCollaboratorToTask(taskDB);
                    case "6" -> updateTask(taskDB);
                    case "7" -> viewTasks(taskDB);
                    case "8" -> searchTasks(taskDB);
                    case "9" -> exportCSV(taskDB);
                    case "10" -> importCSV(taskDB);
                    case "11" -> exportICal(taskDB, iCalGateway);
                    case "12" -> showActivity(taskDB);
                    case "13" -> showOverloaded(taskDB.getAllCollaborators());
                    case "14" -> listTasks(taskDB.getAllTasks());
                    case "0" -> {
                        taskDB.save();
                        return;
                    }
                    default -> System.out.println("Invalid option.");
                }
            } catch (Exception exception) {
                System.out.println("Error: " + exception.getMessage());
            }
        }
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("==== PERSONAL TASK MANAGEMENT SYSTEM ====");
        System.out.println("==== Made By: Adriana, Mike, Shayan ====");
        System.out.println("1. Create Task");
        System.out.println("2. Create Project");
        System.out.println("3. Create Collaborator");
        System.out.println("4. Add Task To Project");
        System.out.println("5. Link Collaborator To Task");
        System.out.println("6. Update Task");
        System.out.println("7. View Tasks");
        System.out.println("8. Search Tasks");
        System.out.println("9. Export CSV");
        System.out.println("10. Import CSV");
        System.out.println("11. Export iCalendar");
        System.out.println("12. View Activity History");
        System.out.println("13. Show Overloaded Collaborators");
        System.out.println("14. List All Tasks");
        System.out.println("0. Exit");
        System.out.print("Choice: ");
    }

    private static void createTask(TaskDB taskDB) {
        System.out.print("Title: ");
        String title = SCANNER.nextLine();
        System.out.print("Description or blank: ");
        String description = SCANNER.nextLine();
        System.out.print("Priority (1-5): ");
        int priority = Integer.parseInt(SCANNER.nextLine());
        System.out.print("Due date (yyyy-mm-dd or blank): ");
        String dueDateInput = SCANNER.nextLine().trim();
        LocalDate dueDate = dueDateInput.isBlank() ? null : LocalDate.parse(dueDateInput);
        Recurrence recurrence = null;
        System.out.print("Recurrence type (blank, DAILY, WEEKLY:MONDAY|WEDNESDAY, MONTHLY, CUSTOM:3): ");
        String recurrenceType = SCANNER.nextLine().trim();
        if (!recurrenceType.isBlank()) {
            System.out.print("Recurrence start date (yyyy-mm-dd): ");
            LocalDate startDate = LocalDate.parse(SCANNER.nextLine().trim());
            System.out.print("Recurrence end date (yyyy-mm-dd): ");
            LocalDate endDate = LocalDate.parse(SCANNER.nextLine().trim());
            System.out.print("Interval/count (positive integer): ");
            int count = Integer.parseInt(SCANNER.nextLine().trim());
            recurrence = new Recurrence(recurrenceType, startDate, endDate, count);
        }

        Task task = taskDB.addTask(Task.createTask(title, description, priority, dueDate, recurrence));

        System.out.print("Comma-separated tags (blank if none): ");
        String tags = SCANNER.nextLine().trim();
        if (!tags.isBlank()) {
            for (String rawTag : tags.split(",")) {
                Tag tag = taskDB.findOrCreateTag(rawTag.trim());
                task.organizeTask(tag);
            }
            taskDB.save();
        }

        System.out.println("Created task with id " + task.getId());
    }

    private static void createProject(TaskDB taskDB) {
        System.out.print("Project name: ");
        String name = SCANNER.nextLine();
        System.out.print("Project description: ");
        String description = SCANNER.nextLine();
        Project project = taskDB.addProject(Project.createProject(name, description));
        System.out.println("Created project with id " + project.getP_id());
    }

    private static void createCollaborator(TaskDB taskDB) {
        System.out.print("Project id: ");
        int projectId = Integer.parseInt(SCANNER.nextLine());
        System.out.print("Collaborator name: ");
        String name = SCANNER.nextLine();
        System.out.print("Category (Junior/Intermediate/Senior): ");
        String category = SCANNER.nextLine().trim();
        Collaborator collaborator = taskDB.createCollaborator(name, category, projectId);
        System.out.println("Created collaborator with id " + collaborator.getC_id());
    }

    private static void assignTaskToProject(TaskDB taskDB) {
        System.out.print("Task id: ");
        int taskId = Integer.parseInt(SCANNER.nextLine());
        System.out.print("Project id: ");
        int projectId = Integer.parseInt(SCANNER.nextLine());
        Project project = taskDB.getProject(projectId);
        Task task = taskDB.getTask(taskId);
        project.addTask(task);
        taskDB.save();
        System.out.println("Task assigned to project.");
    }

    private static void assignCollaboratorToTask(TaskDB taskDB) {
        System.out.print("Task id: ");
        int taskId = Integer.parseInt(SCANNER.nextLine());
        System.out.print("Collaborator id: ");
        int collaboratorId = Integer.parseInt(SCANNER.nextLine());
        taskDB.assignCollaboratorToTask(taskId, collaboratorId);
        System.out.println("Collaborator linked to task.");
    }

    private static void updateTask(TaskDB taskDB) {
        System.out.print("Task id: ");
        int taskId = Integer.parseInt(SCANNER.nextLine());
        System.out.print("Attribute (title, description, status, priority, dueDate): ");
        String attribute = SCANNER.nextLine();
        if ("status".equalsIgnoreCase(attribute)) {
            System.out.print("Value (open/completed): ");
        } else {
            System.out.print("Value: ");
        }
        String value = SCANNER.nextLine();
        Object parsedValue = "priority".equalsIgnoreCase(attribute) ? Integer.parseInt(value) : value;
        taskDB.updateTask(taskId, attribute, parsedValue);
        System.out.println("Task updated.");
    }

    private static void searchTasks(TaskDB taskDB) {
        listTasks(taskDB.searchTasks(collectSearchCriteria()));
    }

    private static void viewTasks(TaskDB taskDB) {
        System.out.println("View tasks by:");
        System.out.println("1. Due date");
        System.out.println("2. Priority");
        System.out.println("3. Status");
        System.out.println("4. Project");
        System.out.println("5. Tag");
        System.out.println("6. Specific date");
        System.out.println("7. Date range");
        System.out.print("Choice: ");

        String choice = SCANNER.nextLine().trim();
        Map<String, Object> criteria = new HashMap<>();
        String viewBy;

        switch (choice) {
            case "1" -> viewBy = "dueDate";
            case "2" -> {
                System.out.print("Priority (1-5 or blank for all): ");
                String priority = SCANNER.nextLine().trim();
                if (!priority.isBlank()) {
                    criteria.put("priority", Integer.parseInt(priority));
                }
                viewBy = "priority";
            }
            case "3" -> {
                System.out.print("Status: ");
                String status = SCANNER.nextLine().trim();
                if (!status.isBlank()) {
                    criteria.put("status", status);
                }
                viewBy = "status";
            }
            case "4" -> {
                System.out.print("Project name: ");
                String project = SCANNER.nextLine().trim();
                criteria.put("project", project);
                viewBy = "project";
            }
            case "5" -> {
                System.out.print("Tag keyword: ");
                String tag = SCANNER.nextLine().trim();
                criteria.put("tag", tag);
                viewBy = "tag";
            }
            case "6" -> {
                System.out.print("Date (yyyy-mm-dd): ");
                criteria.put("date", LocalDate.parse(SCANNER.nextLine().trim()));
                viewBy = "date";
            }
            case "7" -> {
                System.out.print("From date (yyyy-mm-dd): ");
                criteria.put("from", LocalDate.parse(SCANNER.nextLine().trim()));
                System.out.print("To date (yyyy-mm-dd): ");
                criteria.put("to", LocalDate.parse(SCANNER.nextLine().trim()));
                viewBy = "range";
            }
            default -> throw new IllegalArgumentException("Invalid view option.");
        }

        List<Task> tasks = Task.viewTasks(taskDB.getAllTasks(), viewBy, criteria);
        listTasks(tasks);
    }

    private static void exportCSV(TaskDB taskDB) throws IOException {
        System.out.print("CSV file path: ");
        String filePath = SCANNER.nextLine().trim();
        taskDB.exportCSV(filePath);
        System.out.println("CSV exported to " + filePath);
    }

    private static void importCSV(TaskDB taskDB) throws IOException {
        System.out.print("CSV file path: ");
        String filePath = SCANNER.nextLine().trim();
        taskDB.importCSV(filePath);
        System.out.println("CSV imported.");
    }

    private static void exportICal(TaskDB taskDB, iCalGateway iCalGateway) throws IOException {
        System.out.print("Export type (task/project/search): ");
        String type = SCANNER.nextLine().trim().toLowerCase();
        List<Task> tasks;

        switch (type) {
            case "task" -> {
                System.out.print("Task id: ");
                tasks = List.of(taskDB.getTask(Integer.parseInt(SCANNER.nextLine())));
            }
            case "project" -> {
                System.out.print("Project id: ");
                tasks = taskDB.getProject(Integer.parseInt(SCANNER.nextLine())).getTasks();
            }
            default -> {
                System.out.println("Filtered export criteria:");
                tasks = taskDB.searchTasks(collectSearchCriteria());
            }
        }

        Path file = iCalGateway.ExportTasks(tasks);
        System.out.println("iCalendar exported to " + file.toAbsolutePath());
    }

    private static Map<String, Object> collectSearchCriteria() {
        Map<String, Object> criteria = new HashMap<>();
        System.out.print("Task name contains (blank to skip): ");
        String taskName = SCANNER.nextLine().trim();
        if (!taskName.isBlank()) {
            criteria.put("taskName", taskName);
        }
        System.out.print("Status (blank to skip): ");
        String status = SCANNER.nextLine().trim();
        if (!status.isBlank()) {
            criteria.put("status", status);
        }
        System.out.print("From due date yyyy-mm-dd (blank to skip): ");
        String from = SCANNER.nextLine().trim();
        if (!from.isBlank()) {
            criteria.put("from", LocalDate.parse(from));
        }
        System.out.print("To due date yyyy-mm-dd (blank to skip): ");
        String to = SCANNER.nextLine().trim();
        if (!to.isBlank()) {
            criteria.put("to", LocalDate.parse(to));
        }
        System.out.print("Day of week (blank to skip): ");
        String dayOfWeek = SCANNER.nextLine().trim();
        if (!dayOfWeek.isBlank()) {
            criteria.put("dayOfWeek", dayOfWeek);
        }
        return criteria;
    }

    private static void showActivity(TaskDB taskDB) {
        System.out.print("Task id: ");
        int taskId = Integer.parseInt(SCANNER.nextLine());
        Task task = taskDB.getTask(taskId);
        for (ActivityEntry entry : task.getActivityEntries()) {
            System.out.println(entry.getTimestamp() + " | " + entry.getAe_description());
        }
    }

    private static void listTasks(List<Task> tasks) {
        if (tasks.isEmpty()) {
            System.out.println("No tasks found.");
            return;
        }
        for (Task task : tasks) {
            String project = task.getProject() == null ? "-" : task.getProject().getP_name();
            System.out.println(task.getId() + " | " + task.getTitle() + " | " + task.getStatus() + " | due=" + task.getDueDateAsLocalDate() + " | project=" + project);
        }
    }

    public static void showOverloaded(List<Collaborator> collaborators) {
        System.out.println("Overloaded Collaborators:");
        boolean any = false;
        for (Collaborator collaborator : collaborators) {
            if (collaborator.isOverloaded()) {
                any = true;
                System.out.println("- " + collaborator.getC_name() + " (" + collaborator.getNum_open_tasks() + "/" + collaborator.getMaxTasks() + ")");
            }
        }
        if (!any) {
            System.out.println("None");
        }
    }
}
