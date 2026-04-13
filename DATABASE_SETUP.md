# Database Setup Guide

This guide explains how to set up the Task Manager database for this application.

## Files

- **`database.properties`** - Configuration file for database connection parameters
- **`setup_database.sql`** - SQL script to create the database and all required tables

## Step 1: Configure Database Connection

Edit `src/main/resources/database.properties` and set your database credentials:

```properties
jdbc.url=jdbc:mysql://YOUR_HOST:YOUR_PORT/task_manager
jdbc.user=YOUR_USERNAME
jdbc.password=YOUR_PASSWORD
```

Examples:
- **Local MySQL**: `jdbc:mysql://localhost:3306/task_manager`
- **Remote MySQL**: `jdbc:mysql://192.168.1.100:3306/task_manager`
- **Docker container**: `jdbc:mysql://mysql-container:3306/task_manager`

## Step 2: Create the Database

Execute the SQL script using one of these methods:

### Option A: Using MySQL CLI

```bash
mysql -u root -p < setup_database.sql
```

### Option B: Using MySQL Workbench

1. Open MySQL Workbench
2. Create a new query tab
3. Copy and paste the contents of `setup_database.sql`
4. Execute all statements (Ctrl+Shift+Enter)

### Option C: Using a Database Management Tool

1. Open your database management tool (phpMyAdmin, DBeaver, etc.)
2. Create a new SQL query
3. Copy and paste the contents of `setup_database.sql`
4. Execute the script

### Option D: Using Java Application

If MySQL is running, the application will automatically create connections:

1. Ensure MySQL is running
2. Run the Java application - it will attempt to connect to the database
3. If tables don't exist, you'll receive errors; create them using one of the methods above

## Database Schema

The setup script creates the following tables:

| Table | Purpose |
|-------|---------|
| `projects` | Stores project information |
| `collaborators` | Stores collaborator data |
| `tasks` | Main task storage |
| `subtasks` | Subtasks linked to main tasks |
| `tags` | Task classification tags |
| `task_tags` | Junction table linking tasks to tags |
| `activity_entries` | Tracks task activity and history |

## Verification

After setup, verify the database was created successfully:

```sql
USE task_manager;
SHOW TABLES;
```

You should see 7 tables listed.

## Troubleshooting

### Connection Error: "Access denied"
- Check username and password in `database.properties`
- Ensure MySQL user permissions are correct

### Connection Error: "Unknown database 'task_manager'"
- Run the `setup_database.sql` script first
- Verify MySQL is running

### SSL/TLS Connection Errors
Add SSL properties to `jdbc.url` if needed:
```
jdbc:mysql://localhost:3306/task_manager?useSSL=false&serverTimezone=UTC
```

### Port Already in Use
Change the port in `database.properties`:
```
jdbc:mysql://localhost:3307/task_manager
```
(Replace 3307 with your custom MySQL port)

## Resetting the Database

If you need to reset the database (WARNING: This deletes all data):

```sql
DROP DATABASE task_manager;
-- Then re-run setup_database.sql
```

## Next Steps

After database setup is complete:
1. Build the Java project: `mvn clean package`
2. Run the application: `mvn exec:java -Dexec.mainClass="Main"`
3. Start using the Task Manager System
