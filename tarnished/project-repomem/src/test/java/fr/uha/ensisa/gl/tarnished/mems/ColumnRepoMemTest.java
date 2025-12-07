package fr.uha.ensisa.gl.tarnished.mems;

import fr.uha.ensisa.gl.entities.Column;
import fr.uha.ensisa.gl.entities.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class ColumnRepoMemTest {

    private ColumnRepoMem repo;

    @BeforeEach
    void setUp() {
        repo = new ColumnRepoMem();
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
        
        repo.addStoryToColumn(1L, id);
        assertFalse(repo.isColumnFull(id));
        
        repo.addStoryToColumn(2L, id);
        assertTrue(repo.isColumnFull(id));
    }

    @Test
    void testMoveStoryBetweenColumnsSuccess() {
        Column columnFrom = new Column();
        columnFrom.setName("To Do");
        columnFrom.setPosition(1);
        columnFrom.setMaxCapacity(5);
        columnFrom.setStories(new ArrayList<>());
        repo.persist(columnFrom);
        
        Column columnTo = new Column();
        columnTo.setName("In Progress");
        columnTo.setPosition(2);
        columnTo.setMaxCapacity(3);
        columnTo.setStories(new ArrayList<>());
        repo.persist(columnTo);
        
        long fromId = columnFrom.getId();
        long toId = columnTo.getId();
        
        repo.addStoryToColumn(1L, fromId);
        assertEquals(1, repo.find(fromId).getStories().size());
        
        repo.moveStoryBetweenColumns(1L, fromId, toId);
        
        assertEquals(0, repo.find(fromId).getStories().size());
        assertEquals(1, repo.find(toId).getStories().size());
    }

    @Test
    void testMoveStoryBetweenColumnsColumnFull() {
        Column columnFrom = new Column();
        columnFrom.setName("To Do");
        columnFrom.setPosition(1);
        columnFrom.setMaxCapacity(5);
        columnFrom.setStories(new ArrayList<>());
        repo.persist(columnFrom);
        
        Column columnTo = new Column();
        columnTo.setName("Done");
        columnTo.setPosition(3);
        columnTo.setMaxCapacity(1);
        columnTo.setStories(new ArrayList<>());
        repo.persist(columnTo);
        
        long fromId = columnFrom.getId();
        long toId = columnTo.getId();
        
        repo.addStoryToColumn(1L, fromId);
        repo.addStoryToColumn(2L, toId);
        
        assertThrows(IllegalStateException.class, () -> {
            repo.moveStoryBetweenColumns(1L, fromId, toId);
        });
    }
}
