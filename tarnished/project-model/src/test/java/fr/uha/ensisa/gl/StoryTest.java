package fr.uha.ensisa.gl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import fr.uha.ensisa.gl.entities.Story;
import fr.uha.ensisa.gl.entities.StoryStatus;
import fr.uha.ensisa.gl.entities.User;
import fr.uha.ensisa.gl.entities.WorkLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

public class StoryTest {

    private Story sut;

    @BeforeEach
    void setUp() {
        sut = new Story();
    }

    @Test
    @DisplayName("Should set and get story ID correctly")
    void testSetGetId() {
        sut.setId(1);
        assertEquals(1, sut.getId(), "Story ID should be 1");
    }

    @Test
    @DisplayName("Should set and get title correctly")
    void testSetGetTitle() {
        sut.setTitle("Test Story");
        assertEquals("Test Story", sut.getTitle(), "Story title should match");
    }

    @Test
    @DisplayName("Should handle null title")
    void testNullTitle() {
        sut.setTitle(null);
        assertNull(sut.getTitle(), "Story title should be null");
    }

    @Test
    @DisplayName("Should handle empty title")
    void testEmptyTitle() {
        sut.setTitle("");
        assertEquals("", sut.getTitle(), "Story title should be empty string");
    }

    @Test
    @DisplayName("Should set and get description correctly")
    void testSetGetDescription() {
        sut.setDescription("Test Description");
        assertEquals("Test Description", sut.getDescription(), "Description should match");
    }

    @Test
    @DisplayName("Should handle null description")
    void testNullDescription() {
        sut.setDescription(null);
        assertNull(sut.getDescription(), "Description should be null");
    }

    @Test
    @DisplayName("Should set and get status correctly")
    void testSetGetStatus() {
        sut.setStatus(StoryStatus.DONE);
        assertEquals(StoryStatus.DONE, sut.getStatus(), "Status should be DONE");
    }

    @Test
    @DisplayName("Should handle all status values")
    void testAllStatusValues() {
        sut.setStatus(StoryStatus.BACKLOG);
        assertEquals(StoryStatus.BACKLOG, sut.getStatus());
        
        sut.setStatus(StoryStatus.IN_PROGRESS);
        assertEquals(StoryStatus.IN_PROGRESS, sut.getStatus());
        
        sut.setStatus(StoryStatus.REVIEW);
        assertEquals(StoryStatus.REVIEW, sut.getStatus());
        
        sut.setStatus(StoryStatus.DONE);
        assertEquals(StoryStatus.DONE, sut.getStatus());
        
        sut.setStatus(StoryStatus.BLOCKED);
        assertEquals(StoryStatus.BLOCKED, sut.getStatus());
    }

    @Test
    @DisplayName("Should assign user correctly")
    void testSetGetUserAssigned() {
        User user = new User(1, "John Doe", "john@example.com", "password", new ArrayList<>());
        sut.setUserAssigned(user);
        assertEquals(user, sut.getUserAssigned(), "Assigned user should match");
        assertEquals("John Doe", sut.getUserAssigned().getName(), "User name should be John Doe");
    }

    @Test
    @DisplayName("Should handle null user assignment")
    void testNullUserAssignment() {
        sut.setUserAssigned(null);
        assertNull(sut.getUserAssigned(), "Assigned user should be null");
    }

    @Test
    @DisplayName("Should set and get start date correctly")
    void testSetGetDateStart() {
        Date date = new Date();
        sut.setDateStart(date);
        assertEquals(date, sut.getDateStart(), "Start date should match");
    }

    @Test
    @DisplayName("Should set and get created date correctly")
    void testSetGetDateCreated() {
        Date date = new Date();
        sut.setDateCreated(date);
        assertEquals(date, sut.getDateCreated(), "Created date should match");
    }

    @Test
    @DisplayName("Should set and get end date correctly")
    void testSetGetDateEnd() {
        Date date = new Date();
        sut.setDateEnd(date);
        assertEquals(date, sut.getDateEnd(), "End date should match");
    }

    @Test
    @DisplayName("Should add work log successfully")
    void testAddWorkLog() {
        WorkLog log = new WorkLog();
        log.setId(1);
        log.setDuration(30);
        List<WorkLog> modifiedWorkLogs = sut.getWorkLogs();
        modifiedWorkLogs.add(log);
        sut.setWorkLogs(modifiedWorkLogs);
        
        assertEquals(1, sut.getWorkLogs().size(), "Should have 1 work log");
        assertEquals(30, sut.getWorkLogs().get(0).getDuration(), "Work log duration should be 30");
    }

    @Test
    @DisplayName("Should add multiple work logs successfully")
    void testAddMultipleWorkLogs() {
        WorkLog log1 = new WorkLog();
        log1.setId(1);
        log1.setDuration(30);
        WorkLog log2 = new WorkLog();
        log2.setId(2);
        log2.setDuration(45);
        
        List<WorkLog> modifiedWorkLogs = sut.getWorkLogs();
        modifiedWorkLogs.add(log1);
        modifiedWorkLogs.add(log2);
        sut.setWorkLogs(modifiedWorkLogs);
        
        assertEquals(2, sut.getWorkLogs().size(), "Should have 2 work logs");
    }

    @Test
    @DisplayName("Should ignore removal when list is null")
    void testRemoveWorkLogNullList() {
        sut.setWorkLogs(null);

        sut.removeWorkLog(99);

        assertNull(sut.getWorkLogs());
    }

    @Test
    @DisplayName("Should initialize with empty work logs list")
    void testWorkLogsInitialization() {
        assertNotNull(sut.getWorkLogs(), "Work logs list should not be null");
        assertEquals(0, sut.getWorkLogs().size(), "Work logs list should be empty initially");
    }

    @Test
    @DisplayName("Should create story with NoArgsConstructor")
    void testNoArgsConstructor() {
        Story story = new Story();
        assertNotNull(story, "Story should be created");
        assertNotNull(story.getWorkLogs(), "Work logs should be initialized");
    }

    @Test
    @DisplayName("Should create story with AllArgsConstructor")
    void testAllArgsConstructor() {
        int id = 1;
        String title = "Test Story";
        String description = "Test Description";
        StoryStatus status = StoryStatus.IN_PROGRESS;
        User user = new User(1, "John", "john@example.com", "pass", new ArrayList<>());
        Date dateStart = new Date();
        Date dateCreated = new Date();
        Date dateEnd = new Date();
        List<WorkLog> workLogs = new ArrayList<>();
        long totalTimeSpent = 0;
        Long projectId = null;
        Long columnId = null;
        
        Story story = new Story();
        story.setId(id);
        story.setTitle(title);
        story.setDescription(description);
        story.setStatus(status);
        story.setUserAssigned(user);
        story.setDateStart(dateStart);
        story.setDateCreated(dateCreated);
        story.setDateEnd(dateEnd);
        story.setWorkLogs(workLogs);
        story.setTotalTimeSpent(totalTimeSpent);
        story.setProjectId(projectId);
        story.setColumnId(columnId);
        
        assertEquals(id, story.getId());
        assertEquals(title, story.getTitle());
        assertEquals(description, story.getDescription());
        assertEquals(status, story.getStatus());
        assertEquals(user, story.getUserAssigned());
        assertEquals(dateStart, story.getDateStart());
        assertEquals(dateCreated, story.getDateCreated());
        assertEquals(dateEnd, story.getDateEnd());
        assertEquals(workLogs, story.getWorkLogs());
    }

    @Test
    @DisplayName("Should correctly compare equal stories")
    void testEquals() {
        Story story1 = new Story();
        story1.setId(1);
        story1.setTitle("Test");

        Story story2 = new Story();
        story2.setId(1);
        story2.setTitle("Test");

        assertEquals(story1, story2, "Stories with same data should be equal");
    }

    @Test
    @DisplayName("Should generate consistent hashCode")
    void testHashCode() {
        Story story1 = new Story();
        story1.setId(1);
        story1.setTitle("Test");

        Story story2 = new Story();
        story2.setId(1);
        story2.setTitle("Test");

        assertEquals(story1.hashCode(), story2.hashCode(), "Equal stories should have same hashCode");
    }

    @Test
    @DisplayName("Should generate meaningful toString")
    void testToString() {
        sut.setId(1);
        sut.setTitle("Test Story");
        
        String toString = sut.toString();
        assertNotNull(toString, "toString should not be null");
        assertTrue(toString.contains("1"), "toString should contain ID");
        assertTrue(toString.contains("Test Story"), "toString should contain title");
    }

    @Test
    @DisplayName("Should calculate total time correctly from worklogs")
    void testCalculateTotalTimeFromWorkLogs() {
        List<WorkLog> workLogs = new ArrayList<>();
        
        WorkLog workLog1 = new WorkLog();
        workLog1.setDuration(30);
        workLogs.add(workLog1);
        
        WorkLog workLog2 = new WorkLog();
        workLog2.setDuration(45);
        workLogs.add(workLog2);
        
        sut.setWorkLogs(workLogs);

        sut.calculateTotalTime();

        assertEquals(75, sut.getTotalTimeSpent());
    }

    @Test
    @DisplayName("Should add work log and initialize list when null")
    void testAddWorkLogInitializesList() {
        sut.setWorkLogs(null);
        WorkLog workLog = new WorkLog();
        workLog.setDuration(15);

        sut.addWorkLog(workLog);

        assertEquals(1, sut.getWorkLogs().size());
        assertEquals(15, sut.getTotalTimeSpent());
    }

    @Test
    @DisplayName("Should remove work log by id and recalculate total time")
    void testRemoveWorkLog() {
        WorkLog first = new WorkLog();
        first.setId(1);
        first.setDuration(20);
        WorkLog second = new WorkLog();
        second.setId(2);
        second.setDuration(10);

        sut.addWorkLog(first);
        sut.addWorkLog(second);

        sut.removeWorkLog(1);

        assertEquals(1, sut.getWorkLogs().size());
        assertEquals(10, sut.getTotalTimeSpent());
    }

    @Test
    @DisplayName("Should calculate total time to zero when workLogs is null")
    void testCalculateTotalTimeNullSafe() {
        sut.setWorkLogs(null);

        sut.calculateTotalTime();

        assertEquals(0, sut.getTotalTimeSpent());
    }

    @Test
    @DisplayName("Should return running work log when available")
    void testGetRunningWorkLog() {
        WorkLog stopped = new WorkLog();
        stopped.setId(1);
        stopped.stopTimer();

        WorkLog running = new WorkLog();
        running.setId(2);

        sut.addWorkLog(stopped);
        sut.addWorkLog(running);

        WorkLog result = sut.getRunningWorkLog();

        assertNotNull(result);
        assertEquals(2, result.getId());
    }

    @Test
    @DisplayName("Should return null when no running work log exists")
    void testGetRunningWorkLogEmpty() {
        assertNull(sut.getRunningWorkLog());
    }
}