package app;

import java.time.LocalDate;
import java.util.*;

public class SearchService {

    public List<Task> search(List<Task> tasks, String name, String status) {

        List<Task> result = new ArrayList<>();

        for (Task t : tasks) {

            if (name != null && !t.getTitle().contains(name)) continue;
            if (status != null && !t.getStatus().equalsIgnoreCase(status)) continue;

            result.add(t);
        }

        return result;
    }
}
