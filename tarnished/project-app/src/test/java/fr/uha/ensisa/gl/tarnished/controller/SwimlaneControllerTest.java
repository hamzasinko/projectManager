package fr.uha.ensisa.gl.tarnished.controller;

import fr.uha.ensisa.gl.entities.Project;
import fr.uha.ensisa.gl.entities.Swimlane;
import fr.uha.ensisa.gl.tarnished.config.PathHelper;
import fr.uha.ensisa.gl.tarnished.repos.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    @Mock
    private PathHelper pathHelper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(repoFactory.getProjectRepo()).thenReturn(projectRepo);
        when(repoFactory.getSwimlaneRepo()).thenReturn(swimlaneRepo);
        when(repoFactory.getStoryRepo()).thenReturn(storyRepo);
        controller.setPathHelper(pathHelper);
        
        // Configure PathHelper mock
        when(pathHelper.redirectView(anyString())).thenAnswer(invocation -> {
            String path = invocation.getArgument(0);
            return new ModelAndView("redirect:" + path);
        });
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

        fr.uha.ensisa.gl.entities.Story story = new fr.uha.ensisa.gl.entities.Story();
        story.setId(1);
        story.setProjectId(1L);

        when(projectRepo.find(1L)).thenReturn(project);
        when(storyRepo.findAll()).thenReturn(List.of(story));

        ArgumentCaptor<fr.uha.ensisa.gl.entities.Story> storyCaptor = ArgumentCaptor.forClass(fr.uha.ensisa.gl.entities.Story.class);

        ModelAndView mav = controller.createSwimlane("Test Lane", 1L);

        verify(swimlaneRepo, times(1)).persist(any(Swimlane.class));
        verify(projectRepo, times(1)).update(project);
        verify(storyRepo, times(1)).findAll();
        verify(storyRepo, times(1)).persist(storyCaptor.capture()); // Vérifie que forEach est appelé

        assertEquals("redirect:/board/1", mav.getViewName());
        assertEquals(1, project.getSwimlanes().size());
        assertNotNull(storyCaptor.getValue().getSwimlaneId());
    }

    @Test
    void testCreateSwimlane_ProjectWithoutSwimlane() {
        Project project = new Project();
        project.setId(1);
        // project.getSwimlanes() is null

        when(projectRepo.find(1L)).thenReturn(project);
        when(storyRepo.findAll()).thenReturn(new ArrayList<>());

        ModelAndView mav = controller.createSwimlane("First Lane", 1L);

        ArgumentCaptor<Swimlane> swimlaneCaptor = ArgumentCaptor.forClass(Swimlane.class);
        verify(swimlaneRepo).persist(swimlaneCaptor.capture());
        assertEquals("First Lane", swimlaneCaptor.getValue().getName());

        ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepo).update(projectCaptor.capture());
        assertNotNull(projectCaptor.getValue().getSwimlanes());
        assertEquals(1, projectCaptor.getValue().getSwimlanes().size());

        assertEquals("redirect:/board/1", mav.getViewName());
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

        ModelAndView mav = controller.deleteSwimlane(1L,null);

        verify(swimlaneRepo, times(1)).remove(1L);
        verify(projectRepo, times(1)).update(project);

        assertEquals("redirect:/board/1", mav.getViewName());
    }

    @Test
    void testDeleteSwimlane_NullSwimlane() {
        when(swimlaneRepo.find(99L)).thenReturn(null);

        ModelAndView mav = controller.deleteSwimlane(99L, null);

        assertEquals("redirect:/", mav.getViewName());
        verify(projectRepo, never()).update(any());
    }

    @Test
    void testDeleteSwimlane_NullProject() {
        Swimlane swimlane = new Swimlane();
        swimlane.setId(1);
        swimlane.setProjectId(99); // Invalid project ID

        when(swimlaneRepo.find(1L)).thenReturn(swimlane);
        when(projectRepo.find(99L)).thenReturn(null);

        ModelAndView mav = controller.deleteSwimlane(1L, null);

        assertEquals("redirect:/", mav.getViewName());
        verify(swimlaneRepo, never()).remove(anyLong());
    }

    @Test
    void testDeleteSwimlane_WithOtherSwimlanes() {
        Swimlane swimlaneToDelete = new Swimlane(1, "Delete Me", 1);
        Swimlane otherSwimlane = new Swimlane(2, "Keep Me", 1);
        Project project = new Project();
        project.setId(1);
        project.setSwimlanes(new ArrayList<>(List.of(swimlaneToDelete, otherSwimlane)));

        when(swimlaneRepo.find(1L)).thenReturn(swimlaneToDelete);
        when(projectRepo.find(1L)).thenReturn(project);
        when(storyRepo.findAll()).thenReturn(new ArrayList<>());

        ModelAndView mav = controller.deleteSwimlane(1L, 2L);

        verify(swimlaneRepo).remove(1L);
        ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepo).update(projectCaptor.capture());

        assertEquals(1, projectCaptor.getValue().getSwimlanes().size());
        assertEquals(2, projectCaptor.getValue().getSwimlanes().get(0).getId());
        assertEquals("redirect:/board/1", mav.getViewName());
    }

    @Test
    void testDeleteSwimlane_LastSwimlane() {
        Swimlane lastSwimlane = new Swimlane(1, "Last One", 1);
        Project project = new Project();
        project.setId(1);
        project.setSwimlanes(new ArrayList<>(List.of(lastSwimlane)));

        when(swimlaneRepo.find(1L)).thenReturn(lastSwimlane);
        when(projectRepo.find(1L)).thenReturn(project);
        when(storyRepo.findAll()).thenReturn(new ArrayList<>());

        ModelAndView mav = controller.deleteSwimlane(1L, null);

        verify(swimlaneRepo).remove(1L);
        ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepo).update(projectCaptor.capture());

        assertTrue(projectCaptor.getValue().getSwimlanes().isEmpty());
        assertEquals("redirect:/board/1", mav.getViewName());
    }

    @Test
    void testEditSwimlane_ValidId() {
        Swimlane swimlane = new Swimlane(1, "Editable", 1);
        when(swimlaneRepo.find(1L)).thenReturn(swimlane);

        Map<String, Object> response = controller.editSwimlane(1L);

        assertNotNull(response);
        assertEquals(1L, response.get("id"));
        assertEquals("Editable", response.get("name"));
        assertEquals(1, response.get("projectId"));
    }

    @Test
    void testEditSwimlane_InvalidId() {
        when(swimlaneRepo.find(99L)).thenReturn(null);
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
            controller.editSwimlane(99L);
        });
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

    @Test
    void testNewSwimlaneForm_WithNullProjectId() {
        ModelAndView mav = controller.newSwimlaneForm(null);
        assertEquals("redirect:/", mav.getViewName());
    }

    @Test
    void testCreateSwimlane_WithNullProjectId() {
        ModelAndView mav = controller.createSwimlane("Test Lane", null);
        assertEquals("redirect:/", mav.getViewName());
    }

    @Test
    void testCreateSwimlane_WithNullSwimlanesList() {
        Project project = new Project();
        project.setSwimlanes(null);

        when(projectRepo.find(1L)).thenReturn(project);
        when(storyRepo.findAll()).thenReturn(new ArrayList<>());

        ModelAndView mav = controller.createSwimlane("Test Lane", 1L);

        verify(swimlaneRepo, times(1)).persist(any(Swimlane.class));
        verify(projectRepo, times(1)).update(project);
        assertNotNull(project.getSwimlanes());
        assertEquals(1, project.getSwimlanes().size());
        assertEquals("redirect:/board/1", mav.getViewName());
    }

    @Test
    void testCreateSwimlane_NotFirstSwimlane() {
        Project project = new Project();
        project.setSwimlanes(new ArrayList<>());

        Swimlane existingSwimlane = new Swimlane();
        existingSwimlane.setId(99);
        project.getSwimlanes().add(existingSwimlane);

        when(projectRepo.find(1L)).thenReturn(project);
        when(storyRepo.findAll()).thenReturn(new ArrayList<>());

        ModelAndView mav = controller.createSwimlane("Second Lane", 1L);

        verify(swimlaneRepo, times(1)).persist(any(Swimlane.class));
        verify(projectRepo, times(1)).update(project);
        verify(storyRepo, never()).findAll();
        assertEquals(2, project.getSwimlanes().size());
        assertEquals("redirect:/board/1", mav.getViewName());
    }

    @Test
    void testCreateSwimlane_VerifiesSettersCalled() {
        Project project = new Project();
        project.setSwimlanes(new ArrayList<>());

        when(projectRepo.find(1L)).thenReturn(project);
        when(storyRepo.findAll()).thenReturn(new ArrayList<>());

        ArgumentCaptor<Swimlane> swimlaneCaptor = ArgumentCaptor.forClass(Swimlane.class);

        ModelAndView mav = controller.createSwimlane("Test Name", 1L);

        verify(swimlaneRepo).persist(swimlaneCaptor.capture());
        Swimlane captured = swimlaneCaptor.getValue();
        assertEquals("Test Name", captured.getName());
        assertEquals(1, captured.getProjectId());
    }

    @Test
    void testDeleteSwimlane_WithNullSwimlane() {
        when(swimlaneRepo.find(1L)).thenReturn(null);

        ModelAndView mav = controller.deleteSwimlane(1L, null);

        assertEquals("redirect:/", mav.getViewName());
        verify(swimlaneRepo, never()).remove(anyLong());
    }

    @Test
    void testDeleteSwimlane_WithNullProject() {
        Swimlane swimlane = new Swimlane();
        swimlane.setId(1);
        swimlane.setProjectId(1);

        when(swimlaneRepo.find(1L)).thenReturn(swimlane);
        when(projectRepo.find(1L)).thenReturn(null);

        ModelAndView mav = controller.deleteSwimlane(1L, null);

        assertEquals("redirect:/", mav.getViewName());
        verify(swimlaneRepo, never()).remove(anyLong());
    }

    @Test
    void testDeleteSwimlane_WithStoriesToReassign() {
        Swimlane swimlane = new Swimlane();
        swimlane.setId(1);
        swimlane.setProjectId(1);

        Swimlane targetSwimlane = new Swimlane();
        targetSwimlane.setId(2);

        Project project = new Project();
        project.setSwimlanes(new ArrayList<>());
        project.getSwimlanes().add(swimlane);
        project.getSwimlanes().add(targetSwimlane);

        fr.uha.ensisa.gl.entities.Story story = new fr.uha.ensisa.gl.entities.Story();
        story.setId(10);
        story.setProjectId(1L);
        story.setSwimlaneId(1L);

        when(swimlaneRepo.find(1L)).thenReturn(swimlane);
        when(projectRepo.find(1L)).thenReturn(project);
        when(storyRepo.findAll()).thenReturn(List.of(story));

        ModelAndView mav = controller.deleteSwimlane(1L, 2L);

        verify(swimlaneRepo, times(1)).remove(1L);
        verify(storyRepo, times(1)).persist(story);
        assertEquals(Long.valueOf(2), story.getSwimlaneId());
        assertEquals("redirect:/board/1", mav.getViewName());
    }

    @Test
    void testDeleteSwimlane_WithNoRemainingSwimlanes() {
        Swimlane swimlane = new Swimlane();
        swimlane.setId(1);
        swimlane.setProjectId(1);

        Project project = new Project();
        project.setSwimlanes(new ArrayList<>());
        project.getSwimlanes().add(swimlane);

        fr.uha.ensisa.gl.entities.Story story = new fr.uha.ensisa.gl.entities.Story();
        story.setId(10);
        story.setProjectId(1L);
        story.setSwimlaneId(1L);

        when(swimlaneRepo.find(1L)).thenReturn(swimlane);
        when(projectRepo.find(1L)).thenReturn(project);
        when(storyRepo.findAll()).thenReturn(List.of(story));

        ModelAndView mav = controller.deleteSwimlane(1L, null);

        verify(swimlaneRepo, times(1)).remove(1L);
        verify(storyRepo, times(1)).persist(story);
        assertNull(story.getSwimlaneId());
        assertEquals("redirect:/board/1", mav.getViewName());
    }

    @Test
    void testEditSwimlane_WithValidId() {
        Swimlane swimlane = new Swimlane();
        swimlane.setId(1);
        swimlane.setName("Test Lane");
        swimlane.setProjectId(5);

        when(swimlaneRepo.find(1L)).thenReturn(swimlane);

        Map<String, Object> response = controller.editSwimlane(1L);

        assertNotNull(response);
        assertEquals(1L, response.get("id"));
        assertEquals("Test Lane", response.get("name"));
        assertEquals(5, response.get("projectId"));
    }

    @Test
    void testEditSwimlane_WithInvalidId() {
        when(swimlaneRepo.find(999L)).thenReturn(null);

        assertThrows(org.springframework.web.server.ResponseStatusException.class,
            () -> controller.editSwimlane(999L));
    }

    @Test
    void testUpdateSwimlane_WithNullSwimlane() {
        when(swimlaneRepo.find(999L)).thenReturn(null);

        ModelAndView mav = controller.updateSwimlane(999L, "Name");

        assertEquals("redirect:/", mav.getViewName());
        verify(swimlaneRepo, never()).persist(any());
    }

    @Test
    void testUpdateSwimlane_WithNullProject() {
        Swimlane swimlane = new Swimlane();
        swimlane.setId(1);
        swimlane.setProjectId(1);

        when(swimlaneRepo.find(1L)).thenReturn(swimlane);
        when(projectRepo.find(1L)).thenReturn(null);

        ModelAndView mav = controller.updateSwimlane(1L, "Updated Name");

        verify(swimlaneRepo, times(1)).persist(swimlane);
        assertEquals("Updated Name", swimlane.getName());
        assertEquals("redirect:/board/1", mav.getViewName());
    }

    @Test
    void testUpdateSwimlane_WithNullSwimlanesList() {
        Swimlane swimlane = new Swimlane();
        swimlane.setId(1);
        swimlane.setProjectId(1);

        Project project = new Project();
        project.setSwimlanes(null);

        when(swimlaneRepo.find(1L)).thenReturn(swimlane);
        when(projectRepo.find(1L)).thenReturn(project);

        ModelAndView mav = controller.updateSwimlane(1L, "Updated Name");

        verify(swimlaneRepo, times(1)).persist(swimlane);
        verify(projectRepo, never()).update(project);
        assertEquals("Updated Name", swimlane.getName());
        assertEquals("redirect:/board/1", mav.getViewName());
    }

    @Test
    void testUpdateSwimlane_VerifiesReplaceAllCalled() {
        Swimlane swimlane = new Swimlane();
        swimlane.setId(1);
        swimlane.setProjectId(1);
        swimlane.setName("Old Name");

        Swimlane existingSwimlane = new Swimlane();
        existingSwimlane.setId(1);
        existingSwimlane.setName("Old Name");

        Project project = new Project();
        project.setSwimlanes(new ArrayList<>());
        project.getSwimlanes().add(existingSwimlane);

        when(swimlaneRepo.find(1L)).thenReturn(swimlane);
        when(projectRepo.find(1L)).thenReturn(project);

        ModelAndView mav = controller.updateSwimlane(1L, "Updated Name");

        verify(swimlaneRepo, times(1)).persist(swimlane);
        verify(projectRepo, times(1)).update(project);
        assertEquals("Updated Name", swimlane.getName());
        // Vérifie que replaceAll a été appelé (le swimlane dans la liste devrait être mis à jour)
        assertEquals("Updated Name", project.getSwimlanes().get(0).getName());
        assertEquals("redirect:/board/1", mav.getViewName());
    }

    @Test
    void testDeleteSwimlane_VerifiesForEachCalledOnStories() {
        Swimlane swimlane = new Swimlane();
        swimlane.setId(1);
        swimlane.setProjectId(1);

        Project project = new Project();
        project.setSwimlanes(new ArrayList<>());
        project.getSwimlanes().add(swimlane);

        fr.uha.ensisa.gl.entities.Story story1 = new fr.uha.ensisa.gl.entities.Story();
        story1.setId(1);
        story1.setProjectId(1L);
        story1.setSwimlaneId(1L);

        fr.uha.ensisa.gl.entities.Story story2 = new fr.uha.ensisa.gl.entities.Story();
        story2.setId(2);
        story2.setProjectId(2L); // Different project, should not be affected
        story2.setSwimlaneId(1L);

        when(swimlaneRepo.find(1L)).thenReturn(swimlane);
        when(projectRepo.find(1L)).thenReturn(project);
        when(storyRepo.findAll()).thenReturn(List.of(story1, story2));

        ArgumentCaptor<fr.uha.ensisa.gl.entities.Story> storyCaptor = ArgumentCaptor.forClass(fr.uha.ensisa.gl.entities.Story.class);

        ModelAndView mav = controller.deleteSwimlane(1L, null);

        verify(swimlaneRepo, times(1)).remove(1L);
        verify(storyRepo, times(1)).persist(storyCaptor.capture()); // Vérifie que forEach a été appelé
        assertNull(story1.getSwimlaneId()); // Story du projet devrait avoir swimlaneId = null
        assertEquals("redirect:/board/1", mav.getViewName());
    }

    @Test
    void testUpdateSwimlane_NullSwimlane() {
        when(swimlaneRepo.find(99L)).thenReturn(null);

        ModelAndView mav = controller.updateSwimlane(99L, "New Name");

        assertEquals("redirect:/", mav.getViewName());
        verify(swimlaneRepo, never()).persist(any());
    }

    @Test
    void testDeleteSwimlane_WithLambdaConditions() {
        Swimlane swimlane = new Swimlane();
        swimlane.setId(1);
        swimlane.setProjectId(1);

        Swimlane targetSwimlane = new Swimlane();
        targetSwimlane.setId(2);

        Project project = new Project();
        project.setSwimlanes(new ArrayList<>());
        project.getSwimlanes().add(swimlane);
        project.getSwimlanes().add(targetSwimlane);

        fr.uha.ensisa.gl.entities.Story storyWithNullProjectId = new fr.uha.ensisa.gl.entities.Story();
        storyWithNullProjectId.setId(1);
        storyWithNullProjectId.setProjectId(null); // Test du filtre projectId != null
        storyWithNullProjectId.setSwimlaneId(1L);

        fr.uha.ensisa.gl.entities.Story storyWithNullSwimlaneId = new fr.uha.ensisa.gl.entities.Story();
        storyWithNullSwimlaneId.setId(2);
        storyWithNullSwimlaneId.setProjectId(1L);
        storyWithNullSwimlaneId.setSwimlaneId(null); // Test du filtre swimlaneId != null

        fr.uha.ensisa.gl.entities.Story storyMatching = new fr.uha.ensisa.gl.entities.Story();
        storyMatching.setId(3);
        storyMatching.setProjectId(1L);
        storyMatching.setSwimlaneId(1L);

        when(swimlaneRepo.find(1L)).thenReturn(swimlane);
        when(projectRepo.find(1L)).thenReturn(project);
        when(storyRepo.findAll()).thenReturn(List.of(storyWithNullProjectId, storyWithNullSwimlaneId, storyMatching));

        ModelAndView mav = controller.deleteSwimlane(1L, 2L);

        verify(swimlaneRepo, times(1)).remove(1L);
        verify(storyRepo, times(1)).persist(storyMatching); // Seule la story matching devrait être persistée
        assertEquals(Long.valueOf(2), storyMatching.getSwimlaneId());
        assertEquals("redirect:/board/1", mav.getViewName());
    }

    @Test
    void testEditSwimlane_ShouldThrowExceptionWhenNotFound() {
        when(swimlaneRepo.find(999L)).thenReturn(null);

        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
            controller.editSwimlane(999L);
        });
    }
}