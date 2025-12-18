package fr.uha.ensisa.gl.entities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Story Entity Tests")
class StoryTest {

    private Story story;

    @BeforeEach
    void setUp() {
        story = new Story();
        story.setTitle("Test Story");
        story.setDescription("Test Description");
        story.setStatus(StoryStatus.BACKLOG);
    }

    @Test
    @DisplayName("Should add worklog to story")
    void testAddWorkLog() {
        //Arrange
        WorkLog workLog1 = new WorkLog();
        workLog1.setId(1);
        workLog1.setStart(LocalDateTime.now().minusHours(2));
        workLog1.setEnd(LocalDateTime.now().minusHours(1));
        workLog1.setDuration(60L);

        //Act
        story.addWorkLog(workLog1);

        //Assert
        assertNotNull(story.getWorkLogs());
        assertEquals(1, story.getWorkLogs().size());
        assertTrue(story.getWorkLogs().contains(workLog1));
        assertEquals(60L, story.getTotalTimeSpent());
    }

    @Test
    @DisplayName("Should initialize worklog collection if null when adding")
    void testAddWorkLogInitializesCollection() {
        //Arrange
        story.setWorkLogs(null);
        WorkLog workLog = new WorkLog();

        //Act
        story.addWorkLog(workLog);

        //Assert
        assertNotNull(story.getWorkLogs());
        assertEquals(1, story.getWorkLogs().size());
    }

    @Test
    @DisplayName("Should add multiple worklogs and calculate total time")
    void testAddMultipleWorkLogs() {
        //Arrange
        WorkLog workLog1 = new WorkLog();
        workLog1.setId(1);
        workLog1.setDuration(60L);

        WorkLog workLog2 = new WorkLog();
        workLog2.setId(2);
        workLog2.setDuration(45L);

        WorkLog workLog3 = new WorkLog();
        workLog3.setId(3);
        workLog3.setDuration(30L);

        //Act
        story.addWorkLog(workLog1);
        story.addWorkLog(workLog2);
        story.addWorkLog(workLog3);

        //Assert
        assertEquals(3, story.getWorkLogs().size());
        assertEquals(135L, story.getTotalTimeSpent());
    }

    @Test
    @DisplayName("Should remove worklog by ID")
    void testRemoveWorkLog() {
        //Arrange
        WorkLog workLog1 = new WorkLog();
        workLog1.setId(1);
        workLog1.setDuration(60L);

        WorkLog workLog2 = new WorkLog();
        workLog2.setId(2);
        workLog2.setDuration(45L);

        story.addWorkLog(workLog1);
        story.addWorkLog(workLog2);

        //Act
        story.removeWorkLog(1L);

        //Assert
        assertEquals(1, story.getWorkLogs().size());
        assertFalse(story.getWorkLogs().contains(workLog1));
        assertTrue(story.getWorkLogs().contains(workLog2));
        assertEquals(45L, story.getTotalTimeSpent());
    }

    @Test
    @DisplayName("Should handle removing non-existent worklog")
    void testRemoveNonExistentWorkLog() {
        //Arrange
        WorkLog workLog = new WorkLog();
        workLog.setId(1);
        workLog.setDuration(60L);
        story.addWorkLog(workLog);

        //Act
        story.removeWorkLog(999L);

        //Assert
        assertEquals(1, story.getWorkLogs().size());
        assertEquals(60L, story.getTotalTimeSpent());
    }

    @Test
    @DisplayName("Should handle removing from null worklog list")
    void testRemoveWorkLogNullList() {
        //Arrange
        story.setWorkLogs(null);

        //Act & Assert - should not throw exception
        assertDoesNotThrow(() -> story.removeWorkLog(1L));
        assertNull(story.getWorkLogs());
    }

    @Test
    @DisplayName("Should calculate total time correctly")
    void testCalculateTotalTime() {
        //Arrange
        story.setWorkLogs(new ArrayList<>());
        
        WorkLog workLog1 = new WorkLog();
        workLog1.setDuration(60L);

        WorkLog workLog2 = new WorkLog();
        workLog2.setDuration(45L);

        WorkLog workLog3 = new WorkLog();
        workLog3.setDuration(30L);

        story.getWorkLogs().addAll(Arrays.asList(workLog1, workLog2, workLog3));

        //Act
        story.calculateTotalTime();

        //Assert
        assertEquals(135L, story.getTotalTimeSpent());
    }

    @Test
    @DisplayName("Should calculate zero time for empty worklog list")
    void testCalculateTotalTimeEmptyList() {
        //Arrange
        story.setWorkLogs(new ArrayList<>());

        //Act
        story.calculateTotalTime();

        //Assert
        assertEquals(0L, story.getTotalTimeSpent());
    }

    @Test
    @DisplayName("Should calculate zero time for null worklog list")
    void testCalculateTotalTimeNullList() {
        //Arrange
        story.setWorkLogs(null);

        //Act
        story.calculateTotalTime();

        //Assert
        assertEquals(0L, story.getTotalTimeSpent());
    }

    @Test
    @DisplayName("Should handle zero durations in worklogs")
    void testCalculateTotalTimeWithZeroDurations() {
        //Arrange
        story.setWorkLogs(new ArrayList<>());
        
        WorkLog workLog1 = new WorkLog();
        workLog1.setDuration(60L);

        WorkLog workLog2 = new WorkLog();
        workLog2.setDuration(0L);

        WorkLog workLog3 = new WorkLog();
        workLog3.setDuration(30L);

        story.getWorkLogs().addAll(Arrays.asList(workLog1, workLog2, workLog3));

        //Act
        story.calculateTotalTime();

        //Assert
        assertEquals(90L, story.getTotalTimeSpent());
    }

    @Test
    @DisplayName("Should find running worklog")
    void testGetRunningWorkLog() {
        //Arrange
        WorkLog completedWorkLog = new WorkLog();
        completedWorkLog.setId(1);
        completedWorkLog.setStart(LocalDateTime.now().minusHours(2));
        completedWorkLog.setEnd(LocalDateTime.now().minusHours(1));

        WorkLog runningWorkLog = new WorkLog();
        runningWorkLog.setId(2);
        runningWorkLog.setStart(LocalDateTime.now().minusMinutes(30));
        runningWorkLog.setEnd(null);

        story.setWorkLogs(new ArrayList<>(Arrays.asList(completedWorkLog, runningWorkLog)));

        //Act
        WorkLog result = story.getRunningWorkLog();

        //Assert
        assertNotNull(result);
        assertEquals(2, result.getId());
        assertNull(result.getEnd());
    }

    @Test
    @DisplayName("Should return null when no running worklog exists")
    void testGetRunningWorkLogNoneRunning() {
        //Arrange
        WorkLog workLog1 = new WorkLog();
        workLog1.setStart(LocalDateTime.now().minusHours(2));
        workLog1.setEnd(LocalDateTime.now().minusHours(1));

        WorkLog workLog2 = new WorkLog();
        workLog2.setStart(LocalDateTime.now().minusHours(1));
        workLog2.setEnd(LocalDateTime.now());

        story.setWorkLogs(new ArrayList<>(Arrays.asList(workLog1, workLog2)));

        //Act
        WorkLog result = story.getRunningWorkLog();

        //Assert
        assertNull(result);
    }

    @Test
    @DisplayName("Should return null when worklog list is null")
    void testGetRunningWorkLogNullList() {
        //Arrange
        story.setWorkLogs(null);

        //Act
        WorkLog result = story.getRunningWorkLog();

        //Assert
        assertNull(result);
    }

    @Test
    @DisplayName("Should return null when worklog list is empty")
    void testGetRunningWorkLogEmptyList() {
        //Arrange
        story.setWorkLogs(new ArrayList<>());

        //Act
        WorkLog result = story.getRunningWorkLog();

        //Assert
        assertNull(result);
    }

    @Test
    @DisplayName("Should handle multiple running worklogs (return first)")
    void testGetRunningWorkLogMultipleRunning() {
        //Arrange
        WorkLog runningWorkLog1 = new WorkLog();
        runningWorkLog1.setId(1);
        runningWorkLog1.setStart(LocalDateTime.now().minusHours(1));
        runningWorkLog1.setEnd(null);

        WorkLog runningWorkLog2 = new WorkLog();
        runningWorkLog2.setId(2);
        runningWorkLog2.setStart(LocalDateTime.now().minusMinutes(30));
        runningWorkLog2.setEnd(null);

        story.setWorkLogs(new ArrayList<>(Arrays.asList(runningWorkLog1, runningWorkLog2)));

        //Act
        WorkLog result = story.getRunningWorkLog();

        //Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
    }

    @Test
    @DisplayName("Should set and get all properties correctly")
    void testStoryProperties() {
        //Arrange & Act
        story.setId(1);
        story.setTitle("New Title");
        story.setDescription("New Description");
        story.setPosition(5);
        story.setStatus(StoryStatus.IN_PROGRESS);
        story.setSubColumn("DONE");
        story.setProjectId(10L);
        story.setColumnId(20L);

        //Assert
        assertEquals(1, story.getId());
        assertEquals("New Title", story.getTitle());
        assertEquals("New Description", story.getDescription());
        assertEquals(5, story.getPosition());
        assertEquals(StoryStatus.IN_PROGRESS, story.getStatus());
        assertEquals("DONE", story.getSubColumn());
        assertEquals(10L, story.getProjectId());
        assertEquals(20L, story.getColumnId());
    }

    @Test
    @DisplayName("Should handle equals and hashCode correctly")
    void testEqualsAndHashCode() {
        //Arrange
        Story story1 = new Story();
        story1.setId(1);
        story1.setTitle("Story 1");

        Story story2 = new Story();
        story2.setId(1);
        story2.setTitle("Story 1");

        Story story3 = new Story();
        story3.setId(2);
        story3.setTitle("Story 2");

        //Assert
        assertEquals(story1, story2);
        assertEquals(story1.hashCode(), story2.hashCode());
        assertNotEquals(story1, story3);
    }

    @Test
    @DisplayName("Should handle null values in optional fields")
    void testNullOptionalFields() {
        //Act & Assert
        assertDoesNotThrow(() -> {
            story.setDescription(null);
            story.setSubColumn(null);
            story.setWorkLogs(null);
            story.setUserAssigned(null);
            story.setProjectId(null);
            story.setColumnId(null);
        });

        assertNull(story.getDescription());
        assertNull(story.getSubColumn());
        assertNull(story.getUserAssigned());
        assertNull(story.getProjectId());
        assertNull(story.getColumnId());
    }
}
