public void exportCsv(String filePath) throws IOException {

    try (java.io.BufferedWriter writer = new java.io.BufferedWriter(new java.io.FileWriter(filePath))) {

        // :white_check_mark: Header (matches your import structure)
        writer.write("title,priority,status,dueDate");
        writer.newLine();

        for (Task t : tasks) {

            StringBuilder line = new StringBuilder();

            // title
            line.append(t.getTitle()).append(",");

            // priority
            line.append(t.getPriority()).append(",");

            // status
            line.append(t.getStatus()).append(",");

            // due date (handle null)
            if (t.getDueDate() != null) {
                line.append(t.getDueDate());
            } else {
                line.append("");
            }

            writer.write(line.toString());
            writer.newLine();
        }

        System.out.println("Activity: Exported " + tasks.size() + " tasks to " + filePath);
    }
}