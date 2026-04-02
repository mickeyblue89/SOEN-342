package app;

import java.sql.*;
import java.sql.Date;
import java.util.*;

public class TaskRepository {

    public void save(Task t) throws Exception {

        Connection conn = DBConnection.getConnection();

        String sql = "INSERT INTO task(title, description, creation_date, priority, status, due_date) VALUES (?, ?, ?, ?, ?, ?)";

        PreparedStatement stmt = conn.prepareStatement(sql);

        stmt.setString(1, t.getTitle());
        stmt.setString(2, t.getDescription());
        stmt.setDate(3, Date.valueOf(t.getCreationDate()));
        stmt.setInt(4, t.getPriority());
        stmt.setString(5, t.getStatus());
        stmt.setDate(6, t.getDueDate() != null ? Date.valueOf(t.getDueDate()) : null);

        stmt.executeUpdate();
        conn.close();
    }

    public List<Task> getAll() throws Exception {

        List<Task> list = new ArrayList<>();

        Connection conn = DBConnection.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM task");

        while (rs.next()) {

            Task t = new Task(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getInt("priority")
            );

            list.add(t);
        }

        conn.close();
        return list;
    }
}
