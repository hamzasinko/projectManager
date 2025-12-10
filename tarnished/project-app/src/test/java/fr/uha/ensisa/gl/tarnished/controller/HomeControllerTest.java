package fr.uha.ensisa.gl.tarnished.controller;

import fr.uha.ensisa.gl.entities.Project;
import fr.uha.ensisa.gl.entities.Story;
import fr.uha.ensisa.gl.entities.StoryStatus;
import fr.uha.ensisa.gl.tarnished.repos.ProjectRepo;
import fr.uha.ensisa.gl.tarnished.repos.RepoFactory;
import fr.uha.ensisa.gl.tarnished.repos.StoryRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("HomeController Tests")
class HomeControllerTest {

    @Mock
    private RepoFactory repoFactory;

    @Mock
    private ProjectRepo projectRepo;

    @Mock
    private StoryRepo storyRepo;

    private HomeController homeController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        homeController = new HomeController();
        homeController.setRepoFactory(repoFactory);

        when(repoFactory.getProjectRepo()).thenReturn(projectRepo);
        when(repoFactory.getStoryRepo()).thenReturn(storyRepo);
    }

    @Test
    @DisplayName("Should display home page with projects and stories")
    void testHome() {
        // Arrange
        Project project1 = new Project();
        project1.setId(1);
        project1.setName("Project 1");

        Project project2 = new Project();
        project2.setId(2);
        project2.setName("Project 2");

        Collection<Project> projects = Arrays.asList(project1, project2);

        Story story1 = new Story();
        story1.setId(1);
        story1.setTitle("Story 1");
        story1.setStatus(StoryStatus.IN_PROGRESS);

        Story story2 = new Story();
        story2.setId(2);
        story2.setTitle("Story 2");
        story2.setStatus(StoryStatus.DONE);

        Story story3 = new Story();
        story3.setId(3);
        story3.setTitle("Story 3");
        story3.setStatus(StoryStatus.IN_PROGRESS);

        Collection<Story> stories = Arrays.asList(story1, story2, story3);

        when(repoFactory.getProjectRepo()).thenReturn(projectRepo);
        when(repoFactory.getStoryRepo()).thenReturn(storyRepo);
        when(projectRepo.findAll()).thenReturn(projects);
        when(storyRepo.findAll()).thenReturn(stories);

        // Act
        ModelAndView result = homeController.home();

        // Assert
        assertNotNull(result);
        assertEquals("home", result.getViewName());
        assertEquals(projects, result.getModel().get("projects"));
        assertEquals(stories, result.getModel().get("recentStories"));
        assertEquals(2L, result.getModel().get("inProgressCount")); // 2 stories in progress

        verify(projectRepo).findAll();
        verify(storyRepo).findAll();
    }

    @Test
    @DisplayName("Should handle empty projects list")
    void testHomeWithEmptyProjects() {
        // Arrange
        Collection<Project> emptyProjects = new ArrayList<>();
        Collection<Story> stories = Arrays.asList(new Story());

        when(repoFactory.getProjectRepo()).thenReturn(projectRepo);
        when(repoFactory.getStoryRepo()).thenReturn(storyRepo);
        when(projectRepo.findAll()).thenReturn(emptyProjects);
        when(storyRepo.findAll()).thenReturn(stories);

        // Act
        ModelAndView result = homeController.home();

        // Assert
        assertNotNull(result);
        assertEquals("home", result.getViewName());
        assertTrue(((Collection<?>) result.getModel().get("projects")).isEmpty());
    }

    @Test
    @DisplayName("Should handle empty stories list")
    void testHomeWithEmptyStories() {
        // Arrange
        Collection<Project> projects = Arrays.asList(new Project());
        Collection<Story> emptyStories = new ArrayList<>();

        when(repoFactory.getProjectRepo()).thenReturn(projectRepo);
        when(repoFactory.getStoryRepo()).thenReturn(storyRepo);
        when(projectRepo.findAll()).thenReturn(projects);
        when(storyRepo.findAll()).thenReturn(emptyStories);

        // Act
        ModelAndView result = homeController.home();

        // Assert
        assertNotNull(result);
        assertEquals("home", result.getViewName());
        assertEquals(0L, result.getModel().get("inProgressCount"));
    }

    @Test
    @DisplayName("Should count only IN_PROGRESS stories")
    void testInProgressCount() {
        // Arrange
        Story backlogStory = new Story();
        backlogStory.setStatus(StoryStatus.BACKLOG);

        Story inProgressStory1 = new Story();
        inProgressStory1.setStatus(StoryStatus.IN_PROGRESS);

        Story inProgressStory2 = new Story();
        inProgressStory2.setStatus(StoryStatus.IN_PROGRESS);

        Story doneStory = new Story();
        doneStory.setStatus(StoryStatus.DONE);

        Story reviewStory = new Story();
        reviewStory.setStatus(StoryStatus.REVIEW);

        Story blockedStory = new Story();
        blockedStory.setStatus(StoryStatus.BLOCKED);

        Collection<Story> stories = Arrays.asList(
            backlogStory, inProgressStory1, inProgressStory2, 
            doneStory, reviewStory, blockedStory
        );

        when(repoFactory.getProjectRepo()).thenReturn(projectRepo);
        when(repoFactory.getStoryRepo()).thenReturn(storyRepo);
        when(projectRepo.findAll()).thenReturn(new ArrayList<>());
        when(storyRepo.findAll()).thenReturn(stories);

        // Act
        ModelAndView result = homeController.home();

        // Assert
        assertEquals(2L, result.getModel().get("inProgressCount")); // Only 2 IN_PROGRESS stories
    }

    @Test
    @DisplayName("Should handle stories with null status")
    void testHomeWithNullStatusStories() {
        // Arrange
        Story storyWithNullStatus = new Story();
        storyWithNullStatus.setStatus(null);

        Story inProgressStory = new Story();
        inProgressStory.setStatus(StoryStatus.IN_PROGRESS);

        Collection<Story> stories = Arrays.asList(storyWithNullStatus, inProgressStory);

        when(repoFactory.getProjectRepo()).thenReturn(projectRepo);
        when(repoFactory.getStoryRepo()).thenReturn(storyRepo);
        when(projectRepo.findAll()).thenReturn(new ArrayList<>());
        when(storyRepo.findAll()).thenReturn(stories);

        // Act
        ModelAndView result = homeController.home();

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getModel().get("inProgressCount")); // Null status doesn't count
    }

    @Test
    @DisplayName("Should redirect to home from hello endpoint")
    void testHelloRedirect() {
        // Act
        String result = homeController.hello();

        // Assert
        assertEquals("redirect:/", result);
    }

    @Test
    @DisplayName("Should always return non-null ModelAndView")
    void testHomeNeverReturnsNull() {
        // Arrange
        when(repoFactory.getProjectRepo()).thenReturn(projectRepo);
        when(repoFactory.getStoryRepo()).thenReturn(storyRepo);
        when(projectRepo.findAll()).thenReturn(new ArrayList<>());
        when(storyRepo.findAll()).thenReturn(new ArrayList<>());

        // Act
        ModelAndView result = homeController.home();

        // Assert
        assertNotNull(result);
        assertNotNull(result.getModel());
    }
}
