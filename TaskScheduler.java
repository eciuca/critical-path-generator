import au.com.bytecode.opencsv.CSVReader;

import java.io.IOException;
import java.io.StringReader;
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

    public static void main(String[] args) {
        String input = """
                Task,Dependencies,Duration
                1,,0
                2,1,10.00
                3,2,25.00
                4,1,0.50
                5,4,0.50
                6,4,90.00
                7,"3, 4, 5",15.00
                8,"5, 7",10.00
                9,"5, 6",20.00
                10,5,25.00
                11,1,10.00
                12,"3, 7",22.00
                13,"3, 12",35.00
                14,1,3.00
                15,"1, 6, 11, 12 , 13",9.25
                16,"1, 11",4.00
                17,1,4.50
                18,"1, 6, 11, 12, 13",12.00
                19,1,30.00
                20,"1, 11, 12, 13, 19",5.00
                21,1,10.00
                22,"1, 12",16.00
                23,12,40.00
                24,"all",0           
                    """;
        LocalDate startDate = LocalDate.of(2023,7,19);
//        String csvFile = "tasks.csv"; // Replace with the path to your CSV file

        Map<String, Task> taskMap = new HashMap<>();

        try (CSVReader reader = new CSVReader(new StringReader(input))) {
//        try (CSVReader reader = new CSVReader(new FileReader(csvFile))) {
            String[] nextLine;

            // Skip the header line
            reader.readNext();

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
                        .toList();

                tasksWithDependencies.forEach(task -> {
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
                });
            } while (!tasksWithDependencies.isEmpty());

            // Output the results
            taskMap.values().stream()
                    .sorted(Comparator.comparing(task -> Integer.parseInt(task.id()), Comparator.naturalOrder()))
                    .map(Task::toCSVLine)
                    .forEach(System.out::println);

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
            String csvLineNoMinMax = "\"%s\",\"%s\",\"%s\",\"%s\",\"%s\""
                    .formatted(id, dependencies, duration, formatDate(start), formatDate(end));

            if (minDuration > -1) {
                csvLineNoMinMax += ",\"%s\",\"%s\"".formatted( formatDate(maxStart), formatDate(maxEnd));
            }
            if (maxDuration > -1) {
                csvLineNoMinMax += ",\"%s\",\"%s\"".formatted( formatDate(minStart), formatDate(minEnd));
            }

            return csvLineNoMinMax;
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
