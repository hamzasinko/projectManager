package fr.uha.ensisa.gl.tarnished.mems;

import fr.uha.ensisa.gl.entities.Story;
import fr.uha.ensisa.gl.entities.StoryStatus;
import fr.uha.ensisa.gl.entities.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

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
}
