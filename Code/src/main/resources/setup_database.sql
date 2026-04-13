-- Task Manager Database Setup Script
-- Execute this script to create the task_manager database and all required tables

-- Create the database
CREATE DATABASE IF NOT EXISTS task_manager;
USE task_manager;

-- Create projects table
CREATE TABLE IF NOT EXISTS projects (
    project_id INT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description LONGTEXT
);

-- Create collaborators table
CREATE TABLE IF NOT EXISTS collaborators (
    collaborator_id INT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    max_tasks INT DEFAULT 5,
    project_id INT,
    FOREIGN KEY (project_id) REFERENCES projects(project_id)
);

-- Create tasks table
CREATE TABLE IF NOT EXISTS tasks (
    task_id INT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description LONGTEXT,
    creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    priority INT CHECK (priority BETWEEN 1 AND 5),
    status VARCHAR(50) DEFAULT 'open',
    due_date DATE,
    project_id INT,
    UNIQUE KEY unique_title_duedate (title, due_date),
    FOREIGN KEY (project_id) REFERENCES projects(project_id),
    INDEX idx_status (status),
    INDEX idx_due_date (due_date)
);

-- Create subtasks table
CREATE TABLE IF NOT EXISTS subtasks (
    subtask_id INT PRIMARY KEY,
    parent_task_id INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description LONGTEXT,
    status VARCHAR(50) DEFAULT 'open',
    collaborator_id INT,
    FOREIGN KEY (parent_task_id) REFERENCES tasks(task_id),
    FOREIGN KEY (collaborator_id) REFERENCES collaborators(collaborator_id),
    INDEX idx_parent_task (parent_task_id),
    INDEX idx_collaborator (collaborator_id)
);

-- Create tags table
CREATE TABLE IF NOT EXISTS tags (
    tag_id INT PRIMARY KEY,
    keyword VARCHAR(100) NOT NULL UNIQUE
);

-- Create task_tags junction table
CREATE TABLE IF NOT EXISTS task_tags (
    task_id INT NOT NULL,
    tag_id INT NOT NULL,
    PRIMARY KEY (task_id, tag_id),
    FOREIGN KEY (task_id) REFERENCES tasks(task_id),
    FOREIGN KEY (tag_id) REFERENCES tags(tag_id)
);

-- Create activity_entries table
CREATE TABLE IF NOT EXISTS activity_entries (
    activity_id INT PRIMARY KEY,
    task_id INT NOT NULL,
    activity_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    description LONGTEXT,
    FOREIGN KEY (task_id) REFERENCES tasks(task_id),
    INDEX idx_task_id (task_id),
    INDEX idx_activity_time (activity_time)
);

-- Display completion message
SELECT 'Database setup completed successfully!' AS status;
