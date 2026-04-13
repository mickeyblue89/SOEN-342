# SOEN-342
Team Members:
Mike Kouka - 40299971 (Team Leader)
Shayan Javanmardi - 40299147
Adriana Atijas - 40317966

## Personal Task Management System

This program is a console-based personal task management system written in Java. It lets you create tasks, group tasks into projects, assign collaborators, search and view tasks, and export data to CSV and iCalendar formats.

## Requirements

- Java 21
- An IDE such as Eclipse/IntelliJ, or a Java build setup that can compile and run the project
- Maven is used by the project through [`Code/pom.xml`]

## How To Run

The main entry point is [`Main.java`](Code/src/main/java/Main.java).

If you are using an IDE:

1. Open the `Code` folder as a Java/Maven project.
2. Build the project.
3. Run `Main.java`.

If you are using the command line, compile and run the project with your Java/Maven setup, making sure Java 21 is used.

## Data Storage

- For users with different database setup:

Open src/main/resources/database.properties
Update these values to match their SQL setup:

jdbc.url=jdbc:mysql://YOUR_HOST:YOUR_PORT/YOURDB_NAME
jdbc.user=YOUR_USERNAME
jdbc.password=YOUR_PASSWORD

## Main Menu

When the program starts, it shows these options:

1. Create Task
2. Create Project
3. Create Collaborator
4. Add Task To Project
5. Link Collaborator To Task
6. Update Task
7. View Tasks
8. Search Tasks
9. Export CSV
10. Import CSV
11. Export iCalendar
12. View Activity History
13. Show Overloaded Collaborators
14. List All Tasks
0. Exit

## Menu Instructions

### 1. Create Task

Use this option to create a new task.

You will be asked for:

- `Title`
- `Description or blank`
- `Priority (1-5)`
- `Due date (yyyy-mm-dd or blank)`
- `Recurrence type (blank, DAILY, WEEKLY:MONDAY|WEDNESDAY, MONTHLY, CUSTOM:3)`
- If recurrence is not blank:
  `Recurrence start date (yyyy-mm-dd)`
  `Recurrence end date (yyyy-mm-dd)`
  `Interval/count (positive integer)`
- `Comma-separated tags (blank if none)`

Notes:

- If recurrence is left blank, one task is created.
- If recurrence is provided, the program generates multiple task occurrences.
- Tags are optional and should be separated with commas.
- A task name and due date combination must be unique.
- The system does not allow more than 50 open tasks without a due date.

### 2. Create Project

Use this option to create a new project.

You will be asked for:

- `Project name`
- `Project description`

Notes:

- Project names must be unique.

### 3. Create Collaborator

Use this option to create a collaborator and attach them to an existing project.

You will be asked for:

- `Project id`
- `Collaborator name`
- `Category (Junior/Intermediate/Senior)`

Notes:

- The project must already exist.
- The collaborator category must be exactly `Junior`, `Intermediate`, or `Senior`.

### 4. Add Task To Project

Use this option to attach an existing task to an existing project.

You will be asked for:

- `Task id`
- `Project id`

Result:

- The task is assigned to the project.

### 5. Link Collaborator To Task

Use this option to link a collaborator to a task.

You will be asked for:

- `Task id`
- `Collaborator id`

Important behavior:

- Collaborators can only be linked to tasks that already belong to a project.
- Linking a collaborator creates a collaboration subtask automatically.
- A collaborator cannot be assigned if they would become overloaded.

### 6. Update Task

Use this option to change one field of an existing task.

You will be asked for:

- `Task id`
- `Attribute (title, description, status, priority, dueDate)`
- `Value`

If the attribute is `status`, valid values are:

- `open`
- `completed`

Notes:

- For `priority`, enter a number.
- For `dueDate`, use `yyyy-mm-dd`.

### 7. View Tasks

Use this option to display tasks using one of the built-in view cases.

Submenu cases:

1. `Due date`
   Shows tasks ordered by due date.
2. `Priority`
   You will be asked for `Priority (1-5 or blank for all)`.
   Tasks are sorted by priority, highest first.
3. `Status`
   You will be asked for `Status`.
4. `Project`
   You will be asked for `Project name`.
5. `Tag`
   You will be asked for `Tag keyword`.
6. `Specific date`
   You will be asked for `Date (yyyy-mm-dd)`.
7. `Date range`
   You will be asked for:
   `From date (yyyy-mm-dd)`
   `To date (yyyy-mm-dd)`

Output format:

- Each task is printed as:
  `taskId | title | status | due=date | project=projectName`

If no tasks match:

- The program prints `No tasks found.`

### 8. Search Tasks

Use this option to search for tasks with flexible filters.

You will be asked for:

- `Task name contains (blank to skip)`
- `Status (blank to skip)`
- `From due date yyyy-mm-dd (blank to skip)`
- `To due date yyyy-mm-dd (blank to skip)`
- `Day of week (blank to skip)`

Notes:

- You can leave any field blank to ignore that filter.
- The day of week should be a valid weekday such as `MONDAY`, `Tuesday`, etc.

### 9. Export CSV

Use this option to export tasks to a CSV file.

You will be asked for:

- `CSV file path`

Result:

- The program writes a CSV file to the path you provide.

### 10. Import CSV

Use this option to import tasks from a CSV file.

You will be asked for:

- `CSV file path`

CSV expectations:

- The file should contain 10 columns:
  `TaskName, Description, Subtask, Status, Priority, DueDate, ProjectName, ProjectDescription, Collaborator, CollaboratorCategory`

Notes:

- If a project listed in the CSV does not exist yet, it is created automatically.
- If collaborator data is included, the task must belong to a project.

### 11. Export iCalendar

Use this option to export tasks into an `.ics` calendar file.

You will first be asked for:

- `Export type (task/project/search)`

Cases:

1. `task`
   You will be asked for `Task id`.
   Only that task is exported.
2. `project`
   You will be asked for `Project id`.
   All tasks in that project are exported.
3. `search`
   The program shows `Filtered export criteria:` and then asks for the same search filters used in `Search Tasks`:
   `Task name contains`
   `Status`
   `From due date yyyy-mm-dd`
   `To due date yyyy-mm-dd`
   `Day of week`

Notes:

- The exported file is created as `tasks.ics`.
- Tasks without a due date are skipped during iCalendar export.

### 12. View Activity History

Use this option to display the activity log of one task.

You will be asked for:

- `Task id`

Result:

- The program prints each activity entry as:
  `timestamp | activity description`

Examples of logged activity include task creation, status changes, due date changes, project assignment, and collaborator linking.

### 13. Show Overloaded Collaborators

Use this option to display collaborators whose open-task load exceeds their allowed maximum.

Result:

- The program prints:
  `- collaboratorName (openTasks/maxTasks)`
- If none are overloaded, the program prints `None`.

### 14. List All Tasks

Use this option to print every task currently stored in the system.

Output format:

- `taskId | title | status | due=date | project=projectName`

If no tasks exist:

- The program prints `No tasks found.`

### 0. Exit

Use this option to close the program.

Before exiting:

- The program saves the current database to `taskdb.ser`.

## Important Rules And Limits

- Task priority is expected to be between `1` and `5`.
- Task status must be `open` or `completed`.
- A task can have at most 20 subtasks.
- The system prevents duplicate tasks with the same title and due date.
- The system prevents more than 50 open tasks that have no due date.
- Collaborators can only be linked to tasks that are part of a project.