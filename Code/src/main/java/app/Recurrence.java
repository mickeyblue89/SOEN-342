package app;

import java.time.LocalDate;

public class Recurrence {

    private Task task;
    private String r_type;
    private LocalDate start_date;
    private LocalDate end_date;
    private int count;

    public Recurrence(Task task, String r_type, LocalDate start_date, LocalDate end_date, int count) {
        this.task = task;
        this.r_type = r_type;
        this.start_date = start_date;
        this.end_date = end_date;
        this.count = count;
    }

    public String getR_type() {
        return r_type;
    }

    public LocalDate getStart_date() {
        return start_date;
    }

    public LocalDate getEnd_date() {
        return end_date;
    }

    public int getCount() {
        return count;
    }

    public Task getTask() {
        return task;
    }

    public void setR_type(String r_type) {
        this.r_type = r_type;
    }

    public void setStart_date(LocalDate start_date) {
        this.start_date = start_date;
    }

    public void setEnd_date(LocalDate end_date) {
        this.end_date = end_date;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setTask(Task task) {
        this.task = task;
    }

}
