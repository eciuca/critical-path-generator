import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

public class TaskTest {

    @Test
    public void givenStartDateOnMondayWhenSetStartAndEndDateIsCalledWith1DayTaskThenEndOnTuesday() {
        Task task = new Task("1", "2,3", 1, 0.5, 1.5);

        task.setStartAndEndDate(LocalDateTime.of(2023, 10, 3, 0, 0));

        Assertions.assertEquals(LocalDateTime.of(2023, 10, 3, 0, 0), task.getStart());
        Assertions.assertEquals(LocalDateTime.of(2023, 10, 4, 0, 0), task.getEnd());
    }

    @Test
    public void givenStartDateOnFridayWhenSetStartAndEndDateIsCalledWith1DayTaskThenEndOnMonday() {
        Task task = new Task("1", "2,3", 1, 0.5, 1.5);

        task.setStartAndEndDate(LocalDateTime.of(2023, 10, 6, 0, 0));

        Assertions.assertEquals(LocalDateTime.of(2023, 10, 6, 0, 0), task.getStart());
        Assertions.assertEquals(LocalDateTime.of(2023, 10, 9, 0, 0), task.getEnd());
    }

    @Test
    public void givenStartDateOnMondayWhenSetStartAndEndDateIsCalledWith7DaysTaskThenSkipWeekendAndEndOnWednesday() {
        Task task = new Task("1", "2,3", 7, 0.5, 1.5);

        task.setStartAndEndDate(LocalDateTime.of(2023, 10, 2, 0, 0));

        Assertions.assertEquals(LocalDateTime.of(2023, 10, 2, 0, 0), task.getStart());
        Assertions.assertEquals(LocalDateTime.of(2023, 10, 11, 0, 0), task.getEnd());
    }

    @Test
    public void givenStartDateOnMondayWhenSetStartAndEndDateIsCalledWith12DaysTaskThenSkipWeekendAndEndOnWednesday() {
        Task task = new Task("1", "2,3", 10, 0.5, 1.5);

        task.setStartAndEndDate(LocalDateTime.of(2023, 10, 2, 0, 0));


        Assertions.assertEquals(LocalDateTime.of(2023, 10, 2, 0, 0), task.getStart());
        Assertions.assertEquals(LocalDateTime.of(2023, 10, 16, 0, 0), task.getEnd());
    }

}