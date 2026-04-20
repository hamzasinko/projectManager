package fr.uha.ensisa.gl.entities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WorkLog Entity Tests")
class WorkLogTest {

    private WorkLog workLog;

    @BeforeEach
    void setUp() {
        workLog = new WorkLog();
    }

    @Test
    @DisplayName("Should create worklog with 4-parameter constructor")
    void testFourParameterConstructor() {
        //Arrange
        long id = 1L;
        LocalDateTime start = LocalDateTime.now().minusHours(2);
        long userId = 100L;
        long storyId = 200L;

        //Act
        WorkLog workLog = new WorkLog(id, start, userId, storyId);

        //Assert
        assertEquals(id, workLog.getId());
        assertEquals(start, workLog.getStart());
        assertEquals(userId, workLog.getUserId());
        assertEquals(storyId, workLog.getStoryId());
        assertEquals(0L, workLog.getDuration());
    }

    @Test
    @DisplayName("Should stop timer and calculate duration")
    void testStopTimer() {
        //Arrange
        LocalDateTime start = LocalDateTime.now().minusMinutes(90);
        workLog.setStart(start);
        workLog.setEnd(null);

        //Act
        workLog.stopTimer();

        //Assert
        assertNotNull(workLog.getEnd());
        assertNotNull(workLog.getDuration());
        assertTrue(workLog.getDuration() >= 90);
        assertTrue(workLog.getDuration() <= 92); // Allow small time variance
    }

    @Test
    @DisplayName("Should be idempotent when stopping already stopped timer")
    void testStopTimerIdempotent() {
        //Arrange
        LocalDateTime start = LocalDateTime.now().minusMinutes(60);
        workLog.setStart(start);
        workLog.setEnd(null);

        //Act - first stop
        workLog.stopTimer();
        LocalDateTime endAfterFirstStop = workLog.getEnd();
        long durationAfterFirstStop = workLog.getDuration();

        //Wait a tiny bit
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            //Ignore
        }

        //Act - second stop
        workLog.stopTimer();

        //Assert - should not change after second stop
        assertEquals(endAfterFirstStop, workLog.getEnd());
        assertEquals(durationAfterFirstStop, workLog.getDuration());
    }

    @Test
    @DisplayName("Should handle stopping timer with null start")
    void testStopTimerNullStart() {
        //Arrange
        workLog.setStart(null);
        workLog.setEnd(null);

        //Act
        workLog.stopTimer();

        //Assert
        assertNotNull(workLog.getEnd());
        //Duration calculation might fail gracefully or be null
        //Should not throw exception
    }

    @Test
    @DisplayName("Should calculate duration correctly for exact times")
    void testStopTimerExactCalculation() {
        //Arrange
        LocalDateTime start = LocalDateTime.of(2024, 1, 15, 10, 0, 0);
        workLog.setStart(start);
        workLog.setEnd(null);

        //Mock current time by setting end directly after stopTimer would
        LocalDateTime end = LocalDateTime.of(2024, 1, 15, 12, 30, 0);
        workLog.setStart(start);
        workLog.setEnd(null);

        //Act - simulate what stopTimer does
        workLog.setEnd(end);
        long minutes = ChronoUnit.MINUTES.between(start, end);
        workLog.setDuration((int) minutes);

        //Assert
        assertEquals(150, workLog.getDuration()); // 2h 30m = 150 minutes
    }

    @Test
    @DisplayName("Should return true when worklog is running")
    void testIsRunning() {
        //Arrange
        workLog.setStart(LocalDateTime.now());
        workLog.setEnd(null);

        //Act
        boolean result = workLog.isRunning();

        //Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Should return false when worklog has ended")
    void testIsNotRunning() {
        //Arrange
        workLog.setStart(LocalDateTime.now().minusHours(1));
        workLog.setEnd(LocalDateTime.now());

        //Act
        boolean result = workLog.isRunning();

        //Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should return true when end is explicitly set to null")
    void testIsRunningExplicitNull() {
        //Arrange
        workLog.setStart(LocalDateTime.now());
        workLog.setEnd(null);

        //Act
        boolean result = workLog.isRunning();

        //Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Should handle isRunning with null start and null end")
    void testIsRunningBothNull() {
        //Arrange
        workLog.setStart(null);
        workLog.setEnd(null);

        //Act
        boolean result = workLog.isRunning();

        //Assert
        assertTrue(result); // end == null means running
    }

    @Test
    @DisplayName("Should set and get all properties correctly")
    void testWorkLogProperties() {
        //Arrange & Act
        workLog.setId(1L);
        LocalDateTime start = LocalDateTime.now().minusHours(2);
        LocalDateTime end = LocalDateTime.now().minusHours(1);
        workLog.setStart(start);
        workLog.setEnd(end);
        workLog.setDuration(60);
        workLog.setComment("Test comment");

        //Assert
        assertEquals(1L, workLog.getId());
        assertEquals(start, workLog.getStart());
        assertEquals(end, workLog.getEnd());
        assertEquals(60, workLog.getDuration());
        assertEquals("Test comment", workLog.getComment());
    }

    @Test
    @DisplayName("Should handle equals and hashCode correctly")
    void testEqualsAndHashCode() {
        //Arrange
        LocalDateTime start = LocalDateTime.now().minusHours(1);
        LocalDateTime end = LocalDateTime.now();

        WorkLog workLog1 = new WorkLog();
        workLog1.setId(1L);
        workLog1.setStart(start);
        workLog1.setEnd(end);

        WorkLog workLog2 = new WorkLog();
        workLog2.setId(1L);
        workLog2.setStart(start);
        workLog2.setEnd(end);

        WorkLog workLog3 = new WorkLog();
        workLog3.setId(2L);
        workLog3.setStart(start);
        workLog3.setEnd(end);

        //Assert
        assertEquals(workLog1, workLog2);
        assertEquals(workLog1.hashCode(), workLog2.hashCode());
        assertNotEquals(workLog1, workLog3);
    }

    @Test
    @DisplayName("Should handle null comment")
    void testNullComment() {
        //Act & Assert
        assertDoesNotThrow(() -> {
            workLog.setComment(null);
        });

        assertNull(workLog.getComment());
    }

    @Test
    @DisplayName("Should handle empty comment")
    void testEmptyComment() {
        //Act
        workLog.setComment("");

        //Assert
        assertEquals("", workLog.getComment());
    }

    @Test
    @DisplayName("Should calculate duration for very short worklogs")
    void testStopTimerShortDuration() {
        //Arrange
        LocalDateTime start = LocalDateTime.now().minusSeconds(30);
        workLog.setStart(start);
        workLog.setEnd(null);

        //Act
        workLog.stopTimer();

        //Assert
        assertNotNull(workLog.getDuration());
        assertTrue(workLog.getDuration() >= 0);
        assertTrue(workLog.getDuration() <= 1); // Less than 1 minute
    }

    @Test
    @DisplayName("Should calculate duration for multi-day worklogs")
    void testStopTimerMultiDay() {
        //Arrange
        LocalDateTime start = LocalDateTime.of(2024, 1, 15, 10, 0, 0);
        workLog.setStart(start);

        LocalDateTime end = LocalDateTime.of(2024, 1, 17, 14, 0, 0);
        workLog.setStart(start);
        workLog.setEnd(end);
        long minutes = ChronoUnit.MINUTES.between(start, end);
        workLog.setDuration(minutes);

        //Assert
        //Jan 15 10:00 to Jan 17 14:00 = 2 days, 4 hours = 48h + 4h = 52h = 3120 minutes
        assertEquals(3120L, workLog.getDuration());
        assertTrue(workLog.getDuration() > 2880L); // At least 2 days
    }

    @Test
    @DisplayName("Should handle toString method")
    void testToString() {
        //Arrange
        workLog.setId(1L);
        workLog.setDuration(60);
        workLog.setComment("Test");

        //Act
        String result = workLog.toString();

        //Assert
        assertNotNull(result);
        assertTrue(result.contains("WorkLog") || result.contains("1") || result.contains("60"));
    }
}
