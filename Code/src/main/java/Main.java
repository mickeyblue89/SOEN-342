import java.util.*;

public class Main {

    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) throws Exception {

        TaskRepository repo = new TaskRepository();
        SearchService search = new SearchService();
        CSVService csv = new CSVService();
        iCalGateway ical = new iCalGateway();
        CollaboratorService colService = new CollaboratorService();

        List<Collaborator> collaborators = new ArrayList<>();

        while (true) {

            System.out.println("\n==== TASK MANAGER ====");
            System.out.println("1. Add Task");
            System.out.println("2. View Tasks");
            System.out.println("3. Search Task");
            System.out.println("4. Export CSV");
            System.out.println("5. Export iCal");
            System.out.println("6. Show Overloaded Collaborators");
            System.out.println("0. Exit");

            int choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {

                case 1:
                    System.out.print("Title: ");
                    String title = sc.nextLine();
                    
                    System.out.print("Description: ");
                    String description = sc.nextLine();

                    Task t = new Task(0, title, description, 0);
                    repo.save(t);
                    System.out.println("Task saved.");
                    break;

                case 2:
                    List<Task> all = repo.getAll();
                    for (Task task : all) {
                        System.out.println(task.getTitle() + " | " + task.getStatus());
                    }
                    break;

                case 3:
                    System.out.print("Search name: ");
                    String name = sc.nextLine();

                    List<Task> tasks = repo.getAll();
                    List<Task> results = search.search(tasks, name, null);

                    for (Task r : results) {
                        System.out.println(r.getTitle());
                    }
                    break;

                case 4:
                    csv.export(repo.getAll());
                    System.out.println("CSV exported.");
                    break;

                case 5:
                    ical.exportTasks(repo.getAll());
                    System.out.println("iCal exported.");
                    break;

                case 6:
                    colService.showOverloaded(collaborators);
                    break;

                case 0:
                    System.exit(0);
            }
        }
    }
}