import java.util.ArrayList;

;public class Tag {

    private String keyword;
    private ArrayList<Task> tasks;

    public Tag(String keyword) {
        this.keyword = keyword;
        this.tasks = new ArrayList<>();
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
    
    public ArrayList<Task> getTasks() {
        return tasks;
    }
    
}
