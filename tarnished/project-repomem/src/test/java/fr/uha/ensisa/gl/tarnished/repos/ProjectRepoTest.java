package fr.uha.ensisa.gl.tarnished.repos;

import fr.uha.ensisa.gl.entities.Project;
import fr.uha.ensisa.gl.tarnished.mems.ProjectRepoMem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ProjectRepoTest {
    ProjectRepo repo;

    @BeforeEach
    void setup() {
        repo = new ProjectRepoMem();
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
}
