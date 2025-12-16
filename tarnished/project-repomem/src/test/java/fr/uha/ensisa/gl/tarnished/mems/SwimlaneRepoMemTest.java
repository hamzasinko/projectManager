package fr.uha.ensisa.gl.tarnished.mems;

import fr.uha.ensisa.gl.entities.Swimlane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SwimlaneRepoMemTest {

    private SwimlaneRepoMem repo;

    @BeforeEach
    void setup() {
        repo = Mockito.spy(new SwimlaneRepoMem());
    }

    @Test
    void testPersistNewSwimlaneAssignsId() {
        Swimlane swimlane = new Swimlane(0, "Lane 1", 1);
        repo.persist(swimlane);

        assertTrue(swimlane.getId() > 0, "New swimlane should have an id assigned");
        assertEquals(swimlane, repo.find(swimlane.getId()));
    }

    @Test
    void testPersistExistingSwimlaneUpdates() {
        Swimlane swimlane = new Swimlane(0, "Lane 1", 1);
        repo.persist(swimlane);

        long id = swimlane.getId();
        swimlane.setName("Updated Lane");
        repo.persist(swimlane);

        Swimlane found = repo.find(id);
        assertEquals("Updated Lane", found.getName());
    }

    @Test
    void testRemoveSwimlane() {
        Swimlane swimlane = new Swimlane(0, "Lane 1", 1);
        repo.persist(swimlane);
        long id = swimlane.getId();

        repo.remove(id);
        assertNull(repo.find(id));
    }

    @Test
    void testFindAllSwimlanes() {
        Swimlane s1 = new Swimlane(0, "Lane 1", 1);
        Swimlane s2 = new Swimlane(0, "Lane 2", 1);
        repo.persist(s1);
        repo.persist(s2);

        Collection<Swimlane> all = repo.findAll();
        assertEquals(2, all.size());
        assertTrue(all.contains(s1));
        assertTrue(all.contains(s2));
    }

    @Test
    void testFindByProject() {
        Swimlane s1 = new Swimlane(0, "Lane 1", 1);
        Swimlane s2 = new Swimlane(0, "Lane 2", 2);
        Swimlane s3 = new Swimlane(0, "Lane 3", 1);
        repo.persist(s1);
        repo.persist(s2);
        repo.persist(s3);

        Collection<Swimlane> project1 = repo.findByProject(1);
        assertEquals(2, project1.size());
        assertTrue(project1.contains(s1));
        assertTrue(project1.contains(s3));

        Collection<Swimlane> project2 = repo.findByProject(2);
        assertEquals(1, project2.size());
        assertTrue(project2.contains(s2));
    }

    @Test
    void testGetNextId() {
        long nextIdBefore = repo.getNextId();
        Swimlane s = new Swimlane(0, "Lane", 1);
        repo.persist(s);
        long nextIdAfter = repo.getNextId();

        assertEquals(nextIdBefore + 1, nextIdAfter);
    }

    @Test
    void testPersistMethodCalled() {
        Swimlane s = new Swimlane(0, "Lane", 1);
        repo.persist(s);

        verify(repo, times(1)).persist(s); // Vérifie que persist a été appelé exactement 1 fois
    }
}