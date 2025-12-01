package fr.uha.ensisa.gl.tarnished.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.RequestEntity.post;

import fr.uha.ensisa.gl.entities.User;
import fr.uha.ensisa.gl.tarnished.repos.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.ArgumentCaptor;
import org.springframework.web.servlet.ModelAndView;

import fr.uha.ensisa.gl.tarnished.repos.RepoFactory;
import fr.uha.ensisa.gl.tarnished.repos.ProjectRepo;
import fr.uha.ensisa.gl.entities.Project;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Arrays;
import java.util.List;

/**
 * Tests unitaires avec Mockito pour ProjectController
 * Ces tests vérifient le controller indépendamment du repository
 */
public class ProjectControllerTest {
    
    @Mock 
    private RepoFactory repoFactory;
    
    @Mock 
    private ProjectRepo projectRepo;

    @Mock
    UserRepo userRepo;
    
    private ProjectController sut; // System Under Test
    
    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this); // Initialise les @Mock
        
        // Configure le mock pour retourner projectRepo
        when(repoFactory.getProjectRepo()).thenReturn(projectRepo);
        when(repoFactory.getUserRepo()).thenReturn(userRepo);
        User testUser = new User();
        testUser.setId(1);
        testUser.setName("user1");
        testUser.setEmail("email1@gmail.com");
        testUser.setPassword("password1");
        when(userRepo.getAll()).thenReturn(List.of(testUser));
        
        // Crée le controller et injecte le mock
        sut = new ProjectController();
        sut.repoFactory = repoFactory;
    }
    
    @Test
    @DisplayName("showCreateForm should return correct view")
    public void testShowCreateForm() {
        ModelAndView result = sut.showCreateForm();
        
        assertNotNull(result);
        assertEquals("project-create", result.getViewName(), 
                     "Should return project-create view");
    }
    
    @Test
    @DisplayName("createProject should call persist on repository")
    public void testCreateProject() throws IOException {
        String testName = "Test Project";
        String testDescription = "Test Description";
        
        // Appelle la méthode
        String redirect = sut.createProject(testName, testDescription);
        
        // Vérifie la redirection
        assertEquals("redirect:/project/list", redirect, 
                     "Should redirect to project list");
        

        // Vérifie que persist a été appelé avec les bons paramètres
        ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepo).persist(projectCaptor.capture());

        Project capturedProject = projectCaptor.getValue();
        assertEquals(testName, capturedProject.getName());
        assertEquals(testDescription, capturedProject.getDescription());
    }
    
    @Test
    @DisplayName("createProject should handle null description")
    public void testCreateProjectWithNullDescription() throws IOException {
        String testName = "Test Project";
        
        String redirect = sut.createProject(testName, null);
        
        assertEquals("redirect:/project/list", redirect);
        

        verify(projectRepo).persist(any(Project.class));
    }
    
    @Test
    @DisplayName("listProjects should return view with empty list when mocked")
    public void testListProjectsEmpty() throws IOException {
        // Configure le mock pour retourner une liste vide
        when(projectRepo.findAll()).thenReturn(Arrays.asList());
        
        ModelAndView result = sut.listProjects();
        
        assertNotNull(result);
        assertEquals("project-list", result.getViewName());
        
        // Vérifie que projects est dans le modèle
        assertTrue(result.getModelMap().containsKey("projects"), 
                   "Model should contain 'projects' attribute");
        
        Collection<?> projects = (Collection<?>) result.getModelMap().get("projects");
        assertNotNull(projects);

        verify(projectRepo).findAll();
    }
    
    @Test
    @DisplayName("listProjects should return view with projects when they exist")
    public void testListProjectsWithData() throws IOException {
        // Crée des projets mock
        Project p1 = mock(Project.class);
        when(p1.getName()).thenReturn("Project 1");
        when(p1.getId()).thenReturn(1);
        
        Project p2 = mock(Project.class);
        when(p2.getName()).thenReturn("Project 2");
        when(p2.getId()).thenReturn(2);
        
        // Configure le mock pour retourner ces projets
        when(projectRepo.findAll()).thenReturn(Arrays.asList(p1, p2));
        
        ModelAndView result = sut.listProjects();
        
        assertNotNull(result);

        Collection<Project> projects = (Collection<Project>) result.getModelMap().get("projects");
        assertEquals(2, projects.size(), "Should have 2 projects");
        verify(projectRepo).findAll();
    }

    @Test
    void testEditProject() {
        Project p = new Project();
        p.setId(1);
        User u = new User();
        u.setId(10);

        p.setMembers(List.of(u));

        when(repoFactory.getProjectRepo()).thenReturn(projectRepo);
        when(repoFactory.getUserRepo()).thenReturn(userRepo);

        when(projectRepo.find(1L)).thenReturn(p);
        when(userRepo.getAll()).thenReturn(List.of(u));

        ModelAndView mav = sut.editProject(1L);

        assertEquals("project-edit", mav.getViewName());
        assertEquals(p, mav.getModel().get("project"));

        List<Integer> ids = (List<Integer>) mav.getModel().get("memberIds");
        assertTrue(ids.contains(10));
    }

    @Test
    @DisplayName("deleteProject should call remove on repository and redirect")
    void testDeleteProject() {
        // Given
        long projectId = 1L;

        // Make repoFactory return the mocked projectRepo
        when(repoFactory.getProjectRepo()).thenReturn(projectRepo);

        // When: call the controller method directly
        String result = sut.deleteProject(projectId);

        // Then: verify the redirection string
        assertEquals("redirect:/project/list", result, "Should redirect to project list");

        // And: verify that remove was called on the repository with the correct ID
        verify(projectRepo).remove(projectId);
    }

    @Test
    @DisplayName("ProjectController.info should return correct project and view")
    void testProjectInfo() {
        // Arrange
        int projectId = 1;
        Project p = new Project();
        p.setId(projectId);
        p.setName("Test Project");
        p.setDescription("Description");
        User owner = new User();
        owner.setName("Owner Name");
        p.setOwner(owner);
        p.setMembers(List.of(owner));
        when(repoFactory.getProjectRepo()).thenReturn(projectRepo);
        when(projectRepo.find(projectId)).thenReturn(p);

        // Act
        ModelAndView mav = sut.showProject((long) projectId);

        // Assert
        assertEquals("project-detail", mav.getViewName(), "Should return project-info view");
        Project projectInModel = (Project) mav.getModel().get("project");
        assertEquals("Test Project", projectInModel.getName());
        assertEquals("Description", projectInModel.getDescription());
    }

}
