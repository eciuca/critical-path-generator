import au.com.bytecode.opencsv.CSVReader;

import java.io.*;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Predicate;

public class TaskScheduler {

    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_DATE;
    public static final int COLUMN_TASK_ID = 0;
    public static final int COLUMN_DEPENDENCIES = 1;
    public static final int COLUMN_DURATION = 2;
    public static final int COLUMN_MIN_DURATION = 3;
    public static final int COLUMN_MAX_DURATION = 4;
    public static final String SEPARATOR = ",";

    public static void main(String[] args) throws FileNotFoundException {
        Set<String> alreadyProcessed = new HashSet<>();
        LocalDate startDate = LocalDate.of(2023, 10, 30);

        Map<String, Task> taskMap = new HashMap<>();

        File inputFile = Paths.get("src/main/resources/tasks.csv").toFile();
        File outputFile = Paths.get("src/main/resources/result.csv").toFile();

        try (Reader inputReader = new FileReader(inputFile);
             CSVReader reader = new CSVReader(inputReader, SEPARATOR.charAt(0));
             FileWriter fileWriter = new FileWriter(outputFile)) {


            String[] nextLine;

            String[] header = reader.readNext();
            fileWriter.write(String.join(SEPARATOR, header) + ";start-date;end-date\n");

            // Read tasks from CSV and create Task objects
            while ((nextLine = reader.readNext()) != null) {
                String taskId = nextLine[COLUMN_TASK_ID].trim();
                System.out.println("Reading task " + taskId);
                String dependencies = nextLine[COLUMN_DEPENDENCIES];
                double duration = Double.parseDouble(nextLine[COLUMN_DURATION].trim());

                if (nextLine.length > 3) {
                    double minDuration = Double.parseDouble(nextLine[COLUMN_MIN_DURATION].trim());
                    double maxDuration = Double.parseDouble(nextLine[COLUMN_MAX_DURATION].trim());

                    Task task = new Task(taskId, dependencies, duration, minDuration, maxDuration);
                    taskMap.put(taskId, task);
                } else {
                    Task task = new Task(taskId, dependencies, duration);
                    taskMap.put(taskId, task);
                }
            }

            List<Task> tasksWithoutDependencies = taskMap.values().stream()
                    .filter(Predicate.not(Task::hasDependencies))
                    .toList();

            tasksWithoutDependencies.forEach(task -> {
                task.setStartAndEndDate(startDate.atStartOfDay());
                task.setMinStartAndEndDate(startDate.atStartOfDay());
                task.setMaxStartAndEndDate(startDate.atStartOfDay());
            });

            List<Task> tasksWithDependencies;
            do {
                tasksWithDependencies = taskMap.values().stream()
                        .filter(Task::hasDependencies)
                        .filter(task -> task.getStart() == null)
                        .sorted(Comparator.comparing(Task::getId))
                        .toList();

                tasksWithDependencies.forEach(task -> {
//                    if (alreadyProcessed.contains(task.getId())) {
//                        throw new RuntimeException("Circular dependency for task: " + task.getId());
//                    } else {
//                        alreadyProcessed.add(task.getId());
//                    }
                    System.out.println("Handle dependencies for task " + task.getId());
                    String[] dependencies = task.getDependencies().length == 1 && task.getDependencies()[0].equals("all")
                            ? taskMap.keySet().stream().filter(taskId -> !taskId.equals(task.getId())).toArray(String[]::new)
                            : task.getDependencies();
                    boolean allDependenciesAreComputed = Arrays.stream(dependencies).allMatch(taskId -> taskMap.containsKey(taskId) && taskMap.get(taskId).getEnd() != null);
                    if (allDependenciesAreComputed) {
                        LocalDateTime maxFinishDateOfDependencies = Arrays.stream(dependencies)
                                .map(taskMap::get)
                                .map(Task::getEnd).max(Comparator.naturalOrder())
                                .orElseThrow(IllegalStateException::new);
                        task.setStartAndEndDate(maxFinishDateOfDependencies);

                        if (task.getMaxDuration() > -1) {
                            LocalDateTime maxMaxFinishDateOfDependencies = Arrays.stream(dependencies)
                                    .map(taskMap::get)
                                    .map(Task::getMaxEnd).max(Comparator.naturalOrder())
                                    .orElseThrow(IllegalStateException::new);
                            task.setMaxStartAndEndDate(maxMaxFinishDateOfDependencies);
                        }

                        if (task.getMinDuration() > -1) {
                            LocalDateTime maxMinFinishDateOfDependencies = Arrays.stream(dependencies)
                                    .map(taskMap::get)
                                    .map(Task::getMinEnd).max(Comparator.naturalOrder())
                                    .orElseThrow(IllegalStateException::new);
                            task.setMinStartAndEndDate(maxMinFinishDateOfDependencies);
                        }
                    }
                    else {
                        System.out.println("Not all dependencies are computed for task: " + task.getId());
                    }
                });
            } while (!tasksWithDependencies.isEmpty());

            // Output the results
            taskMap.values().stream()
                    .sorted(Comparator.comparing(task -> Integer.parseInt(task.id()), Comparator.naturalOrder()))
                    .map(Task::toCSVLine)
                    .forEach(csvLine -> {
                        try {
                            fileWriter.write(csvLine + "\n");
                        } catch (IOException e) {
                            System.out.println("Failed to write line: " + csvLine);
                        }
                    });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static long manDaysToHours(double duration) {
        return Math.round(24 * duration);
    }

    static final class Task {
        private final String id;
        private final String dependencies;
        private final double duration;
        private final double minDuration;
        private final double maxDuration;
        private LocalDateTime start;
        private LocalDateTime end;
        private LocalDateTime minStart;
        private LocalDateTime minEnd;
        private LocalDateTime maxStart;
        private LocalDateTime maxEnd;

        Task(String id, String dependencies, double duration) {
            this(id, dependencies, duration, -1, -1);
        }

        Task(String id, String dependencies, double duration, double minDuration, double maxDuration) {
            this.id = id;
            this.dependencies = dependencies;
            this.duration = duration;
            this.minDuration = minDuration;
            this.maxDuration = maxDuration;
        }

        public String id() {
            return id;
        }

        public boolean hasDependencies() {
            return dependencies != null && !dependencies.trim().isEmpty();
        }

        public String dependencies() {
            return dependencies;
        }

        public double duration() {
            return duration;
        }

        public String getId() {
            return id;
        }

        public String[] getDependencies() {
            return Arrays.stream(dependencies.split(",")).map(String::trim).toArray(String[]::new);
        }

        public double getDuration() {
            return duration;
        }

        public double getMinDuration() {
            return minDuration;
        }

        public double getMaxDuration() {
            return maxDuration;
        }

        public LocalDateTime getStart() {
            return start;
        }

        public void setStart(LocalDateTime start) {
            this.start = start;
        }

        public void setStartAndEndDate(LocalDateTime start) {
            this.start = start;
            long hours = manDaysToHours(duration);
            this.end = start.plusHours(hours);
        }

        public void setMinStartAndEndDate(LocalDateTime start) {
            this.minStart = start;
            long hours = manDaysToHours(minDuration);
            this.minEnd = start.plusHours(hours);
        }

        public void setMaxStartAndEndDate(LocalDateTime start) {
            this.maxStart = start;
            long hours = manDaysToHours(maxDuration);
            this.maxEnd = start.plusHours(hours);
        }

        public LocalDateTime getMinStart() {
            return minStart;
        }

        public LocalDateTime getMinEnd() {
            return minEnd;
        }

        public LocalDateTime getMaxStart() {
            return maxStart;
        }

        public LocalDateTime getMaxEnd() {
            return maxEnd;
        }

        public LocalDateTime getEnd() {
            return end;
        }

        public void setEnd(LocalDateTime end) {
            this.end = end;
        }

        public String toCSVLine() {
            String csvLineNoMinMax = "\"%s\"#SEP#\"%s\"#SEP#\"%s\"#SEP#\"%s\"#SEP#\"%s\""
                    .formatted(id, dependencies, duration, formatDate(start), formatDate(end));

            if (minDuration > -1) {
                csvLineNoMinMax += "#SEP#\"%s\"#SEP#\"%s\"".formatted(formatDate(maxStart), formatDate(maxEnd));
            }
            if (maxDuration > -1) {
                csvLineNoMinMax += "#SEP#\"%s\"#SEP#\"%s\"".formatted(formatDate(minStart), formatDate(minEnd));
            }

            return csvLineNoMinMax
                    .replaceAll("#SEP#", SEPARATOR);
        }

        private String formatDate(LocalDateTime start) {
            return start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }

        @Override
        public String toString() {
            return "Task{" +
                    "id='" + id + '\'' +
                    ", dependencies='" + dependencies + '\'' +
                    ", duration=" + duration +
                    ", minDuration=" + minDuration +
                    ", maxDuration=" + maxDuration +
                    ", start=" + start +
                    ", end=" + end +
                    ", minStart=" + minStart +
                    ", minEnd=" + minEnd +
                    ", maxStart=" + maxStart +
                    ", maxEnd=" + maxEnd +
                    '}';
        }
    }
}
