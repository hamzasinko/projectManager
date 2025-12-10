package fr.uha.ensisa.gl.tarnished.mems;

import fr.uha.ensisa.gl.entities.Story;
import fr.uha.ensisa.gl.entities.StoryStatus;
import fr.uha.ensisa.gl.entities.User;
import fr.uha.ensisa.gl.entities.WorkLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

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
    @DisplayName("Should return empty collection for findByProject")
    void testFindByProject() {
        storyRepo.persist(testStory);
        
        Collection<Story> storiesByProject = storyRepo.findByProject(1L);
        
        assertNotNull(storiesByProject, "Should return a collection");
        assertEquals(0, storiesByProject.size(), "Should be empty (not implemented yet)");
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

    @Test
    @DisplayName("Should move story to column")
    void testMoveToColumn() {
        storyRepo.persist(testStory);
        storyRepo.moveToColumn((long)testStory.getId(), 5L);
        
        Story found = storyRepo.find(testStory.getId());
        assertEquals(5L, found.getColumnId());
    }

    @Test
    @DisplayName("Should update story status")
    void testUpdateStatus() {
        storyRepo.persist(testStory);
        storyRepo.updateStatus((long)testStory.getId(), StoryStatus.DONE);
        
        Story found = storyRepo.find(testStory.getId());
        assertEquals(StoryStatus.DONE, found.getStatus());
    }

    @Test
    @DisplayName("Should add worklog to story")
    void testAddWorkLog() {
        storyRepo.persist(testStory);
        fr.uha.ensisa.gl.entities.WorkLog workLog = new fr.uha.ensisa.gl.entities.WorkLog();
        workLog.setId(1L);
        workLog.setDuration(60L);
        
        storyRepo.addWorkLog((long)testStory.getId(), workLog);
        
        Story found = storyRepo.find(testStory.getId());
        assertNotNull(found.getWorkLogs());
        assertEquals(1, found.getWorkLogs().size());
    }

    @Test
    @DisplayName("Should remove worklog from story")
    void testRemoveWorkLog() {
        storyRepo.persist(testStory);
        fr.uha.ensisa.gl.entities.WorkLog workLog1 = new fr.uha.ensisa.gl.entities.WorkLog();
        workLog1.setId(1L);
        fr.uha.ensisa.gl.entities.WorkLog workLog2 = new fr.uha.ensisa.gl.entities.WorkLog();
        workLog2.setId(2L);
        
        storyRepo.addWorkLog((long)testStory.getId(), workLog1);
        storyRepo.addWorkLog((long)testStory.getId(), workLog2);
        storyRepo.removeWorkLog((long)testStory.getId(), 1L);
        
        Story found = storyRepo.find(testStory.getId());
        assertEquals(1, found.getWorkLogs().size());
    }

    @Test
    @DisplayName("Should calculate total time")
    void testCalculateTotalTime() {
        storyRepo.persist(testStory);
        fr.uha.ensisa.gl.entities.WorkLog workLog = new fr.uha.ensisa.gl.entities.WorkLog();
        workLog.setId(1L);
        workLog.setDuration(120L);
        
        storyRepo.addWorkLog((long)testStory.getId(), workLog);
        Long totalTime = storyRepo.calculateTotalTime((long)testStory.getId());
        
        assertEquals(120L, totalTime);
    }

    @Test
    @DisplayName("Should start timer for story")
    void testStartTimer() {
        storyRepo.persist(testStory);
        fr.uha.ensisa.gl.entities.WorkLog workLog = storyRepo.startTimer((long)testStory.getId(), 10L);
        
        assertNotNull(workLog);
        assertEquals(10L, workLog.getUserId());
        assertTrue(workLog.isRunning());
    }

    @Test
    @DisplayName("Should stop timer for story")
    void testStopTimer() {
        storyRepo.persist(testStory);
        WorkLog workLog = storyRepo.startTimer((long)testStory.getId(), 10L);
        long workLogId = workLog.getId();
        
        WorkLog stoppedLog = storyRepo.stopTimer((long)testStory.getId(), workLogId);
        
        assertNotNull(stoppedLog);
        assertFalse(stoppedLog.isRunning());
    }

    @Test
    @DisplayName("Should return null when starting timer for non-existent story")
    void testStartTimerNonExistentStory() {
        WorkLog workLog = storyRepo.startTimer(999L, 10L);
        assertNull(workLog);
    }

    @Test
    @DisplayName("Should return null when stopping timer for non-existent story")
    void testStopTimerNonExistentStory() {
        WorkLog workLog = storyRepo.stopTimer(999L, 1L);
        assertNull(workLog);
    }

    @Test
    @DisplayName("Should return null when stopping timer with null worklogs")
    void testStopTimerNullWorkLogs() {
        testStory.setWorkLogs(null);
        storyRepo.persist(testStory);
        WorkLog workLog = storyRepo.stopTimer((long)testStory.getId(), 1L);
        assertNull(workLog);
    }

    @Test
    @DisplayName("Should return null when stopping non-existent worklog")
    void testStopTimerNonExistentWorkLog() {
        storyRepo.persist(testStory);
        storyRepo.startTimer((long)testStory.getId(), 10L);
        WorkLog workLog = storyRepo.stopTimer((long)testStory.getId(), 999L);
        assertNull(workLog);
    }

    @Test
    @DisplayName("Should handle addWorkLog with null worklogs list")
    void testAddWorkLogWithNullList() {
        testStory.setWorkLogs(null);
        storyRepo.persist(testStory);
        
        WorkLog workLog = new WorkLog(1L, java.time.LocalDateTime.now(), 10L, (long)testStory.getId());
        storyRepo.addWorkLog((long)testStory.getId(), workLog);
        
        Story found = storyRepo.find((long)testStory.getId());
        assertNotNull(found.getWorkLogs());
        assertEquals(1, found.getWorkLogs().size());
    }

    @Test
    @DisplayName("Should handle addWorkLog for non-existent story")
    void testAddWorkLogNonExistentStory() {
        WorkLog workLog = new WorkLog(1L, java.time.LocalDateTime.now(), 10L, 999L);
        assertDoesNotThrow(() -> storyRepo.addWorkLog(999L, workLog));
    }

    @Test
    @DisplayName("Should handle removeWorkLog for non-existent story")
    void testRemoveWorkLogNonExistentStory() {
        assertDoesNotThrow(() -> storyRepo.removeWorkLog(999L, 1L));
    }

    @Test
    @DisplayName("Should handle removeWorkLog with null worklogs")
    void testRemoveWorkLogNullWorkLogs() {
        testStory.setWorkLogs(null);
        storyRepo.persist(testStory);
        assertDoesNotThrow(() -> storyRepo.removeWorkLog((long)testStory.getId(), 1L));
    }

    @Test
    @DisplayName("Should handle calculateTotalTime with null worklogs")
    void testCalculateTotalTimeNullWorkLogs() {
        testStory.setWorkLogs(null);
        storyRepo.persist(testStory);
        Long total = storyRepo.calculateTotalTime((long)testStory.getId());
        assertEquals(0L, total);
    }

    @Test
    @DisplayName("Should handle calculateTotalTime for non-existent story")
    void testCalculateTotalTimeNonExistentStory() {
        Long total = storyRepo.calculateTotalTime(999L);
        assertEquals(0L, total);
    }

    @Test
    @DisplayName("Should handle moveToColumn for non-existent story")
    void testMoveToColumnNonExistentStory() {
        assertDoesNotThrow(() -> storyRepo.moveToColumn(999L, 5L));
    }

    @Test
    @DisplayName("Should handle updateStatus for non-existent story")
    void testUpdateStatusNonExistentStory() {
        assertDoesNotThrow(() -> storyRepo.updateStatus(999L, StoryStatus.DONE));
    }

    @Test
    @DisplayName("Should find stories by column with null columnId")
    void testFindByColumnNullId() {
        Story story1 = new Story();
        story1.setTitle("Story Without Column");
        story1.setColumnId(null);
        story1.setPosition(1);
        
        Story story2 = new Story();
        story2.setTitle("Story With Column");
        story2.setColumnId(5L);
        story2.setPosition(2);
        
        storyRepo.persist(story1);
        storyRepo.persist(story2);
        
        Collection<Story> stories = storyRepo.findByColumn(null);
        assertEquals(1, stories.size());
        assertEquals("Story Without Column", stories.iterator().next().getTitle());
    }

    @Test
    @DisplayName("Should sort stories by position when finding by column")
    void testFindByColumnSorted() {
        Story story1 = new Story();
        story1.setTitle("Story 3");
        story1.setColumnId(1L);
        story1.setPosition(3);
        
        Story story2 = new Story();
        story2.setTitle("Story 1");
        story2.setColumnId(1L);
        story2.setPosition(1);
        
        Story story3 = new Story();
        story3.setTitle("Story 2");
        story3.setColumnId(1L);
        story3.setPosition(2);
        
        storyRepo.persist(story1);
        storyRepo.persist(story2);
        storyRepo.persist(story3);
        
        Collection<Story> stories = storyRepo.findByColumn(1L);
        List<Story> list = new ArrayList<>(stories);
        assertEquals(3, list.size());
        assertEquals("Story 1", list.get(0).getTitle());
        assertEquals("Story 2", list.get(1).getTitle());
        assertEquals("Story 3", list.get(2).getTitle());
    }

    @Test
    @DisplayName("Should find stories by project with null projectId in stories")
    void testFindByProjectWithNullProjectIds() {
        Story story1 = new Story();
        story1.setTitle("Story 1");
        story1.setProjectId(null);
        
        Story story2 = new Story();
        story2.setTitle("Story 2");
        story2.setProjectId(5L);
        
        storyRepo.persist(story1);
        storyRepo.persist(story2);
        
        Collection<Story> stories = storyRepo.findByProject(5L);
        assertEquals(1, stories.size());
        assertEquals("Story 2", stories.iterator().next().getTitle());
    }
}
