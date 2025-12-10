package fr.uha.ensisa.gl.tarnished.mems;

import fr.uha.ensisa.gl.tarnished.repos.ColumnRepo;
import fr.uha.ensisa.gl.tarnished.repos.ProjectRepo;
import fr.uha.ensisa.gl.tarnished.repos.StoryRepo;
import fr.uha.ensisa.gl.tarnished.repos.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RepoFactoryMem Tests")
class RepoFactoryMemTest {

    private RepoFactoryMem repoFactory;

    @BeforeEach
    void setUp() {
        repoFactory = new RepoFactoryMem();
    }

    @Test
    @DisplayName("Should return ColumnRepo instance")
    void testGetColumnRepo() {
        ColumnRepo columnRepo = repoFactory.getColumnRepo();
        assertNotNull(columnRepo);
        assertInstanceOf(ColumnRepoMem.class, columnRepo);
    }

    @Test
    @DisplayName("Should return same ColumnRepo instance on multiple calls")
    void testGetColumnRepoSingleton() {
        ColumnRepo columnRepo1 = repoFactory.getColumnRepo();
        ColumnRepo columnRepo2 = repoFactory.getColumnRepo();
        assertSame(columnRepo1, columnRepo2);
    }

    @Test
    @DisplayName("Should return ProjectRepo instance")
    void testGetProjectRepo() {
        ProjectRepo projectRepo = repoFactory.getProjectRepo();
        assertNotNull(projectRepo);
        assertInstanceOf(ProjectRepoMem.class, projectRepo);
    }

    @Test
    @DisplayName("Should return same ProjectRepo instance on multiple calls")
    void testGetProjectRepoSingleton() {
        ProjectRepo projectRepo1 = repoFactory.getProjectRepo();
        ProjectRepo projectRepo2 = repoFactory.getProjectRepo();
        assertSame(projectRepo1, projectRepo2);
    }

    @Test
    @DisplayName("Should return StoryRepo instance")
    void testGetStoryRepo() {
        StoryRepo storyRepo = repoFactory.getStoryRepo();
        assertNotNull(storyRepo);
        assertInstanceOf(StoryRepoMem.class, storyRepo);
    }

    @Test
    @DisplayName("Should return same StoryRepo instance on multiple calls")
    void testGetStoryRepoSingleton() {
        StoryRepo storyRepo1 = repoFactory.getStoryRepo();
        StoryRepo storyRepo2 = repoFactory.getStoryRepo();
        assertSame(storyRepo1, storyRepo2);
    }

    @Test
    @DisplayName("Should return UserRepo instance")
    void testGetUserRepo() {
        UserRepo userRepo = repoFactory.getUserRepo();
        assertNotNull(userRepo);
        assertInstanceOf(UserRepoMem.class, userRepo);
    }

    @Test
    @DisplayName("Should return same UserRepo instance on multiple calls")
    void testGetUserRepoSingleton() {
        UserRepo userRepo1 = repoFactory.getUserRepo();
        UserRepo userRepo2 = repoFactory.getUserRepo();
        assertSame(userRepo1, userRepo2);
    }

    @Test
    @DisplayName("Should configure dependencies between repos")
    void testRepoDependencies() {
        // Get all repos
        ColumnRepo columnRepo = repoFactory.getColumnRepo();
        ProjectRepo projectRepo = repoFactory.getProjectRepo();
        StoryRepo storyRepo = repoFactory.getStoryRepo();

        // All should be initialized
        assertNotNull(columnRepo);
        assertNotNull(projectRepo);
        assertNotNull(storyRepo);
    }
}
