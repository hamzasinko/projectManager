package fr.uha.ensisa.gl.tarnished.controller;

import fr.uha.ensisa.gl.entities.Project;
import fr.uha.ensisa.gl.entities.Swimlane;
import fr.uha.ensisa.gl.tarnished.repos.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SwimlaneControllerTest {

    @InjectMocks
    private SwimlaneController controller;

    @Mock
    private RepoFactory repoFactory;

    @Mock
    private ProjectRepo projectRepo;

    @Mock
    private SwimlaneRepo swimlaneRepo;

    @Mock
    private StoryRepo storyRepo;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(repoFactory.getProjectRepo()).thenReturn(projectRepo);
        when(repoFactory.getSwimlaneRepo()).thenReturn(swimlaneRepo);
        when(repoFactory.getStoryRepo()).thenReturn(storyRepo);
    }

    @Test
    void testNewSwimlaneForm_WithValidProjectId() {
        Project project = new Project();
        when(projectRepo.find(1)).thenReturn(project);

        ModelAndView mav = controller.newSwimlaneForm(1);

        assertEquals("swimlane-create", mav.getViewName());
        assertEquals(1, mav.getModel().get("projectId"));
    }

    @Test
    void testNewSwimlaneForm_WithInvalidProjectId() {
        when(projectRepo.find(99)).thenReturn(null);

        ModelAndView mav = controller.newSwimlaneForm(99);

        assertEquals("redirect:/", mav.getViewName());
    }

    @Test
    void testCreateSwimlane_FirstSwimlane() {
        Project project = new Project();
        project.setSwimlanes(new ArrayList<>());

        when(projectRepo.find(1L)).thenReturn(project);

        ModelAndView mav = controller.createSwimlane("Test Lane", 1L);

        verify(swimlaneRepo, times(1)).persist(any(Swimlane.class));
        verify(projectRepo, times(1)).update(project);
        verify(storyRepo, times(1)).findAll();

        assertEquals("redirect:/board/1", mav.getViewName());
        assertEquals(1, project.getSwimlanes().size());
    }

    @Test
    void testCreateSwimlane_WithInvalidProject() {
        when(projectRepo.find(99L)).thenReturn(null);

        ModelAndView mav = controller.createSwimlane("Test Lane", 99L);

        assertEquals("redirect:/", mav.getViewName());
    }

    @Test
    void testDeleteSwimlane() {
        Swimlane swimlane = new Swimlane();
        swimlane.setId(1);
        swimlane.setProjectId(1);

        Project project = new Project();
        project.setSwimlanes(new ArrayList<>());
        project.getSwimlanes().add(swimlane);

        when(swimlaneRepo.find(1L)).thenReturn(swimlane);
        when(projectRepo.find(1L)).thenReturn(project);
        when(storyRepo.findAll()).thenReturn(new ArrayList<>());

        ModelAndView mav = controller.deleteSwimlane(1L);

        verify(swimlaneRepo, times(1)).remove(1L);
        verify(projectRepo, times(1)).update(project);

        assertEquals("redirect:/board/1", mav.getViewName());
    }

    @Test
    void testUpdateSwimlane() {
        Swimlane swimlane = new Swimlane();
        swimlane.setId(1);
        swimlane.setProjectId(1);

        Project project = new Project();
        project.setSwimlanes(new ArrayList<>());
        project.getSwimlanes().add(swimlane);

        when(swimlaneRepo.find(1L)).thenReturn(swimlane);
        when(projectRepo.find(1L)).thenReturn(project);

        ModelAndView mav = controller.updateSwimlane(1, "Updated Name");

        verify(swimlaneRepo, times(1)).persist(swimlane);
        verify(projectRepo, times(1)).update(project);

        assertEquals("Updated Name", swimlane.getName());
        assertEquals("redirect:/board/1", mav.getViewName());
    }
}