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
        //Verify it's the same instance
        assertSame(factory.storyRepo, factory.getStoryRepo());
    }

    @Test
    void testProjectRepoHasColumnRepoInjected() {
        ProjectRepo projectRepo = factory.getProjectRepo();
        assertNotNull(projectRepo);
        //Verify it's the same instance
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

    @Test
    void testColumnRepoCanUseStoryRepo() {
        ColumnRepo columnRepo = factory.getColumnRepo();
        StoryRepo storyRepo = factory.getStoryRepo();
        
        //Create a story
        fr.uha.ensisa.gl.entities.Story story = new fr.uha.ensisa.gl.entities.Story();
        story.setTitle("Test Story");
        storyRepo.persist(story);
        
        //Create a column and verify it can work with stories
        fr.uha.ensisa.gl.entities.Column column = new fr.uha.ensisa.gl.entities.Column();
        column.setName("Test Column");
        column.setMaxCapacity(5);
        columnRepo.persist(column);
        
        //Move story to column - this requires StoryRepo to be injected
        storyRepo.moveToColumn((long) story.getId(), (long) column.getId());
        
        //Verify the story was moved
        fr.uha.ensisa.gl.entities.Story foundStory = storyRepo.find((long) story.getId());
        assertEquals((long) column.getId(), foundStory.getColumnId());
    }

    @Test
    void testProjectRepoCanUseColumnRepo() {
        ProjectRepo projectRepo = factory.getProjectRepo();
        ColumnRepo columnRepo = factory.getColumnRepo();
        
        //Create a project - this should create default columns via ColumnRepo
        fr.uha.ensisa.gl.entities.Project project = new fr.uha.ensisa.gl.entities.Project();
        project.setName("Test Project");
        projectRepo.persist(project);
        
        //Verify the project was created
        fr.uha.ensisa.gl.entities.Project foundProject = projectRepo.find(project.getId());
        assertNotNull(foundProject);
        assertEquals("Test Project", foundProject.getName());
        
        //Verify columns were created (requires ColumnRepo injection)
        //The default columns should have been created
        java.util.Collection<fr.uha.ensisa.gl.entities.Column> columns = columnRepo.findByProject((long) project.getId());
        assertTrue(columns.size() > 0, "Default columns should have been created");
    }
}

