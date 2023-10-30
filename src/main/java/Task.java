import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;

@Data
@RequiredArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class Task implements Comparable<Task> {

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

    public static Task of(String id, String dependencies, double duration) {
        return new Task(id, dependencies, duration, -1, -1);
    }
    public static Task of(String id, String dependencies, double duration, double minDuration, double maxDuration) {
        return new Task(id, dependencies, duration, minDuration, maxDuration);
    }

    public boolean hasDependencies() {
        return dependencies != null && !dependencies.trim().isEmpty();
    }

    public String[] getDependenciesAsArray() {
        return Arrays.stream(dependencies.split(",")).map(String::trim).toArray(String[]::new);
    }

    public void setStartAndEndDate(LocalDateTime start) {
        this.start = start;
        this.end = computeEnd(start);
    }

    private LocalDateTime computeEnd(LocalDateTime start) {
        long hours = manDaysToHours(duration);

        int noOfWeekends = (int) duration / 7;
        var end = start.plusHours(hours).plusDays(noOfWeekends * 2);

        DayOfWeek dayOfWeek = end.getDayOfWeek();
        end = switch (dayOfWeek) {
            case SATURDAY -> end.plusDays(2);
            case SUNDAY -> end.plusDays(1);
            default -> end;
        };
        return end;
    }

    public void setMinStartAndEndDate(LocalDateTime start) {
        this.minStart = start;
        this.minEnd = computeEnd(start);
    }

    public void setMaxStartAndEndDate(LocalDateTime start) {
        this.maxStart = start;
        this.maxEnd = computeEnd(start);
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
                .replaceAll("#SEP#", TaskScheduler.SEPARATOR);
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

    private static long manDaysToHours(double duration) {
        return Math.round(24 * duration);
    }

    @Override
    public int compareTo(Task o) {
        return Comparator.comparing((Task task) -> Integer.parseInt(task.getId()), Comparator.naturalOrder())
                .compare(this, o);
    }
}
