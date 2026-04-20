package fr.uha.ensisa.gl.tarnished.repos;

import fr.uha.ensisa.gl.entities.Column;
import fr.uha.ensisa.gl.entities.Project;
import fr.uha.ensisa.gl.tarnished.mems.ProjectRepoMem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ProjectRepoTest {
    private ProjectRepoMem repo;

    @BeforeEach
    void setup() {
        repo = new ProjectRepoMem();

        //Mock the ColumnRepo and inject it into ProjectRepoMem
        ColumnRepo columnRepo = Mockito.mock(ColumnRepo.class);
        repo.setColumnRepo(columnRepo);
    }

    @Test
    void testPersistAndFind() {
        Project p = new Project();
        p.setName("Test");

        repo.persist(p);

        Project found = repo.find(p.getId());

        assertNotNull(found);
        assertEquals("Test", found.getName());
    }

    @Test
    void testUpdate() {
        Project p = new Project();
        p.setName("Old");

        repo.persist(p);

        p.setName("New");
        repo.update(p);

        assertEquals("New", repo.find(p.getId()).getName());
    }

    @Test
    void testRemove() {
        Project p = new Project();
        p.setName("Test");
        repo.persist(p);

        long id = p.getId();
        repo.remove(id);

        assertNull(repo.find(id));
    }

    @Test
    void testFindAll() {
        Project p1 = new Project();
        p1.setName("Project 1");
        repo.persist(p1);

        Project p2 = new Project();
        p2.setName("Project 2");
        repo.persist(p2);

        assertEquals(2, repo.findAll().size());
    }

    @Test
    void testCount() {
        assertEquals(0, repo.count());

        Project p1 = new Project();
        p1.setName("Project 1");
        repo.persist(p1);

        assertEquals(1, repo.count());

        Project p2 = new Project();
        p2.setName("Project 2");
        repo.persist(p2);

        assertEquals(2, repo.count());
    }

    @Test
    void testCreateDefaultColumns() {
        //Get the mocked columnRepo from setup
        ColumnRepo columnRepo = Mockito.mock(ColumnRepo.class);
        repo.setColumnRepo(columnRepo);
        
        //Capture all columns persisted
        ArgumentCaptor<Column> columnCaptor = ArgumentCaptor.forClass(Column.class);
        
        Project p = new Project();
        p.setName("Test");
        repo.persist(p);

        //Verify persist was called 5 times for default columns
        Mockito.verify(columnRepo, Mockito.times(5)).persist(columnCaptor.capture());
        
        //Get all captured columns
        List<Column> columns = columnCaptor.getAllValues();
        assertEquals(5, columns.size());
        
        //Verify first column (BACKLOG)
        Column backlog = columns.get(0);
        assertEquals("BACKLOG", backlog.getName());
        assertEquals(1, backlog.getPosition());
        assertEquals(0, backlog.getMaxCapacity());
        assertEquals(p, backlog.getProject());
        assertFalse(backlog.isHasSubColumns());
        
        //Verify second column (IN PROGRESS)
        Column inProgress = columns.get(1);
        assertEquals("IN PROGRESS", inProgress.getName());
        assertEquals(2, inProgress.getPosition());
        assertEquals(0, inProgress.getMaxCapacity());
        assertEquals(p, inProgress.getProject());
        assertTrue(inProgress.isHasSubColumns());
        
        //Verify third column (REVIEW)
        Column review = columns.get(2);
        assertEquals("REVIEW", review.getName());
        assertEquals(3, review.getPosition());
        assertEquals(0, review.getMaxCapacity());
        assertEquals(p, review.getProject());
        assertTrue(review.isHasSubColumns());
        
        //Verify fourth column (DONE)
        Column done = columns.get(3);
        assertEquals("DONE", done.getName());
        assertEquals(4, done.getPosition());
        assertEquals(0, done.getMaxCapacity());
        assertEquals(p, done.getProject());
        assertFalse(done.isHasSubColumns());
        
        //Verify fifth column (BLOCKED)
        Column blocked = columns.get(4);
        assertEquals("BLOCKED", blocked.getName());
        assertEquals(5, blocked.getPosition());
        assertEquals(0, blocked.getMaxCapacity());
        assertEquals(p, blocked.getProject());
        assertFalse(blocked.isHasSubColumns());
    }
    
    @Test
    void testPersistSetsProjectId() {
        Project p = new Project();
        p.setName("Test Project");
        
        assertEquals(0, p.getId());
        repo.persist(p);
        
        //Verify ID was auto-incremented
        assertTrue(p.getId() > 0);
    }
    
    @Test
    void testPersistSetsDateStarted() {
        Project p = new Project();
        p.setName("Test Project");
        
        assertNull(p.getDateStarted());
        repo.persist(p);
        
        //Verify dateStarted was set
        assertNotNull(p.getDateStarted());
    }
    
    @Test
    void testMultipleProjectsGetIncrementingIds() {
        Project p1 = new Project();
        p1.setName("Project 1");
        repo.persist(p1);
        
        Project p2 = new Project();
        p2.setName("Project 2");
        repo.persist(p2);
        
        assertTrue(p2.getId() > p1.getId());
        assertEquals(p1.getId() + 1, p2.getId());
    }

    @Test
    void testDefaultColumnsHaveZeroMaxCapacity() {
        //Use a real ColumnRepo to verify actual capacity values
        ColumnRepo realColumnRepo = new fr.uha.ensisa.gl.tarnished.mems.ColumnRepoMem();
        repo.setColumnRepo(realColumnRepo);
        
        Project p = new Project();
        p.setName("Test Project");
        repo.persist(p);
        
        //Find all columns for this project
        java.util.Collection<Column> columns = realColumnRepo.findByProject((long) p.getId());
        
        assertEquals(5, columns.size(), "Should have 5 default columns");
        
        //Verify each column has maxCapacity of exactly 0
        for (Column column : columns) {
            assertEquals(0, column.getMaxCapacity(), 
                "Column " + column.getName() + " should have maxCapacity of exactly 0");
            assertNotEquals(1, column.getMaxCapacity(), 
                "Column " + column.getName() + " should not have maxCapacity of 1");
            assertNotEquals(-1, column.getMaxCapacity(), 
                "Column " + column.getName() + " should not have maxCapacity of -1");
        }
    }
}