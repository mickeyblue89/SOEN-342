package app;

import java.io.*;
import java.util.List;

public class CSVService {

    public void export(List<Task> tasks) throws Exception {

        PrintWriter writer = new PrintWriter("tasks.csv");

        writer.println("Title,Status,Priority");

        for (Task t : tasks) {
            writer.println(t.getTitle() + "," + t.getStatus() + "," + t.getPriority());
        }

        writer.close();
    }
}
