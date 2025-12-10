package fr.uha.ensisa.gl.tarnished.repos;

import fr.uha.ensisa.gl.entities.Column;
import fr.uha.ensisa.gl.entities.Project;
import fr.uha.ensisa.gl.tarnished.mems.ProjectRepoMem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

public class ProjectRepoTest {
    private ProjectRepoMem repo;

    @BeforeEach
    void setup() {
        repo = new ProjectRepoMem();

        // Mock the ColumnRepo and inject it into ProjectRepoMem
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
        // Get the mocked columnRepo from setup
        ColumnRepo columnRepo = Mockito.mock(ColumnRepo.class);
        repo.setColumnRepo(columnRepo);
        
        Project p = new Project();
        p.setName("Test");
        repo.persist(p);

        // Verify persist was called 5 times for default columns (BACKLOG, IN PROGRESS, REVIEW, DONE, BLOCKED)
        Mockito.verify(columnRepo, Mockito.times(5)).persist(Mockito.any(Column.class));
    }
}