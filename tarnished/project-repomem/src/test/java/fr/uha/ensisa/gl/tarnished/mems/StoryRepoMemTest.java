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
        
        Story story3 = new Story();
        story3.setTitle("Story 3");
        
        storyRepo.persist(story1);
        long id1 = story1.getId();
        
        storyRepo.persist(story2);
        long id2 = story2.getId();
        
        storyRepo.persist(story3);
        long id3 = story3.getId();
        
        //Verify exact incrementation
        assertEquals(id1 + 1, id2, "Second ID must be exactly first ID + 1");
        assertEquals(id2 + 1, id3, "Third ID must be exactly second ID + 1");
        
        //Also verify they're sequential starting from 1
        assertTrue(id1 > 0, "First ID should be positive");
        assertTrue(id2 > id1, "IDs should increase");
        assertTrue(id3 > id2, "IDs should keep increasing");
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
        //Should not throw
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
        //Should not throw
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
        //Should not throw
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
        //Should not throw
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
    @DisplayName("Should assign incrementing IDs to work logs")
    void testWorkLogIdIncrementation() {
        //Create story
        storyRepo.persist(testStory);
        long storyId = testStory.getId();
        
        //Start multiple timers to create work logs
        WorkLog wl1 = storyRepo.startTimer(storyId, 1L);
        WorkLog wl2 = storyRepo.startTimer(storyId, 1L);
        WorkLog wl3 = storyRepo.startTimer(storyId, 1L);
        
        assertNotNull(wl1, "First work log should be created");
        assertNotNull(wl2, "Second work log should be created");
        assertNotNull(wl3, "Third work log should be created");
        
        //Verify exact incrementation
        assertEquals(wl1.getId() + 1, wl2.getId(), "Second WorkLog ID must be exactly first + 1");
        assertEquals(wl2.getId() + 1, wl3.getId(), "Third WorkLog ID must be exactly second + 1");
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

    @Test
    @DisplayName("Should sort stories by position when finding by column")
    void testFindByColumnSorting() {
        Story story1 = new Story();
        story1.setTitle("Story 1");
        story1.setColumnId(1L);
        story1.setPosition(3);
        storyRepo.persist(story1);
        
        Story story2 = new Story();
        story2.setTitle("Story 2");
        story2.setColumnId(1L);
        story2.setPosition(1);
        storyRepo.persist(story2);
        
        Story story3 = new Story();
        story3.setTitle("Story 3");
        story3.setColumnId(1L);
        story3.setPosition(2);
        storyRepo.persist(story3);
        
        Collection<Story> stories = storyRepo.findByColumn(1L);
        Story[] storiesArray = stories.toArray(new Story[0]);
        
        assertEquals(3, storiesArray.length, "Should have 3 stories");
        assertEquals(1, storiesArray[0].getPosition(), "First story should have position 1");
        assertEquals(2, storiesArray[1].getPosition(), "Second story should have position 2");
        assertEquals(3, storiesArray[2].getPosition(), "Third story should have position 3");
    }

    @Test
    @DisplayName("Should sort stories with null column by position")
    void testFindByNullColumnSorting() {
        Story story1 = new Story();
        story1.setTitle("Story 1");
        story1.setColumnId(null);
        story1.setPosition(5);
        storyRepo.persist(story1);
        
        Story story2 = new Story();
        story2.setTitle("Story 2");
        story2.setColumnId(null);
        story2.setPosition(2);
        storyRepo.persist(story2);
        
        Story story3 = new Story();
        story3.setTitle("Story 3");
        story3.setColumnId(null);
        story3.setPosition(3);
        storyRepo.persist(story3);
        
        Collection<Story> stories = storyRepo.findByColumn(null);
        Story[] storiesArray = stories.toArray(new Story[0]);
        
        assertEquals(3, storiesArray.length, "Should have 3 stories");
        assertEquals(2, storiesArray[0].getPosition(), "First story should have position 2");
        assertEquals(3, storiesArray[1].getPosition(), "Second story should have position 3");
        assertEquals(5, storiesArray[2].getPosition(), "Third story should have position 5");
    }

    @Test
    @DisplayName("Should initialize work logs list when adding to story without work logs")
    void testAddWorkLogInitializesList() {
        //Create a fresh story without using testStory which might have been modified
        Story freshStory = new Story();
        freshStory.setTitle("Fresh Story");
        freshStory.setDescription("No work logs yet");
        storyRepo.persist(freshStory);
        long storyId = freshStory.getId();
        
        //Ensure story has no work logs initially by checking directly after persist
        //Note: Story entity might initialize workLogs to empty list in constructor
        Story story = storyRepo.find(storyId);
        boolean initiallyEmpty = story.getWorkLogs() == null || story.getWorkLogs().isEmpty();
        assertTrue(initiallyEmpty, "Work logs should be null or empty initially");
        
        WorkLog workLog = new WorkLog(1L, LocalDateTime.now(), 1L, storyId);
        workLog.setDuration(3600L);
        
        storyRepo.addWorkLog(storyId, workLog);
        
        Story found = storyRepo.find(storyId);
        assertNotNull(found.getWorkLogs(), "Work logs should be initialized");
        assertEquals(1, found.getWorkLogs().size(), "Should have 1 work log");
        assertEquals(workLog, found.getWorkLogs().get(0), "Work log should match");
    }

    @Test
    @DisplayName("Should remove specific work log by ID")
    void testRemoveWorkLogById() {
        storyRepo.persist(testStory);
        long storyId = testStory.getId();
        
        WorkLog workLog1 = new WorkLog(10L, LocalDateTime.now(), 1L, storyId);
        WorkLog workLog2 = new WorkLog(20L, LocalDateTime.now(), 1L, storyId);
        WorkLog workLog3 = new WorkLog(30L, LocalDateTime.now(), 1L, storyId);
        
        storyRepo.addWorkLog(storyId, workLog1);
        storyRepo.addWorkLog(storyId, workLog2);
        storyRepo.addWorkLog(storyId, workLog3);
        
        assertEquals(3, storyRepo.find(storyId).getWorkLogs().size(), "Should have 3 work logs");
        
        storyRepo.removeWorkLog(storyId, 20L);
        
        Story found = storyRepo.find(storyId);
        assertEquals(2, found.getWorkLogs().size(), "Should have 2 work logs remaining");
        assertTrue(found.getWorkLogs().stream().anyMatch(wl -> wl.getId() == 10L), "Should contain work log 10");
        assertTrue(found.getWorkLogs().stream().anyMatch(wl -> wl.getId() == 30L), "Should contain work log 30");
        assertFalse(found.getWorkLogs().stream().anyMatch(wl -> wl.getId() == 20L), "Should not contain work log 20");
    }

    @Test
    @DisplayName("Should find and stop specific work log by ID")
    void testStopTimerFindsCorrectWorkLog() {
        storyRepo.persist(testStory);
        long storyId = testStory.getId();
        
        WorkLog workLog1 = storyRepo.startTimer(storyId, 1L);
        WorkLog workLog2 = storyRepo.startTimer(storyId, 1L);
        WorkLog workLog3 = storyRepo.startTimer(storyId, 1L);
        
        assertNotNull(workLog1, "First work log should be created");
        assertNotNull(workLog2, "Second work log should be created");
        assertNotNull(workLog3, "Third work log should be created");
        
        //Stop the second work log
        WorkLog stopped = storyRepo.stopTimer(storyId, workLog2.getId());
        
        assertNotNull(stopped, "Should find and return the work log");
        assertEquals(workLog2.getId(), stopped.getId(), "Should stop the correct work log");
    }

    @Test
    @DisplayName("Should call stopTimer on work log when stopping")
    void testStopTimerCallsStopOnWorkLog() {
        storyRepo.persist(testStory);
        long storyId = testStory.getId();
        
        WorkLog workLog = storyRepo.startTimer(storyId, 1L);
        assertNotNull(workLog, "Work log should be created");
        assertNull(workLog.getEnd(), "End time should be null initially");
        
        //Wait a tiny bit to ensure duration is non-zero
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            //Ignore
        }
        
        WorkLog stopped = storyRepo.stopTimer(storyId, workLog.getId());
        
        assertNotNull(stopped, "Work log should be found");
        assertNotNull(stopped.getEnd(), "End time should be set after stopping");
        assertTrue(stopped.getDuration() >= 0, "Duration should be calculated");
    }

    @Test
    @DisplayName("Should return null when stopping timer for non-existent work log")
    void testStopTimerNonExistentWorkLog() {
        storyRepo.persist(testStory);
        long storyId = testStory.getId();
        
        WorkLog workLog = storyRepo.startTimer(storyId, 1L);
        assertNotNull(workLog, "Work log should be created");
        
        WorkLog stopped = storyRepo.stopTimer(storyId, 999L);
        
        assertNull(stopped, "Should return null for non-existent work log ID");
    }
    @Test
    @DisplayName("Should safely handle null work log")
    void testAddWorkLogNullSafe() {
        storyRepo.persist(testStory);
        long storyId = testStory.getId();

        assertDoesNotThrow(() -> storyRepo.addWorkLog(storyId, null));

        Story found = storyRepo.find(storyId);

        assertNotNull(found.getWorkLogs());
        assertEquals(1, found.getWorkLogs().size());
        assertNull(found.getWorkLogs().get(0));
    }
}
