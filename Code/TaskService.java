import java.util.*;
import java.util.stream.Collectors;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;

public class TaskService {	

	    private List<Task> tasks = new ArrayList<>();

	    public Task createTask(String title, int priority, String status) {
	        Task t = new Task(title, priority, status);
	        tasks.add(t);

	        System.out.println("Activity: Task created -> " + title);

	        return t;
	    }
	    
	    public List<Task> searchTasks(String name, String status, LocalDate from, LocalDate to) {

	        return tasks.stream()
	                .filter(t -> name == null || t.getTitle().toLowerCase().contains(name.toLowerCase()))
	                .filter(t -> status == null || t.getStatus().equalsIgnoreCase(status))
	                .filter(t -> from == null || (t.getDueDate() != null && !t.getDueDate().isBefore(from)))
	                .filter(t -> to == null || (t.getDueDate() != null && !t.getDueDate().isAfter(to)))
	                .sorted(Comparator.comparing(Task::getDueDate,
	                        Comparator.nullsLast(Comparator.naturalOrder())))
	                .collect(Collectors.toList());
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
	
}
