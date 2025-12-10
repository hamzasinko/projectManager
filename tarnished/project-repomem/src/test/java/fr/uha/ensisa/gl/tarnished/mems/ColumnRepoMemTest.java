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

        assertFalse(repo.isColumnFull(id));

        Story s1 = new Story();
        s1.setId(1);
        column.getStories().add(s1);
        assertFalse(repo.isColumnFull(id));

        Story s2 = new Story();
        s2.setId(2);
        column.getStories().add(s2);
        assertTrue(repo.isColumnFull(id));
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
    void testCanAcceptStory() {
        Column column = new Column();
        column.setMaxCapacity(2);
        column.setStories(new ArrayList<>());
        repo.persist(column);

        assertTrue(repo.canAcceptStory((long)column.getId()));

        column.getStories().add(new Story());
        column.getStories().add(new Story());

        assertFalse(repo.canAcceptStory((long)column.getId()));
    }

    @Test
    void testCanAcceptStoryUnlimited() {
        Column column = new Column();
        column.setMaxCapacity(0);
        column.setStories(new ArrayList<>());
        repo.persist(column);

        assertTrue(repo.canAcceptStory((long)column.getId()));

        column.getStories().add(new Story());
        column.getStories().add(new Story());
        column.getStories().add(new Story());

        assertTrue(repo.canAcceptStory((long)column.getId()));
    }

    @Test
    void testAddStoryToColumn() {
        Column column = new Column();
        column.setMaxCapacity(5);
        column.setStories(new ArrayList<>());
        repo.persist(column);

        Story story = new Story();
        story.setId(100);

        Mockito.when(storyRepo.find(100L)).thenReturn(story);

        repo.addStoryToColumn(100L, (long)column.getId());

        Mockito.verify(storyRepo).moveToColumn(100L, (long)column.getId());
    }

    @Test
    void testAddStoryToFullColumn() {
        Column column = new Column();
        column.setMaxCapacity(1);
        column.setStories(new ArrayList<>());
        column.getStories().add(new Story());
        repo.persist(column);

        assertThrows(IllegalStateException.class, () -> repo.addStoryToColumn(100L, (long)column.getId()));
    }

    @Test
    void testRemoveStoryFromColumn() {
        Column column = new Column();
        repo.persist(column);

        repo.removeStoryFromColumn(100L, (long)column.getId());

        Mockito.verify(storyRepo).moveToColumn(100L, null);
    }

    @Test
    void testReorderNonExistentColumn() {
        repo.reorder(999L, 5);
        assertNull(repo.find(999L));
    }

    @Test
    void testIsColumnFullWithNullStories() {
        Column column = new Column();
        column.setMaxCapacity(2);
        column.setStories(null);
        repo.persist(column);

        assertFalse(repo.isColumnFull((long)column.getId()));
    }

    @Test
    void testIsColumnFullNonExistentColumn() {
        assertFalse(repo.isColumnFull(999L));
    }

    @Test
    void testIsColumnFullUnlimitedCapacity() {
        Column column = new Column();
        column.setMaxCapacity(0);
        column.setStories(new ArrayList<>());
        column.getStories().add(new Story());
        column.getStories().add(new Story());
        column.getStories().add(new Story());
        repo.persist(column);

        assertFalse(repo.isColumnFull((long)column.getId()));
    }

    @Test
    void testMoveStoryToNullColumn() {
        Column col1 = new Column();
        col1.setId(1);
        repo.persist(col1);

        repo.moveStoryBetweenColumns(10L, 1L, null);

        Mockito.verify(storyRepo).moveToColumn(10L, null);
    }

    @Test
    void testMoveStoryWithoutStoryRepo() {
        repo.setStoryRepo(null);
        Column col1 = new Column();
        col1.setId(1);
        repo.persist(col1);

        assertDoesNotThrow(() -> repo.moveStoryBetweenColumns(10L, 1L, null));
    }

    @Test
    void testAddStoryWithoutStoryRepo() {
        repo.setStoryRepo(null);
        Column column = new Column();
        column.setMaxCapacity(5);
        repo.persist(column);

        assertDoesNotThrow(() -> repo.addStoryToColumn(100L, (long)column.getId()));
    }

    @Test
    void testRemoveStoryWithoutStoryRepo() {
        repo.setStoryRepo(null);
        Column column = new Column();
        repo.persist(column);

        assertDoesNotThrow(() -> repo.removeStoryFromColumn(100L, (long)column.getId()));
    }

    @Test
    void testFindByProjectSorted() {
        Project project = new Project();
        project.setId(1);

        Column col3 = new Column();
        col3.setName("Third");
        col3.setPosition(3);
        col3.setProject(project);
        repo.persist(col3);

        Column col1 = new Column();
        col1.setName("First");
        col1.setPosition(1);
        col1.setProject(project);
        repo.persist(col1);

        Column col2 = new Column();
        col2.setName("Second");
        col2.setPosition(2);
        col2.setProject(project);
        repo.persist(col2);

        Collection<Column> columns = repo.findByProject(1L);
        List<Column> list = new ArrayList<>(columns);

        assertEquals(3, list.size());
        assertEquals(1, list.get(0).getPosition());
        assertEquals(2, list.get(1).getPosition());
        assertEquals(3, list.get(2).getPosition());
    }

    @Test
    void testFindByProjectWithNullProject() {
        Column col = new Column();
        col.setName("Orphan");
        col.setPosition(1);
        col.setProject(null);
        repo.persist(col);

        Collection<Column> columns = repo.findByProject(1L);
        assertTrue(columns.isEmpty());
    }

    @Test
    void testPersistWithExistingId() {
        Column column = new Column();
        column.setId(999);
        column.setName("Pre-assigned ID");

        repo.persist(column);

        assertEquals(999, column.getId());
        assertNotNull(repo.find(999L));
    }
}