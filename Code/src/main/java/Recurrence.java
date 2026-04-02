import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Recurrence implements Serializable {
    private static final long serialVersionUID = 1L;

    private String r_type;
    private LocalDate start_Date;
    private LocalDate end_Date;
    private int count;

    public Recurrence(String r_type, LocalDate start_Date, LocalDate end_Date, int count) {
        this.r_type = r_type;
        this.start_Date = start_Date;
        this.end_Date = end_Date;
        this.count = count;
    }

    public List<Task> createOccurence(Task task, String r_type) {
        List<LocalDate> dates = generateDates(r_type);
        List<Task> occurrences = new ArrayList<>();
        for (LocalDate date : dates) {
            Task occurrence = new Task(0, task.getTitle(), task.getDescription(), task.getPriority());
            occurrence.setStatus(task.getStatus());
            occurrence.setDueDate(java.sql.Date.valueOf(date));
            occurrence.setRecurrence(this);
            occurrences.add(occurrence);
        }
        return occurrences;
    }

    public List<LocalDate> generateDates(String recurrenceType) {
        List<LocalDate> dates = new ArrayList<>();
        if (start_Date == null) {
            return dates;
        }

        String normalized = recurrenceType == null ? "" : recurrenceType.trim().toUpperCase();
        if (normalized.startsWith("WEEKLY")) {
            Set<DayOfWeek> weekdays = parseWeekdays(normalized);
            LocalDate cursor = start_Date;
            while (!isLimitReached(dates, cursor)) {
                if (weekdays.contains(cursor.getDayOfWeek())) {
                    dates.add(cursor);
                }
                cursor = cursor.plusDays(1);
            }
            return dates;
        }

        if (normalized.startsWith("MONTHLY")) {
            int months = Math.max(count, 1);
            int dayOfMonth = start_Date.getDayOfMonth();
            LocalDate cursor = start_Date;
            while (!isLimitReached(dates, cursor)) {
                dates.add(cursor);
                LocalDate nextMonth = cursor.plusMonths(months);
                cursor = nextMonth.withDayOfMonth(Math.min(dayOfMonth, nextMonth.lengthOfMonth()));
            }
            return dates;
        }

        if (normalized.startsWith("CUSTOM:")) {
            int interval = Integer.parseInt(normalized.substring("CUSTOM:".length()));
            LocalDate cursor = start_Date;
            while (!isLimitReached(dates, cursor)) {
                dates.add(cursor);
                cursor = cursor.plusDays(Math.max(interval, 1));
            }
            return dates;
        }

        LocalDate cursor = start_Date;
        int interval = Math.max(count, 1);
        while (!isLimitReached(dates, cursor)) {
            dates.add(cursor);
            cursor = cursor.plusDays(interval);
        }
        return dates;
    }

    private boolean isLimitReached(List<LocalDate> dates, LocalDate cursor) {
        return (end_Date != null && cursor.isAfter(end_Date)) || dates.size() >= 365;
    }

    private Set<DayOfWeek> parseWeekdays(String recurrenceType) {
        Set<DayOfWeek> weekdays = new LinkedHashSet<>();
        if (!recurrenceType.contains(":")) {
            weekdays.add(start_Date.getDayOfWeek());
            return weekdays;
        }
        String[] values = recurrenceType.substring(recurrenceType.indexOf(':') + 1).split("\\|");
        for (String value : values) {
            weekdays.add(DayOfWeek.valueOf(value.trim()));
        }
        return weekdays;
    }

    public String getR_type() {
        return r_type;
    }

    public LocalDate getStart_Date() {
        return start_Date;
    }

    public LocalDate getEnd_Date() {
        return end_Date;
    }

    public int getCount() {
        return count;
    }

    public void setR_type(String r_type) {
        this.r_type = r_type;
    }

    public void setStart_Date(LocalDate start_Date) {
        this.start_Date = start_Date;
    }

    public void setEnd_Date(LocalDate end_Date) {
        this.end_Date = end_Date;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
