package fr.uha.ensisa.gl.tarnished.mems;

import fr.uha.ensisa.gl.tarnished.repos.ColumnRepo;
import fr.uha.ensisa.gl.tarnished.repos.ProjectRepo;
import fr.uha.ensisa.gl.tarnished.repos.StoryRepo;
import fr.uha.ensisa.gl.tarnished.repos.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RepoFactoryMemTest {
    private RepoFactoryMem factory;

    @BeforeEach
    void setup() {
        factory = new RepoFactoryMem();
    }

    @Test
    void testGetStoryRepo() {
        StoryRepo repo = factory.getStoryRepo();
        assertNotNull(repo);
        assertTrue(repo instanceof StoryRepoMem);
    }

    @Test
    void testGetColumnRepo() {
        ColumnRepo repo = factory.getColumnRepo();
        assertNotNull(repo);
        assertTrue(repo instanceof ColumnRepoMem);
    }

    @Test
    void testGetProjectRepo() {
        ProjectRepo repo = factory.getProjectRepo();
        assertNotNull(repo);
        assertTrue(repo instanceof ProjectRepoMem);
    }

    @Test
    void testGetUserRepo() {
        UserRepo repo = factory.getUserRepo();
        assertNotNull(repo);
        assertTrue(repo instanceof UserRepoMem);
    }

    @Test
    void testColumnRepoHasStoryRepoInjected() {
        ColumnRepo columnRepo = factory.getColumnRepo();
        assertNotNull(columnRepo);
        // Verify it's the same instance
        assertSame(factory.storyRepo, factory.getStoryRepo());
    }

    @Test
    void testProjectRepoHasColumnRepoInjected() {
        ProjectRepo projectRepo = factory.getProjectRepo();
        assertNotNull(projectRepo);
        // Verify it's the same instance
        assertSame(factory.columnRepo, factory.getColumnRepo());
    }

    @Test
    void testAllReposAreNotNull() {
        assertNotNull(factory.storyRepo);
        assertNotNull(factory.columnRepo);
        assertNotNull(factory.projectRepo);
        assertNotNull(factory.userRepo);
    }

    @Test
    void testGetReposReturnsSameInstance() {
        StoryRepo repo1 = factory.getStoryRepo();
        StoryRepo repo2 = factory.getStoryRepo();
        assertSame(repo1, repo2, "Should return same instance");
    }
}

