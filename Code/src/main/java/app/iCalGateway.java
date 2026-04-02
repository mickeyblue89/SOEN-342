package app;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.property.*;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.data.CalendarOutputter;

import java.io.FileOutputStream;
import java.time.ZoneId;
import java.util.List;

public class iCalGateway {
	
	 public static List<Subtask> getAllSubtasks(Task task) {
	        return task.getSubtasks();
	    }

    public void exportTasks(List<Task> tasks) throws Exception {

    	 Calendar calendar = new Calendar();
         calendar.getProperties().add(new ProdId("-//Task Manager//iCal4j Export//EN"));
         calendar.getProperties().add(Version.VERSION_2_0);

         for (Task t : tasks) {
             if (t.getDueDate() == null) continue;
             java.util.Date utilDate = java.util.Date.from(
                     t.getDueDate().atStartOfDay(ZoneId.systemDefault()).toInstant()
             );

             DateTime start = new DateTime(utilDate);
             VEvent event = new VEvent(start, t.getTitle());
             StringBuilder desc = new StringBuilder();
             desc.append("Description: ").append(t.getDescription()).append("\n");
             desc.append("Status: ").append(t.getStatus()).append("\n");
             desc.append("Priority: ").append(t.getPriority()).append("\n");

             if (t.getProject() != null) {
                 desc.append("Project: ").append(t.getProject().getName()).append("\n");
             }
             List<Subtask> subs = getAllSubtasks(t);

             if (!subs.isEmpty()) {
                 desc.append("Subtasks:\n");
                 for (Subtask s : subs) {
                     desc.append("- ")
                         .append(s.getTitle())
                         .append(" [")
                         .append(s.getStatus())
                         .append("]\n");
                 }
             }

             event.getProperties().add(new Description(desc.toString()));
             event.getProperties().add(new Priority(t.getPriority()));
             event.getProperties().add(new Uid(java.util.UUID.randomUUID().toString()));
             calendar.getComponents().add(event);
         }
         FileOutputStream fout = new FileOutputStream("tasks.ics");
         CalendarOutputter outputter = new CalendarOutputter();
         outputter.output(calendar, fout);
     }
    }
