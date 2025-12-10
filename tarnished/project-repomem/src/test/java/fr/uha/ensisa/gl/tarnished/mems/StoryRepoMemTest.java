package fr.uha.ensisa.gl.tarnished.mems;

import fr.uha.ensisa.gl.entities.Story;
import fr.uha.ensisa.gl.entities.StoryStatus;
import fr.uha.ensisa.gl.entities.User;
import fr.uha.ensisa.gl.entities.WorkLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class StoryRepoMemTest {

    private StoryRepoMem storyRepo;
    private Story testStory;

    @BeforeEach
    void setUp() {
        storyRepo = new StoryRepoMem();
        testStory = new Story();
        testStory.setTitle("Test Story");
        testStory.setDescription("Test Description");
        testStory.setStatus(StoryStatus.BACKLOG);
        testStory.setDateCreated(new Date());
    }

    @Test
    @DisplayName("Should persist story successfully")
    void testPersistStory() {
        storyRepo.persist(testStory);
        
        assertTrue(testStory.getId() > 0, "Story should have an ID after persist");
        assertEquals(1, storyRepo.count(), "Repository should contain 1 story");
    }

    @Test
    @DisplayName("Should assign auto-incremented IDs to persisted stories")
    void testPersistAutoIncrementId() {
        Story story1 = new Story();
        story1.setTitle("Story 1");
        
        Story story2 = new Story();
        story2.setTitle("Story 2");
        
        storyRepo.persist(story1);
        storyRepo.persist(story2);
        
        assertEquals(1, story1.getId(), "First story should have ID 1");
        assertEquals(2, story2.getId(), "Second story should have ID 2");
    }

    @Test
    @DisplayName("Should find story by ID successfully")
    void testFindStoryById() {
        storyRepo.persist(testStory);
        int id = testStory.getId();
        
        Story found = storyRepo.find(id);
        
        assertNotNull(found, "Story should be found");
        assertEquals(testStory.getId(), found.getId(), "Story IDs should match");
        assertEquals("Test Story", found.getTitle(), "Story title should match");
    }

    @Test
    @DisplayName("Should return null when story not found")
    void testFindNonExistentStory() {
        Story found = storyRepo.find(999L);
        assertNull(found, "Should return null for non-existent story");
    }

    @Test
    @DisplayName("Should find all stories successfully")
    void testFindAllStories() {
        Story story1 = new Story();
        story1.setTitle("Story 1");
        
        Story story2 = new Story();
        story2.setTitle("Story 2");
        
        Story story3 = new Story();
        story3.setTitle("Story 3");
        
        storyRepo.persist(story1);
        storyRepo.persist(story2);
        storyRepo.persist(story3);
        
        Collection<Story> allStories = storyRepo.findAll();
        
        assertNotNull(allStories, "Should return a collection");
        assertEquals(3, allStories.size(), "Should contain 3 stories");
    }

    @Test
    @DisplayName("Should return empty collection when no stories exist")
    void testFindAllEmpty() {
        Collection<Story> allStories = storyRepo.findAll();
        
        assertNotNull(allStories, "Should return a collection");
        assertEquals(0, allStories.size(), "Collection should be empty");
    }

    @Test
    @DisplayName("Should remove story successfully")
    void testRemoveStory() {
        storyRepo.persist(testStory);
        int id = testStory.getId();
        
        assertEquals(1, storyRepo.count(), "Should have 1 story before removal");
        
        storyRepo.remove(id);
        
        assertEquals(0, storyRepo.count(), "Should have 0 stories after removal");
        assertNull(storyRepo.find(id), "Removed story should not be found");
    }

    @Test
    @DisplayName("Should handle removal of non-existent story")
    void testRemoveNonExistentStory() {
        storyRepo.persist(testStory);
        
        storyRepo.remove(999L);
        
        assertEquals(1, storyRepo.count(), "Count should remain 1 after removing non-existent story");
    }

    @Test
    @DisplayName("Should persist story with assigned user")
    void testPersistStoryWithUser() {
        User user = new User(1, "John Doe", "john@example.com", "password", new ArrayList<>());
        testStory.setUserAssigned(user);
        
        storyRepo.persist(testStory);
        
        Story found = storyRepo.find(testStory.getId());
        assertNotNull(found.getUserAssigned(), "Story should have assigned user");
        assertEquals("John Doe", found.getUserAssigned().getName(), "User name should match");
    }

    @Test
    @DisplayName("Should persist story with all status values")
    void testPersistStoryWithDifferentStatuses() {
        Story backlogStory = new Story();
        backlogStory.setTitle("BACKLOG Story");
        backlogStory.setStatus(StoryStatus.BACKLOG);
        storyRepo.persist(backlogStory);
        
        Story inProgressStory = new Story();
        inProgressStory.setTitle("In Progress Story");
        inProgressStory.setStatus(StoryStatus.IN_PROGRESS);
        storyRepo.persist(inProgressStory);
        
        Story doneStory = new Story();
        doneStory.setTitle("Done Story");
        doneStory.setStatus(StoryStatus.DONE);
        storyRepo.persist(doneStory);
        
        assertEquals(3, storyRepo.count(), "Should have 3 stories with different statuses");
        assertEquals(StoryStatus.BACKLOG, storyRepo.find(backlogStory.getId()).getStatus());
        assertEquals(StoryStatus.IN_PROGRESS, storyRepo.find(inProgressStory.getId()).getStatus());
        assertEquals(StoryStatus.DONE, storyRepo.find(doneStory.getId()).getStatus());
    }

    @Test
    @DisplayName("Should persist story with dates")
    void testPersistStoryWithDates() {
        Date created = new Date();
        Date started = new Date();
        testStory.setDateCreated(created);
        testStory.setDateStart(started);
        
        storyRepo.persist(testStory);
        
        Story found = storyRepo.find(testStory.getId());
        assertNotNull(found.getDateCreated(), "Created date should be persisted");
        assertNotNull(found.getDateStart(), "Start date should be persisted");
    }

    @Test
    @DisplayName("Should update story through persist")
    void testUpdateStory() {
        storyRepo.persist(testStory);
        int id = testStory.getId();
        
        Story foundStory = storyRepo.find(id);
        foundStory.setTitle("Updated Title");
        foundStory.setStatus(StoryStatus.DONE);
        
        assertEquals("Updated Title", storyRepo.find(id).getTitle(), "Title should be updated");
        assertEquals(StoryStatus.DONE, storyRepo.find(id).getStatus(), "Status should be updated");
    }

    @Test
    @DisplayName("Should handle multiple persist and remove operations")
    void testMultiplePersistAndRemove() {
        Story story1 = new Story();
        story1.setTitle("Story 1");
        
        Story story2 = new Story();
        story2.setTitle("Story 2");
        
        Story story3 = new Story();
        story3.setTitle("Story 3");
        
        storyRepo.persist(story1);
        storyRepo.persist(story2);
        storyRepo.persist(story3);
        
        assertEquals(3, storyRepo.count());
        
        storyRepo.remove(story2.getId());
        
        assertEquals(2, storyRepo.count());
        assertNotNull(storyRepo.find(story1.getId()));
        assertNull(storyRepo.find(story2.getId()));
        assertNotNull(storyRepo.find(story3.getId()));
    }

    @Test
    @DisplayName("Should find stories by project ID")
    void testFindByProject() {
        Story story1 = new Story();
        story1.setTitle("Story 1");
        story1.setProjectId(1L);
        storyRepo.persist(story1);
        
        Story story2 = new Story();
        story2.setTitle("Story 2");
        story2.setProjectId(1L);
        storyRepo.persist(story2);
        
        Story story3 = new Story();
        story3.setTitle("Story 3");
        story3.setProjectId(2L);
        storyRepo.persist(story3);
        
        Collection<Story> storiesByProject = storyRepo.findByProject(1L);
        
        assertNotNull(storiesByProject, "Should return a collection");
        assertEquals(2, storiesByProject.size(), "Should find 2 stories for project 1");
    }

    @Test
    @DisplayName("Should return empty collection for findByProject when no stories match")
    void testFindByProjectEmpty() {
        Story story1 = new Story();
        story1.setTitle("Story 1");
        story1.setProjectId(1L);
        storyRepo.persist(story1);
        
        Collection<Story> storiesByProject = storyRepo.findByProject(999L);
        
        assertNotNull(storiesByProject, "Should return a collection");
        assertEquals(0, storiesByProject.size(), "Should be empty");
    }

    @Test
    @DisplayName("Should find stories by column ID")
    void testFindByColumn() {
        Story story1 = new Story();
        story1.setTitle("Story 1");
        story1.setColumnId(1L);
        story1.setPosition(1);
        storyRepo.persist(story1);
        
        Story story2 = new Story();
        story2.setTitle("Story 2");
        story2.setColumnId(1L);
        story2.setPosition(2);
        storyRepo.persist(story2);
        
        Story story3 = new Story();
        story3.setTitle("Story 3");
        story3.setColumnId(2L);
        story3.setPosition(1);
        storyRepo.persist(story3);
        
        Collection<Story> storiesByColumn = storyRepo.findByColumn(1L);
        
        assertNotNull(storiesByColumn, "Should return a collection");
        assertEquals(2, storiesByColumn.size(), "Should find 2 stories for column 1");
    }

    @Test
    @DisplayName("Should find stories with null column ID")
    void testFindByColumnNull() {
        Story story1 = new Story();
        story1.setTitle("Story 1");
        story1.setColumnId(null);
        story1.setPosition(1);
        storyRepo.persist(story1);
        
        Story story2 = new Story();
        story2.setTitle("Story 2");
        story2.setColumnId(1L);
        story2.setPosition(1);
        storyRepo.persist(story2);
        
        Collection<Story> storiesByColumn = storyRepo.findByColumn(null);
        
        assertNotNull(storiesByColumn, "Should return a collection");
        assertEquals(1, storiesByColumn.size(), "Should find 1 story with null column");
    }

    @Test
    @DisplayName("Should move story to column")
    void testMoveToColumn() {
        storyRepo.persist(testStory);
        long storyId = testStory.getId();
        
        storyRepo.moveToColumn(storyId, 5L);
        
        Story found = storyRepo.find(storyId);
        assertNotNull(found, "Story should exist");
        assertEquals(5L, found.getColumnId(), "Column ID should be updated");
    }

    @Test
    @DisplayName("Should move story to null column")
    void testMoveToColumnNull() {
        storyRepo.persist(testStory);
        testStory.setColumnId(1L);
        long storyId = testStory.getId();
        
        storyRepo.moveToColumn(storyId, null);
        
        Story found = storyRepo.find(storyId);
        assertNotNull(found, "Story should exist");
        assertNull(found.getColumnId(), "Column ID should be null");
    }

    @Test
    @DisplayName("Should handle moveToColumn for non-existent story")
    void testMoveToColumnNonExistent() {
        storyRepo.moveToColumn(999L, 1L);
        // Should not throw
    }

    @Test
    @DisplayName("Should update story status")
    void testUpdateStatus() {
        storyRepo.persist(testStory);
        long storyId = testStory.getId();
        
        storyRepo.updateStatus(storyId, StoryStatus.IN_PROGRESS);
        
        Story found = storyRepo.find(storyId);
        assertNotNull(found, "Story should exist");
        assertEquals(StoryStatus.IN_PROGRESS, found.getStatus(), "Status should be updated");
    }

    @Test
    @DisplayName("Should handle updateStatus for non-existent story")
    void testUpdateStatusNonExistent() {
        storyRepo.updateStatus(999L, StoryStatus.DONE);
        // Should not throw
    }

    @Test
    @DisplayName("Should add work log to story")
    void testAddWorkLog() {
        storyRepo.persist(testStory);
        long storyId = testStory.getId();
        
        WorkLog workLog = new WorkLog(1L, LocalDateTime.now(), 1L, storyId);
        workLog.setDuration(3600L);
        
        storyRepo.addWorkLog(storyId, workLog);
        
        Story found = storyRepo.find(storyId);
        assertNotNull(found, "Story should exist");
        assertNotNull(found.getWorkLogs(), "Work logs should be initialized");
        assertEquals(1, found.getWorkLogs().size(), "Should have 1 work log");
    }

    @Test
    @DisplayName("Should handle addWorkLog for non-existent story")
    void testAddWorkLogNonExistent() {
        WorkLog workLog = new WorkLog(1L, LocalDateTime.now(), 1L, 999L);
        storyRepo.addWorkLog(999L, workLog);
        // Should not throw
    }

    @Test
    @DisplayName("Should remove work log from story")
    void testRemoveWorkLog() {
        storyRepo.persist(testStory);
        long storyId = testStory.getId();
        
        WorkLog workLog1 = new WorkLog(1L, LocalDateTime.now(), 1L, storyId);
        WorkLog workLog2 = new WorkLog(2L, LocalDateTime.now(), 1L, storyId);
        
        storyRepo.addWorkLog(storyId, workLog1);
        storyRepo.addWorkLog(storyId, workLog2);
        
        storyRepo.removeWorkLog(storyId, 1L);
        
        Story found = storyRepo.find(storyId);
        assertNotNull(found, "Story should exist");
        assertEquals(1, found.getWorkLogs().size(), "Should have 1 work log remaining");
    }

    @Test
    @DisplayName("Should handle removeWorkLog for non-existent story")
    void testRemoveWorkLogNonExistent() {
        storyRepo.removeWorkLog(999L, 1L);
        // Should not throw
    }

    @Test
    @DisplayName("Should calculate total time from work logs")
    void testCalculateTotalTime() {
        storyRepo.persist(testStory);
        long storyId = testStory.getId();
        
        WorkLog workLog1 = new WorkLog(1L, LocalDateTime.now(), 1L, storyId);
        workLog1.setDuration(3600L);
        WorkLog workLog2 = new WorkLog(2L, LocalDateTime.now(), 1L, storyId);
        workLog2.setDuration(1800L);
        
        storyRepo.addWorkLog(storyId, workLog1);
        storyRepo.addWorkLog(storyId, workLog2);
        
        Long totalTime = storyRepo.calculateTotalTime(storyId);
        
        assertEquals(5400L, totalTime, "Total time should be sum of durations");
    }

    @Test
    @DisplayName("Should return zero for calculateTotalTime when story has no work logs")
    void testCalculateTotalTimeNoWorkLogs() {
        storyRepo.persist(testStory);
        long storyId = testStory.getId();
        
        Long totalTime = storyRepo.calculateTotalTime(storyId);
        
        assertEquals(0L, totalTime, "Total time should be 0");
    }

    @Test
    @DisplayName("Should return zero for calculateTotalTime for non-existent story")
    void testCalculateTotalTimeNonExistent() {
        Long totalTime = storyRepo.calculateTotalTime(999L);
        assertEquals(0L, totalTime, "Total time should be 0");
    }

    @Test
    @DisplayName("Should start timer and create work log")
    void testStartTimer() {
        storyRepo.persist(testStory);
        long storyId = testStory.getId();
        
        WorkLog workLog = storyRepo.startTimer(storyId, 1L);
        
        assertNotNull(workLog, "Work log should be created");
        Story found = storyRepo.find(storyId);
        assertNotNull(found.getWorkLogs(), "Work logs should be initialized");
        assertEquals(1, found.getWorkLogs().size(), "Should have 1 work log");
    }

    @Test
    @DisplayName("Should return null for startTimer for non-existent story")
    void testStartTimerNonExistent() {
        WorkLog workLog = storyRepo.startTimer(999L, 1L);
        assertNull(workLog, "Should return null for non-existent story");
    }

    @Test
    @DisplayName("Should stop timer and update work log")
    void testStopTimer() {
        storyRepo.persist(testStory);
        long storyId = testStory.getId();
        
        WorkLog workLog = storyRepo.startTimer(storyId, 1L);
        assertNotNull(workLog, "Work log should be created");
        
        WorkLog stopped = storyRepo.stopTimer(storyId, workLog.getId());
        
        assertNotNull(stopped, "Work log should be found");
    }

    @Test
    @DisplayName("Should return null for stopTimer for non-existent story")
    void testStopTimerNonExistent() {
        WorkLog workLog = storyRepo.stopTimer(999L, 1L);
        assertNull(workLog, "Should return null for non-existent story");
    }

    @Test
    @DisplayName("Should update existing story when persist is called with existing ID")
    void testPersistWithExistingId() {
        storyRepo.persist(testStory);
        long storyId = testStory.getId();
        
        Story updatedStory = new Story();
        updatedStory.setId((int) storyId);
        updatedStory.setTitle("Updated Title");
        
        storyRepo.persist(updatedStory);
        
        Story found = storyRepo.find(storyId);
        assertNotNull(found, "Story should exist");
        assertEquals("Updated Title", found.getTitle(), "Title should be updated");
        assertEquals(1, storyRepo.count(), "Should still have 1 story");
    }

    @Test
    @DisplayName("Should count stories correctly")
    void testCount() {
        assertEquals(0, storyRepo.count(), "Initial count should be 0");
        
        storyRepo.persist(testStory);
        assertEquals(1, storyRepo.count(), "Count should be 1 after adding story");
        
        Story story2 = new Story();
        story2.setTitle("Story 2");
        storyRepo.persist(story2);
        assertEquals(2, storyRepo.count(), "Count should be 2 after adding second story");
        
        storyRepo.remove(testStory.getId());
        assertEquals(1, storyRepo.count(), "Count should be 1 after removing story");
    }

    @Test
    @DisplayName("Should maintain story data integrity after persist")
    void testDataIntegrity() {
        String title = "Important Story";
        String description = "Critical bug fix";
        StoryStatus status = StoryStatus.IN_PROGRESS;
        User user = new User(1, "Jane", "jane@example.com", "pass", new ArrayList<>());
        Date created = new Date();
        
        testStory.setTitle(title);
        testStory.setDescription(description);
        testStory.setStatus(status);
        testStory.setUserAssigned(user);
        testStory.setDateCreated(created);
        
        storyRepo.persist(testStory);
        
        Story retrieved = storyRepo.find(testStory.getId());
        
        assertEquals(title, retrieved.getTitle());
        assertEquals(description, retrieved.getDescription());
        assertEquals(status, retrieved.getStatus());
        assertEquals(user, retrieved.getUserAssigned());
        assertEquals(created, retrieved.getDateCreated());
    }
}
