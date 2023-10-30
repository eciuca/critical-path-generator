import au.com.bytecode.opencsv.CSVReader;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Predicate;

@Slf4j
public class TaskScheduler {

    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_DATE;
    public static final int COLUMN_TASK_ID = 0;
    public static final int COLUMN_DEPENDENCIES = 1;
    public static final int COLUMN_DURATION = 2;
    public static final int COLUMN_MIN_DURATION = 3;
    public static final int COLUMN_MAX_DURATION = 4;
    public static final String SEPARATOR = ",";

    public static void main(String[] args) {
        LocalDate startDate = LocalDate.of(2023, 10, 30);

        Map<String, Task> taskMap = new HashMap<>();

        File inputFile = Paths.get("src/main/resources/tasks.csv").toFile();
        File outputFile = Paths.get("src/main/resources/result.csv").toFile();

        try (Reader inputReader = new FileReader(inputFile);
             CSVReader reader = new CSVReader(inputReader, SEPARATOR.charAt(0));
             FileWriter fileWriter = new FileWriter(outputFile)) {

            String[] nextLine;

            writeHeader(reader, fileWriter);

            // Read tasks from CSV and create Task objects
            while ((nextLine = reader.readNext()) != null) {
                String taskId = nextLine[COLUMN_TASK_ID].trim();
                log.info("Reading task " + taskId);
                String dependencies = nextLine[COLUMN_DEPENDENCIES];
                double duration = Double.parseDouble(nextLine[COLUMN_DURATION].trim());

                if (nextLine.length > 3) {
                    double minDuration = Double.parseDouble(nextLine[COLUMN_MIN_DURATION].trim());
                    double maxDuration = Double.parseDouble(nextLine[COLUMN_MAX_DURATION].trim());

                    Task task = Task.of(taskId, dependencies, duration, minDuration, maxDuration);
                    taskMap.put(taskId, task);
                } else {
                    Task task = Task.of(taskId, dependencies, duration);
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
                    log.info("Handle dependencies for task " + task.getId());
                    String[] dependencies = task.getDependenciesAsArray().length == 1 && task.getDependenciesAsArray()[0].equals("all")
                            ? taskMap.keySet().stream().filter(taskId -> !taskId.equals(task.getId())).toArray(String[]::new)
                            : task.getDependenciesAsArray();
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
                        log.info("Not all dependencies are computed for task: " + task.getId());
                    }
                });
            } while (!tasksWithDependencies.isEmpty());

            // Output the results
            taskMap.values().stream()
                    .sorted()
                    .map(Task::toCSVLine)
                    .forEach(csvLine -> {
                        try {
                            fileWriter.write(csvLine + "\n");
                        } catch (IOException e) {
                            log.error("Failed to write line: " + csvLine, e);
                        }
                    });

        } catch (IOException e) {
            log.error("Failed to read input file", e);
        }
    }

    private static void writeHeader(CSVReader reader, FileWriter fileWriter) throws IOException {
        String[] header = reader.readNext();
        fileWriter.write(String.join(SEPARATOR, header) + "#SEP#start-date#SEP#end-date\n".replaceAll("#SEP#", SEPARATOR));
    }

}
