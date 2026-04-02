package app;

/*import java.util.*;
import java.util.stream.Collectors;
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
}
*/
