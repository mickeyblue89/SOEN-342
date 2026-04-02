import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class TaskService {	

	    private List<Task> tasks = new ArrayList<>();

	    public Task createTask(String title, int priority, String status) {
	        Task t = new Task(title, priority, status);
	        tasks.add(t);

	        System.out.println("Activity: Task created -> " + title);

	        return t;
	    }
	    
	    public List<Task> searchTasks(Map<String, Object> criteria) {

    return tasks.stream().filter(t -> {

                if (criteria.containsKey("title")) {
                    String name = (String) criteria.get("title");
                    if (!t.getTitle().toLowerCase().contains(name.toLowerCase())) {
                        return false;
                    }
                }

                if (criteria.containsKey("status")) {
                    String status = (String) criteria.get("status");
                    if (!t.getStatus().equalsIgnoreCase(status)) {
                        return false;
                    }
                }

                if (criteria.containsKey("from")) {
                    LocalDate from = (LocalDate) criteria.get("from");
                    if (t.getDueDate() == null || t.getDueDate().isBefore(from)) {
                        return false;
                    }
                }

                if (criteria.containsKey("to")) {
                    LocalDate to = (LocalDate) criteria.get("to");
                    if (t.getDueDate() == null || t.getDueDate().isAfter(to)) {
                        return false;
                    }
                }

                return true;
            })
            .sorted(Comparator.comparing(Task::getDueDate,
                    Comparator.nullsLast(Comparator.naturalOrder())))
            .collect(Collectors.toList());
}

public void updateTask(int id, String attribute, Object value) {

    Task task = tasks.stream()
            .filter(t -> t.getId() == id)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Task not found"));

    switch (attribute.toLowerCase()) {

        case "title":
            task.setTitle((String) value);
            break;

        case "description":
            task.setDescription((String) value);
            break;

        case "status":
            task.setStatus((String) value);
            break;

        case "priority":
            task.setPriority((Integer) value);
            break;

        case "duedate":
            task.setDueDate((LocalDate) value);
            break;

        default:
            throw new RuntimeException("Invalid attribute: " + attribute);
    }
}
	    public List<Task> getAllTasks() {
	        return tasks;
	    }

		public void importFromCsv(String filePath) throws IOException {
        List<Task> importedTasks = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                if (firstLine) {
                    firstLine = false;
                    if (line.toLowerCase().contains("title")) {
                        continue;
                    }
                }

				String[] parts = line.split(",");

                if (parts.length < 3) {
                    throw new IllegalArgumentException("Invalid CSV row: " + line);
                }

                String title = parts[0].trim();
                int priority = Integer.parseInt(parts[1].trim());
                String status = parts[2].trim();

                Task task = new Task(title, priority, status);

                if (parts.length >= 4 && !parts[3].trim().isEmpty()) {
                    LocalDate dueDate = LocalDate.parse(parts[3].trim());
                    task.setDueDate(dueDate);
                }

                importedTasks.add(task);
            }
        }

		tasks.clear();
        tasks.addAll(importedTasks);

        System.out.println("Activity: Imported " + importedTasks.size() + " tasks from " + filePath);
    }

    public void exportCsv(String filePath) throws IOException {

    try (java.io.BufferedWriter writer = new java.io.BufferedWriter(new java.io.FileWriter(filePath))) {

        // :white_check_mark: Header (matches your import structure)
        writer.write("title,priority,status,dueDate");
        writer.newLine();

        for (Task t : tasks) {

            StringBuilder line = new StringBuilder();

            // title
            line.append(t.getTitle()).append(",");

            // priority
            line.append(t.getPriority()).append(",");

            // status
            line.append(t.getStatus()).append(",");

            // due date (handle null)
            if (t.getDueDate() != null) {
                line.append(t.getDueDate());
            } else {
                line.append("");
            }

            writer.write(line.toString());
            writer.newLine();
        }

        System.out.println("Activity: Exported " + tasks.size() + " tasks to " + filePath);
    }
}
	
}
