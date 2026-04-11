import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.Priority;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;

public class iCalGateway {

    public Path ExportTasks(List<Task> tasks) throws IOException {
        Calendar calendar = new Calendar();
        calendar.getProperties().add(new ProdId("-//Personal Task Management System//iCal4j Export//EN"));
        calendar.getProperties().add(Version.VERSION_2_0);
        int exportedCount = 0;

        for (Task task : tasks) {
            LocalDate dueDate = task.getDueDateAsLocalDate();
            if (dueDate == null) {
                continue;
            }

            java.util.Date utilDate = java.util.Date.from(
                    dueDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

            DateTime start = new DateTime(utilDate);
            VEvent event = new VEvent(start, task.getTitle());
            event.getProperties().add(new Description(buildDescription(task)));
            event.getProperties().add(new Priority(task.getPriority()));
            event.getProperties().add(new Uid(UUID.randomUUID().toString()));
            calendar.getComponents().add(event);
            exportedCount++;
        }

        if (exportedCount == 0) {
            throw new IllegalStateException("Tasks without a due date are skipped during iCalendar export.");
        }

        Path file = Path.of("tasks.ics");
        try (FileOutputStream outputStream = new FileOutputStream(file.toFile())) {
            CalendarOutputter outputter = new CalendarOutputter();
            outputter.output(calendar, outputStream);
        } catch (Exception exception) {
            throw new IOException("Failed to export iCalendar file.", exception);
        }

        return file;
    }

    public Path exportTasks(List<Task> tasks) throws IOException {
        return ExportTasks(tasks);
    }

    public List<Subtask> getAllSubtasks(Task task) {
        return task.getSubtasks();
    }

    private String buildDescription(Task task) {
        StringBuilder builder = new StringBuilder();
        builder.append("Description: ").append(task.getDescription()).append('\n');
        builder.append("Due Date: ").append(task.getDueDateAsLocalDate()).append('\n');
        builder.append("Status: ").append(task.getStatus()).append('\n');
        builder.append("Priority: ").append(task.getPriority()).append('\n');
        if (task.getProject() != null) {
            builder.append("Project: ").append(task.getProject().getP_name()).append('\n');
        }
        if (!task.getSubtasks().isEmpty()) {
            builder.append("Subtasks:").append('\n');
            for (Subtask subtask : task.getSubtasks()) {
                builder.append("- ").append(subtask.getTitle()).append(" [").append(subtask.getStatus()).append("]").append('\n');
            }
        }
        return builder.toString().trim();
    }
}
