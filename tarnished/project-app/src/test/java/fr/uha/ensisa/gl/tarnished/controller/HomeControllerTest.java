package fr.uha.ensisa.gl.tarnished.controller;

import fr.uha.ensisa.gl.entities.Project;
import fr.uha.ensisa.gl.entities.Story;
import fr.uha.ensisa.gl.entities.StoryStatus;
import fr.uha.ensisa.gl.tarnished.config.PathHelper;
import fr.uha.ensisa.gl.tarnished.repos.ProjectRepo;
import fr.uha.ensisa.gl.tarnished.repos.RepoFactory;
import fr.uha.ensisa.gl.tarnished.repos.StoryRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class HomeControllerTest {

    @Mock
    private RepoFactory repoFactory;

    @Mock
    private ProjectRepo projectRepo;

    @Mock
    private StoryRepo storyRepo;

    @Mock
    private PathHelper pathHelper;

    @InjectMocks
    private HomeController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(repoFactory.getProjectRepo()).thenReturn(projectRepo);
        when(repoFactory.getStoryRepo()).thenReturn(storyRepo);
        controller.setPathHelper(pathHelper);
        when(pathHelper.redirect("/")).thenReturn("redirect:/");
    }

    @Test
    void testHome() {
        //Prepare test data
        List<Project> projects = new ArrayList<>();
        Project project1 = new Project();
        project1.setId(1);
        project1.setName("Project 1");
        projects.add(project1);

        List<Story> stories = new ArrayList<>();
        Story story1 = new Story();
        story1.setId(1);
        story1.setTitle("Story 1");
        story1.setStatus(StoryStatus.IN_PROGRESS);
        stories.add(story1);

        Story story2 = new Story();
        story2.setId(2);
        story2.setTitle("Story 2");
        story2.setStatus(StoryStatus.DONE);
        stories.add(story2);

        when(projectRepo.findAll()).thenReturn(projects);
        when(storyRepo.findAll()).thenReturn(stories);

        //Call controller method
        ModelAndView mav = controller.home();

        //Verify
        assertNotNull(mav);
        assertEquals("home", mav.getViewName());
        assertEquals(projects, mav.getModel().get("projects"));
        assertEquals(stories, mav.getModel().get("recentStories"));
        assertEquals(1L, mav.getModel().get("inProgressCount"));

        verify(projectRepo).findAll();
        verify(storyRepo).findAll();
    }

    @Test
    void testHomeWithNoStories() {
        List<Project> projects = new ArrayList<>();
        List<Story> stories = new ArrayList<>();

        when(projectRepo.findAll()).thenReturn(projects);
        when(storyRepo.findAll()).thenReturn(stories);

        ModelAndView mav = controller.home();

        assertNotNull(mav);
        assertEquals("home", mav.getViewName());
        assertEquals(0L, mav.getModel().get("inProgressCount"));
    }

    @Test
    void testHomeWithMultipleInProgressStories() {
        List<Project> projects = new ArrayList<>();
        List<Story> stories = new ArrayList<>();
        
        for (int i = 0; i < 5; i++) {
            Story story = new Story();
            story.setId(i);
            story.setTitle("Story " + i);
            story.setStatus(StoryStatus.IN_PROGRESS);
            stories.add(story);
        }

        when(projectRepo.findAll()).thenReturn(projects);
        when(storyRepo.findAll()).thenReturn(stories);

        ModelAndView mav = controller.home();

        assertNotNull(mav);
        assertEquals(5L, mav.getModel().get("inProgressCount"));
    }

    @Test
    void testHello() {
        String result = controller.hello();
        assertEquals("redirect:/", result);
        verify(pathHelper).redirect("/");
    }
}

