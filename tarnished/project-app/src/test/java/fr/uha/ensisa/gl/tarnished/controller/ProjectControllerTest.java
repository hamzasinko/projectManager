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
        
        // Mock UserRepo
        User mockUser = new User();
        mockUser.setId(1);
        when(repoFactory.getUserRepo()).thenReturn(userRepo);
        when(userRepo.getAll()).thenReturn(Arrays.asList(mockUser));
        
        // Appelle la méthode
        String redirect = sut.createProject(testName, testDescription);
        
        //Vérifie la redirection
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
        
        // Mock UserRepo
        User mockUser = new User();
        mockUser.setId(1);
        when(repoFactory.getUserRepo()).thenReturn(userRepo);
        when(userRepo.getAll()).thenReturn(Arrays.asList(mockUser));
        
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

        // Make repoFactory return the mocked repositories
        fr.uha.ensisa.gl.tarnished.repos.StoryRepo mockStoryRepo = mock(fr.uha.ensisa.gl.tarnished.repos.StoryRepo.class);
        fr.uha.ensisa.gl.tarnished.repos.ColumnRepo mockColumnRepo = mock(fr.uha.ensisa.gl.tarnished.repos.ColumnRepo.class);
        
        when(repoFactory.getProjectRepo()).thenReturn(projectRepo);
        when(repoFactory.getStoryRepo()).thenReturn(mockStoryRepo);
        when(repoFactory.getColumnRepo()).thenReturn(mockColumnRepo);
        
        // Mock empty lists for cascade delete
        when(mockStoryRepo.findByProject(projectId)).thenReturn(List.of());
        when(mockColumnRepo.findByProject(projectId)).thenReturn(List.of());

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

    @Test
    @DisplayName("createProject with name length 29 should succeed")
    void testCreateProjectNameLength29() throws Exception {
        when(userRepo.getAll()).thenReturn(List.of(new User()));
        String name = "A".repeat(29);
        
        String result = sut.createProject(name, "desc");
        
        assertEquals("redirect:/project/list", result);
    }

    @Test
    @DisplayName("showProject with null project should redirect to list")
    void testShowProjectNull() {
        when(projectRepo.find(999L)).thenReturn(null);
        
        ModelAndView result = sut.showProject(999L);
        
        assertEquals("redirect:/project/list", result.getViewName());
    }

    @Test
    @DisplayName("editProject should create user when repo is empty")
    void testEditProjectCreatesUser() {
        Project p = new Project();
        when(projectRepo.find(1L)).thenReturn(p);
        when(userRepo.getAll()).thenReturn(List.of());
        
        sut.editProject(1L);
        
        verify(userRepo).add(any(User.class));
    }

    @Test
    @DisplayName("updateProject with null name should return error")
    public void testUpdateProjectNullName() {
        Project project = new Project();
        when(projectRepo.find(1L)).thenReturn(project);
        
        String result = sut.updateProject(1L, null, "desc", List.of());
        
        assertTrue(result.contains("error=Project name is required"));
    }

    @Test
    @DisplayName("updateProject with empty name should return error")
    public void testUpdateProjectEmptyName() {
        Project project = new Project();
        when(projectRepo.find(1L)).thenReturn(project);
        
        String result = sut.updateProject(1L, "   ", "desc", List.of());
        
        assertTrue(result.contains("error=Project name is required"));
    }

    @Test
    @DisplayName("updateProject with name too long should return error")
    public void testUpdateProjectNameTooLong() {
        Project project = new Project();
        when(projectRepo.find(1L)).thenReturn(project);
        String longName = "A".repeat(30);
        
        String result = sut.updateProject(1L, longName, "desc", List.of());
        
        assertTrue(result.contains("error=Project name must be less than 29"));
    }

    @Test
    @DisplayName("updateProject should set name and update")
    public void testUpdateProjectSuccess() {
        Project project = new Project();
        project.setId(5);
        when(projectRepo.find(1L)).thenReturn(project);
        when(userRepo.getAll()).thenReturn(List.of());
        
        String result = sut.updateProject(1L, "NewName", "NewDesc", List.of());
        
        assertEquals("NewName", project.getName());
        assertEquals("NewDesc", project.getDescription());
        verify(projectRepo).update(project);
        assertEquals("redirect:/project/info/5", result);
    }

    @Test
    @DisplayName("updateProject with non-existent project should redirect to list")
    public void testUpdateProjectNotFound() {
        when(projectRepo.find(999L)).thenReturn(null);
        
        String result = sut.updateProject(999L, "Name", "desc", List.of());
        
        assertEquals("redirect:/project/list", result);
    }

    @Test
    @DisplayName("updateProject with memberIds should update members")
    public void testUpdateProjectWithMembers() {
        Project project = new Project();
        User user1 = new User();
        user1.setId(10);
        User user2 = new User();
        user2.setId(20);
        
        when(projectRepo.find(1L)).thenReturn(project);
        when(userRepo.getAll()).thenReturn(Arrays.asList(user1, user2));
        
        sut.updateProject(1L, "Name", "desc", Arrays.asList(10L, 20L));
        
        assertEquals(2, project.getMembers().size());
        verify(projectRepo).update(project);
    }

    @Test
    @DisplayName("updateProject with empty memberIds should clear members")
    public void testUpdateProjectEmptyMembers() {
        Project project = new Project();
        project.setId(7);
        when(projectRepo.find(1L)).thenReturn(project);
        when(userRepo.getAll()).thenReturn(List.of());
        
        String result = sut.updateProject(1L, "Name", "desc", List.of());
        
        verify(projectRepo).update(project);
        assertEquals("redirect:/project/info/7", result);
    }

    // ====== NEW TESTS FOR showProjectStories ======

    @Test
    @DisplayName("showProjectStories should display stories for valid project")
    public void testShowProjectStories() {
        Project project = new Project();
        project.setId(1);
        project.setName("Test Project");
        
        fr.uha.ensisa.gl.entities.Story story1 = new fr.uha.ensisa.gl.entities.Story();
        story1.setId(10);
        fr.uha.ensisa.gl.entities.Story story2 = new fr.uha.ensisa.gl.entities.Story();
        story2.setId(20);
        
        fr.uha.ensisa.gl.tarnished.repos.StoryRepo storyRepo = mock(fr.uha.ensisa.gl.tarnished.repos.StoryRepo.class);
        
        when(projectRepo.find(1L)).thenReturn(project);
        when(repoFactory.getStoryRepo()).thenReturn(storyRepo);
        when(storyRepo.findByProject(1L)).thenReturn(Arrays.asList(story1, story2));
        
        ModelAndView result = sut.showProjectStories(1L);
        
        assertEquals("project-stories", result.getViewName());
        assertEquals(project, result.getModel().get("project"));
        
        Collection<?> stories = (Collection<?>) result.getModel().get("stories");
        assertEquals(2, stories.size());
        
        verify(storyRepo).findByProject(1L);
    }

    @Test
    @DisplayName("showProjectStories with null project should redirect to home")
    public void testShowProjectStoriesNotFound() {
        when(projectRepo.find(999L)).thenReturn(null);
        
        ModelAndView result = sut.showProjectStories(999L);
        
        assertEquals("redirect:/", result.getViewName());
        verify(projectRepo).find(999L);
    }

    @Test
    @DisplayName("showProject should display project details")
    public void testShowProjectInfo() {
        Project project = new Project();
        project.setId(1);
        project.setName("Test Project");
        
        when(projectRepo.find(1L)).thenReturn(project);
        
        ModelAndView result = sut.showProject(1L);
        
        assertEquals("project-detail", result.getViewName());
        assertEquals(project, result.getModel().get("project"));
    }

    @Test
    @DisplayName("showProject with null project should redirect to list")
    public void testShowProjectNotFound() {
        when(projectRepo.find(999L)).thenReturn(null);
        
        ModelAndView result = sut.showProject(999L);
        
        assertEquals("redirect:/project/list", result.getViewName());
    }

    // ====== Additional edge cases for createProject ======

    @Test
    @DisplayName("createProject with null name should redirect with error")
    public void testCreateProjectNullName() throws IOException {
        String result = sut.createProject(null, "Description");
        
        assertTrue(result.contains("error=Project name is required"));
        verify(projectRepo, never()).persist(any());
    }

    @Test
    @DisplayName("createProject with empty name should redirect with error")
    public void testCreateProjectEmptyName() throws IOException {
        String result = sut.createProject("   ", "Description");
        
        assertTrue(result.contains("error=Project name is required"));
        verify(projectRepo, never()).persist(any());
    }

    @Test
    @DisplayName("createProject with name longer than 29 chars should redirect with error")
    public void testCreateProjectNameTooLong() throws IOException {
        String longName = "A".repeat(30);
        
        String result = sut.createProject(longName, "Description");
        
        assertTrue(result.contains("error=Project name must be less than 29 characters"));
        verify(projectRepo, never()).persist(any());
    }

}
