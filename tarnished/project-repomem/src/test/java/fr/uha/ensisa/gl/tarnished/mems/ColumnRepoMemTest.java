package fr.uha.ensisa.gl.tarnished.mems;

import fr.uha.ensisa.gl.entities.Column;
import fr.uha.ensisa.gl.entities.Project;
import fr.uha.ensisa.gl.entities.Story;
import fr.uha.ensisa.gl.tarnished.repos.StoryRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ColumnRepoMemTest {

    private ColumnRepoMem repo;
    private StoryRepo storyRepo;

    @BeforeEach
    void setUp() {
        repo = new ColumnRepoMem(); // Use real instance
        storyRepo = Mockito.mock(StoryRepo.class);
        repo.setStoryRepo(storyRepo);
    }

    @Test
    void testPersistAndFind() {
        Column column = new Column();
        column.setName("To Do");
        column.setPosition(1);
        column.setMaxCapacity(5);

        repo.persist(column);

        assertNotEquals(0, column.getId());
        Column found = repo.find((long) column.getId());
        assertNotNull(found);
        assertEquals("To Do", found.getName());
    }
    
    @Test
    void testPersistAssignsIncrementingIds() {
        Column col1 = new Column();
        col1.setName("Column 1");
        repo.persist(col1);
        long id1 = col1.getId();
        
        Column col2 = new Column();
        col2.setName("Column 2");
        repo.persist(col2);
        long id2 = col2.getId();
        
        Column col3 = new Column();
        col3.setName("Column 3");
        repo.persist(col3);
        long id3 = col3.getId();
        
        //IDs must increment by exactly 1
        assertEquals(id1 + 1, id2, "Second ID should be first ID + 1");
        assertEquals(id2 + 1, id3, "Third ID should be second ID + 1");
    }

    @Test
    void testFindAll() {
        Column column1 = new Column();
        column1.setName("To Do");
        column1.setPosition(1);
        column1.setMaxCapacity(5);
        repo.persist(column1);

        Column column2 = new Column();
        column2.setName("In Progress");
        column2.setPosition(2);
        column2.setMaxCapacity(3);
        repo.persist(column2);

        Collection<Column> all = repo.findAll();
        assertEquals(2, all.size());
    }

    @Test
    void testRemove() {
        Column column = new Column();
        column.setName("Done");
        column.setPosition(3);
        column.setMaxCapacity(10);
        repo.persist(column);

        long id = column.getId();
        repo.remove(id);

        assertNull(repo.find(id));
    }

    @Test
    void testFindByProject() {
        Project project1 = new Project();
        project1.setId(1);

        Project project2 = new Project();
        project2.setId(2);

        Column column1 = new Column();
        column1.setName("To Do");
        column1.setPosition(1);
        column1.setMaxCapacity(5);
        column1.setProject(project1);
        repo.persist(column1);

        Column column2 = new Column();
        column2.setName("In Progress");
        column2.setPosition(2);
        column2.setMaxCapacity(3);
        column2.setProject(project1);
        repo.persist(column2);

        Column column3 = new Column();
        column3.setName("Done");
        column3.setPosition(1);
        column3.setMaxCapacity(10);
        column3.setProject(project2);
        repo.persist(column3);

        Collection<Column> project1Columns = repo.findByProject(1L);
        assertEquals(2, project1Columns.size());
    }

    @Test
    void testReorder() {
        Column column = new Column();
        column.setName("To Do");
        column.setPosition(1);
        column.setMaxCapacity(5);
        repo.persist(column);

        long id = column.getId();
        repo.reorder(id, 3);

        Column updated = repo.find(id);
        assertEquals(3, updated.getPosition());
    }

    @Test
    void testIsColumnFull() {
        Column column = new Column();
        column.setName("Done");
        column.setPosition(3);
        column.setMaxCapacity(2);
        column.setStories(new ArrayList<>());
        repo.persist(column);

        long id = column.getId();

        //Empty column is not full
        assertFalse(repo.isColumnFull(id));

        //One story, not full yet
        Story s1 = new Story();
        s1.setId(1);
        column.getStories().add(s1);
        assertFalse(repo.isColumnFull(id));

        //Exactly at capacity, should be full
        Story s2 = new Story();
        s2.setId(2);
        column.getStories().add(s2);
        assertTrue(repo.isColumnFull(id));
        
        //More than capacity, still full
        Story s3 = new Story();
        s3.setId(3);
        column.getStories().add(s3);
        assertTrue(repo.isColumnFull(id));
    }
    
    @Test
    void testIsColumnFullWithNoLimit() {
        Column column = new Column();
        column.setMaxCapacity(0); // No limit
        column.setStories(new ArrayList<>());
        repo.persist(column);
        
        long id = column.getId();
        
        //With maxCapacity = 0, column is never full
        assertFalse(repo.isColumnFull(id));
        
        for (int i = 0; i < 100; i++) {
            Story s = new Story();
            s.setId(i);
            column.getStories().add(s);
        }
        
        //Still not full even with 100 stories
        assertFalse(repo.isColumnFull(id));
    }
    
    @Test
    void testIsColumnFullWithNullColumn() {
        //Non-existent column should not be full
        assertFalse(repo.isColumnFull(999L));
    }
    
    @Test
    void testIsColumnFullWithNullStories() {
        Column column = new Column();
        column.setMaxCapacity(5);
        column.setStories(null); // Null stories list
        repo.persist(column);
        
        long id = column.getId();
        
        //Null stories is treated as empty, so not full
        assertFalse(repo.isColumnFull(id));
    }

    @Test
    void testCanAcceptStory() {
        Column column = new Column();
        column.setMaxCapacity(1);
        column.setStories(new ArrayList<>());
        repo.persist(column);
        
        long id = column.getId();
        
        //Empty column can accept story
        assertTrue(repo.canAcceptStory(id));
        
        //Add one story
        Story s1 = new Story();
        s1.setId(1);
        column.getStories().add(s1);
        
        //Full column cannot accept story
        assertFalse(repo.canAcceptStory(id));
    }
    
    @Test
    void testCanAcceptStoryWithNoLimit() {
        Column column = new Column();
        column.setMaxCapacity(0); // No limit
        column.setStories(new ArrayList<>());
        repo.persist(column);
        
        long id = column.getId();
        
        //Can always accept story when no limit
        assertTrue(repo.canAcceptStory(id));
    }
    
    @Test
    void testMoveStoryBetweenColumnsSuccess() {
        Column col1 = new Column();
        col1.setId(1);
        col1.setMaxCapacity(5);
        repo.persist(col1);

        Column col2 = new Column();
        col2.setId(2);
        col2.setMaxCapacity(5);
        repo.persist(col2);

        Story s = new Story();
        s.setId(10);

        Mockito.when(storyRepo.find(10L)).thenReturn(s);
        Mockito.when(storyRepo.findByColumn(2L)).thenReturn(new ArrayList<>());

        repo.moveStoryBetweenColumns(10L, 1L, 2L);

        Mockito.verify(storyRepo).moveToColumn(10L, 2L);
    }

    @Test
    void testMoveStoryBetweenColumnsColumnFull() {
        Column columnFrom = new Column();
        columnFrom.setId(1);
        columnFrom.setMaxCapacity(5);
        repo.persist(columnFrom);

        Column columnTo = new Column();
        columnTo.setId(2);
        columnTo.setMaxCapacity(1);
        repo.persist(columnTo);

        Story s1 = new Story();
        s1.setId(1);

        Story s2 = new Story();
        s2.setId(2);

        Mockito.when(storyRepo.find(1L)).thenReturn(s1);
        Mockito.when(storyRepo.findByColumn(2L)).thenReturn(List.of(s2));

        assertThrows(IllegalStateException.class, () -> repo.moveStoryBetweenColumns(1L, 1L, 2L));
    }

    @Test
    void testAddStoryToColumn() {
        Column column = new Column();
        column.setId(1);
        column.setMaxCapacity(5);
        column.setStories(new ArrayList<>());
        repo.persist(column);

        Story story = new Story();
        story.setId(10);
        Mockito.when(storyRepo.find(10L)).thenReturn(story);

        repo.addStoryToColumn(10L, 1L);

        Mockito.verify(storyRepo).moveToColumn(10L, 1L);
    }

    @Test
    void testAddStoryToColumnFull() {
        Column column = new Column();
        column.setId(1);
        column.setMaxCapacity(1);
        column.setStories(new ArrayList<>());
        Story existingStory = new Story();
        existingStory.setId(5);
        column.getStories().add(existingStory);
        repo.persist(column);

        assertThrows(IllegalStateException.class, () -> repo.addStoryToColumn(10L, 1L));
    }

    @Test
    void testAddStoryToColumnNullColumn() {
        repo.addStoryToColumn(10L, 999L); // Non-existent column
        //Should not throw, just do nothing
    }

    @Test
    void testRemoveStoryFromColumn() {
        repo.removeStoryFromColumn(10L, 1L);
        Mockito.verify(storyRepo).moveToColumn(10L, null);
    }

    @Test
    void testMoveStoryBetweenColumnsNullTarget() {
        repo.moveStoryBetweenColumns(10L, 1L, null);
        Mockito.verify(storyRepo).moveToColumn(10L, null);
    }

    @Test
    void testMoveStoryBetweenColumnsNullStoryRepo() {
        repo.setStoryRepo(null);
        Column col1 = new Column();
        col1.setId(1);
        repo.persist(col1);

        Column col2 = new Column();
        col2.setId(2);
        repo.persist(col2);

        //Should not throw when storyRepo is null
        repo.moveStoryBetweenColumns(10L, 1L, 2L);
    }

    @Test
    void testIsColumnFullNullColumn() {
        assertFalse(repo.isColumnFull(999L));
    }

    @Test
    void testIsColumnFullNullStories() {
        Column column = new Column();
        column.setName("Done");
        column.setPosition(3);
        column.setMaxCapacity(2);
        column.setStories(null);
        repo.persist(column);

        assertFalse(repo.isColumnFull((long) column.getId()));
    }

    @Test
    void testMoveStoryBetweenColumnsWithMaxCapacityZero() {
        //Test boundary condition: maxCapacity = 0 (unlimited)
        Column columnFrom = new Column();
        columnFrom.setId(1);
        columnFrom.setMaxCapacity(5);
        repo.persist(columnFrom);

        Column columnTo = new Column();
        columnTo.setId(2);
        columnTo.setMaxCapacity(0); // Unlimited capacity
        repo.persist(columnTo);

        Story story = new Story();
        story.setId(10);

        //Mock story and column stories
        List<Story> existingStories = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Story s = new Story();
            s.setId(i);
            existingStories.add(s);
        }

        Mockito.when(storyRepo.find(10L)).thenReturn(story);
        Mockito.when(storyRepo.findByColumn(2L)).thenReturn(existingStories);

        //Should succeed even with 100 stories because maxCapacity is 0 (unlimited)
        repo.moveStoryBetweenColumns(10L, 1L, 2L);

        Mockito.verify(storyRepo).moveToColumn(10L, 2L);
    }

    @Test
    void testMoveStoryBetweenColumnsWithMaxCapacityOne() {
        //Test boundary condition: maxCapacity = 1 (exactly at boundary)
        Column columnFrom = new Column();
        columnFrom.setId(1);
        columnFrom.setMaxCapacity(5);
        repo.persist(columnFrom);

        Column columnTo = new Column();
        columnTo.setId(2);
        columnTo.setMaxCapacity(1); // Exactly 1
        repo.persist(columnTo);

        Story story = new Story();
        story.setId(10);

        //Empty target column
        Mockito.when(storyRepo.find(10L)).thenReturn(story);
        Mockito.when(storyRepo.findByColumn(2L)).thenReturn(new ArrayList<>());

        //Should succeed because target is empty and capacity is 1
        repo.moveStoryBetweenColumns(10L, 1L, 2L);

        Mockito.verify(storyRepo).moveToColumn(10L, 2L);
    }

    @Test
    void find_onUnknownId_returnsNull() {
        ColumnRepoMem repo = new ColumnRepoMem();
        Column result = repo.find(999L);
        assertNull(result);
    }

    @Test
    void remove_onUnknownId_doesNotThrow() {
        ColumnRepoMem repo = new ColumnRepoMem();
        assertDoesNotThrow(() -> repo.remove(999L));
    }

    @Test
    void persist_thenFindAll_containsColumn() {
        ColumnRepoMem repo = new ColumnRepoMem();
        Column c = new Column();
        c.setId(1);

        repo.persist(c);

        assertEquals(1, repo.findAll().size());
        assertTrue(repo.findAll().contains(c));
    }

    @Test
    void findAll_reflectsPersistAndRemoveOperations() {
        ColumnRepoMem repo = new ColumnRepoMem();

        assertEquals(0, repo.findAll().size());

        Column c1 = new Column();
        c1.setId(1);
        repo.persist(c1);
        assertEquals(1, repo.findAll().size());

        Column c2 = new Column();
        c2.setId(2);
        repo.persist(c2);
        assertEquals(2, repo.findAll().size());

        repo.remove(1L);
        assertEquals(1, repo.findAll().size());
    }

}