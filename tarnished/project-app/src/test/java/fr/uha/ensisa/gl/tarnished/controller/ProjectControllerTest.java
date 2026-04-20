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

import fr.uha.ensisa.gl.tarnished.config.PathHelper;
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
    
    @Mock
    private PathHelper pathHelper;
    
    private ProjectController sut; // System Under Test
    
    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this); // Initialise les @Mock
        
        //Configure le mock pour retourner projectRepo
        when(repoFactory.getProjectRepo()).thenReturn(projectRepo);
        when(repoFactory.getUserRepo()).thenReturn(userRepo);
        User testUser = new User();
        testUser.setId(1);
        testUser.setName("user1");
        testUser.setEmail("email1@gmail.com");
        testUser.setPassword("password1");
        when(userRepo.getAll()).thenReturn(List.of(testUser));
        
        //Crée le controller et injecte le mock
        sut = new ProjectController();
        sut.repoFactory = repoFactory;
        sut.setPathHelper(pathHelper);
        
        // Configure PathHelper mock pour retourner les redirections standard
        when(pathHelper.redirect(anyString())).thenAnswer(invocation -> {
            String path = invocation.getArgument(0);
            return "redirect:" + path;
        });
        when(pathHelper.redirectView(anyString())).thenAnswer(invocation -> {
            String path = invocation.getArgument(0);
            return new ModelAndView("redirect:" + path);
        });
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
        
        //Mock UserRepo
        User mockUser = new User();
        mockUser.setId(1);
        when(repoFactory.getUserRepo()).thenReturn(userRepo);
        when(userRepo.getAll()).thenReturn(Arrays.asList(mockUser));
        
        //Appelle la méthode
        String redirect = sut.createProject(testName, testDescription);
        
        //Vérifie la redirection
        assertEquals("redirect:/project/list", redirect, 
                     "Should redirect to project list");
        

        //Vérifie que persist a été appelé avec les bons paramètres
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
        
        //Mock UserRepo
        User mockUser = new User();
        mockUser.setId(1);
        when(repoFactory.getUserRepo()).thenReturn(userRepo);
        when(userRepo.getAll()).thenReturn(Arrays.asList(mockUser));
        
        String redirect = sut.createProject(testName, null);
        
        assertEquals("redirect:/project/list", redirect);
        

        verify(projectRepo).persist(any(Project.class));
    }

    @Test
    @DisplayName("createProject should accept name with exactly 29 characters")
    public void testCreateProjectWith29Characters() throws IOException {
        String name29Chars = "12345678901234567890123456789"; // exactly 29 chars
        
        User mockUser = new User();
        mockUser.setId(1);
        when(repoFactory.getUserRepo()).thenReturn(userRepo);
        when(userRepo.getAll()).thenReturn(Arrays.asList(mockUser));
        
        String redirect = sut.createProject(name29Chars, "Description");
        
        assertEquals("redirect:/project/list", redirect);
        verify(projectRepo).persist(any(Project.class));
    }

    @Test
    @DisplayName("createProject should reject name with exactly 30 characters")
    public void testCreateProjectWith30Characters() throws IOException {
        String name30Chars = "123456789012345678901234567890"; // exactly 30 chars
        
        String redirect = sut.createProject(name30Chars, "Description");
        
        assertTrue(redirect.contains("error"));
        verify(projectRepo, never()).persist(any(Project.class));
    }

    @Test
    @DisplayName("createProject should create default user when userRepo is empty")
    public void testCreateProjectCreatesDefaultUser() throws IOException {
        User defaultUser = new User();
        defaultUser.setId(1);
        defaultUser.setName("user1");
        defaultUser.setPassword("password1");
        defaultUser.setEmail("email1@gmail.com");
        
        // First call returns empty list, second call (after add) returns list with user
        when(userRepo.getAll())
            .thenReturn(new ArrayList<>())
            .thenReturn(Arrays.asList(defaultUser));
        
        org.mockito.ArgumentCaptor<User> userCaptor = org.mockito.ArgumentCaptor.forClass(User.class);
        
        String redirect = sut.createProject("Test Project", "Description");
        
        assertEquals("redirect:/project/list", redirect);
        verify(userRepo).add(userCaptor.capture());
        
        User capturedUser = userCaptor.getValue();
        assertEquals(1, capturedUser.getId());
        assertEquals("user1", capturedUser.getName());
        assertEquals("password1", capturedUser.getPassword());
        assertEquals("email1@gmail.com", capturedUser.getEmail());
        verify(projectRepo).persist(any(Project.class));
    }
    
    @Test
    @DisplayName("listProjects should return view with empty list when mocked")
    public void testListProjectsEmpty() throws IOException {
        //Configure le mock pour retourner une liste vide
        when(projectRepo.findAll()).thenReturn(Arrays.asList());
        
        ModelAndView result = sut.listProjects();
        
        assertNotNull(result);
        assertEquals("project-list", result.getViewName());
        
        //Vérifie que projects est dans le modèle
        assertTrue(result.getModelMap().containsKey("projects"), 
                   "Model should contain 'projects' attribute");
        
        Collection<?> projects = (Collection<?>) result.getModelMap().get("projects");
        assertNotNull(projects);

        verify(projectRepo).findAll();
    }
    
    @Test
    @DisplayName("listProjects should return view with projects when they exist")
    public void testListProjectsWithData() throws IOException {
        //Crée des projets mock
        Project p1 = mock(Project.class);
        when(p1.getName()).thenReturn("Project 1");
        when(p1.getId()).thenReturn(1);
        
        Project p2 = mock(Project.class);
        when(p2.getName()).thenReturn("Project 2");
        when(p2.getId()).thenReturn(2);
        
        //Configure le mock pour retourner ces projets
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
    @DisplayName("editProject should create default user when userRepo is empty")
    void testEditProjectCreatesDefaultUser() {
        Project p = new Project();
        p.setId(1);
        p.setMembers(new ArrayList<>());

        User defaultUser = new User();
        defaultUser.setId(1);
        defaultUser.setName("user1");
        defaultUser.setPassword("password1");
        defaultUser.setEmail("email1@gmail.com");

        when(repoFactory.getProjectRepo()).thenReturn(projectRepo);
        when(repoFactory.getUserRepo()).thenReturn(userRepo);
        when(projectRepo.find(1L)).thenReturn(p);
        
        // First call returns empty, second call (after add) returns list with user
        when(userRepo.getAll())
            .thenReturn(new ArrayList<>())
            .thenReturn(Arrays.asList(defaultUser));

        org.mockito.ArgumentCaptor<User> userCaptor = org.mockito.ArgumentCaptor.forClass(User.class);
        
        ModelAndView mav = sut.editProject(1L);

        assertEquals("project-edit", mav.getViewName());
        verify(userRepo).add(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertEquals(1, capturedUser.getId());
        assertEquals("user1", capturedUser.getName());
        assertEquals("password1", capturedUser.getPassword());
        assertEquals("email1@gmail.com", capturedUser.getEmail());
    }

    @Test
    @DisplayName("deleteProject should call remove on repository and redirect")
    void testDeleteProject() {
        //Given
        long projectId = 1L;

        //Make repoFactory return the mocked repositories
        fr.uha.ensisa.gl.tarnished.repos.StoryRepo mockStoryRepo = mock(fr.uha.ensisa.gl.tarnished.repos.StoryRepo.class);
        fr.uha.ensisa.gl.tarnished.repos.ColumnRepo mockColumnRepo = mock(fr.uha.ensisa.gl.tarnished.repos.ColumnRepo.class);
        
        when(repoFactory.getProjectRepo()).thenReturn(projectRepo);
        when(repoFactory.getStoryRepo()).thenReturn(mockStoryRepo);
        when(repoFactory.getColumnRepo()).thenReturn(mockColumnRepo);
        
        //Mock empty lists for cascade delete
        when(mockStoryRepo.findByProject(projectId)).thenReturn(List.of());
        when(mockColumnRepo.findByProject(projectId)).thenReturn(List.of());

        //When: call the controller method directly
        String result = sut.deleteProject(projectId);

        //Then: verify the redirection string
        assertEquals("redirect:/project/list", result, "Should redirect to project list");

        //And: verify that remove was called on the repository with the correct ID
        verify(projectRepo).remove(projectId);
    }

    @Test
    @DisplayName("ProjectController.info should return correct project and view")
    void testProjectInfo() {
        //Arrange
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

        //Act
        ModelAndView mav = sut.showProject((long) projectId);

        //Assert
        assertEquals("project-detail", mav.getViewName(), "Should return project-info view");
        Project projectInModel = (Project) mav.getModel().get("project");
        assertEquals("Test Project", projectInModel.getName());
        assertEquals("Description", projectInModel.getDescription());
    }

    @Test
    @DisplayName("showProjectStories should return view with stories")
    void testShowProjectStories() {
        long projectId = 1L;

        Project project = new Project();
        project.setId((int) projectId);
        project.setName("Test Project");

        fr.uha.ensisa.gl.tarnished.repos.StoryRepo storyRepo = mock(fr.uha.ensisa.gl.tarnished.repos.StoryRepo.class);
        when(projectRepo.find(projectId)).thenReturn(project);
        when(repoFactory.getStoryRepo()).thenReturn(storyRepo);
        when(storyRepo.findByProject(projectId)).thenReturn(List.of());

        ModelAndView mav = sut.showProjectStories(projectId);

        assertEquals("project-stories", mav.getViewName());
        assertEquals(project, mav.getModel().get("project"));
    }

    @Test
    @DisplayName("createProject should handle empty name")
    void testCreateProjectWithEmptyName() throws IOException {
        String emptyName = "";
        String description = "Description";

        String result = sut.createProject(emptyName, description);

        assertTrue(result.contains("error") || result.contains("redirect"));
        verify(projectRepo, never()).persist(any(Project.class));
    }

    @Test
    @DisplayName("updateProject should redirect to project info")
    void testUpdateProjectRedirection() {
        long projectId = 1L;
        String name = "Updated";
        String description = "Desc";

        Project project = new Project();
        project.setId((int) projectId);

        when(projectRepo.find(projectId)).thenReturn(project);
        when(userRepo.getAll()).thenReturn(List.of());

        String result = sut.updateProject(projectId, name, description, List.of());

        assertEquals("redirect:/project/info/" + projectId, result);
        verify(projectRepo).update(project);
    }

    @Test
    @DisplayName("updateProject should handle empty name")
    void testUpdateProjectEmptyName() {
        long projectId = 1L;

        String result = sut.updateProject(projectId, "", "Description", List.of());

        assertTrue(result.contains("error"));
        verify(projectRepo, never()).update(any(Project.class));
    }

    @Test
    @DisplayName("updateProject should handle null name")
    void testUpdateProjectNullName() {
        long projectId = 1L;

        String result = sut.updateProject(projectId, null, "Description", List.of());

        assertTrue(result.contains("error"));
        verify(projectRepo, never()).update(any(Project.class));
    }

    @Test
    @DisplayName("updateProject should handle long name")
    void testUpdateProjectLongName() {
        long projectId = 1L;
        String longName = "This is a very long project name that exceeds the limit of 29 characters";

        String result = sut.updateProject(projectId, longName, "Description", List.of());

        assertTrue(result.contains("error"));
        verify(projectRepo, never()).update(any(Project.class));
    }

    @Test
    @DisplayName("updateProject should handle null project")
    void testUpdateProjectNotFound() {
        long projectId = 999L;

        when(projectRepo.find(projectId)).thenReturn(null);

        String result = sut.updateProject(projectId, "Name", "Description", List.of());

        assertEquals("redirect:/project/list", result);
        verify(projectRepo, never()).update(any(Project.class));
    }

    @Test
    @DisplayName("updateProject should set members from memberIds")
    void testUpdateProjectWithMembers() {
        long projectId = 1L;

        Project project = new Project();
        project.setId((int) projectId);

        User user1 = new User();
        user1.setId(10);
        User user2 = new User();
        user2.setId(20);

        when(projectRepo.find(projectId)).thenReturn(project);
        when(userRepo.getAll()).thenReturn(List.of(user1, user2));

        String result = sut.updateProject(projectId, "Name", "Description", List.of(10L, 20L));

        assertEquals("redirect:/project/info/" + projectId, result);
        assertEquals(2, project.getMembers().size());
        verify(projectRepo).update(project);
    }

    @Test
    @DisplayName("updateProject should clear members when memberIds is empty")
    void testUpdateProjectClearMembers() {
        long projectId = 1L;

        Project project = new Project();
        project.setId((int) projectId);

        when(projectRepo.find(projectId)).thenReturn(project);
        when(userRepo.getAll()).thenReturn(List.of());

        String result = sut.updateProject(projectId, "Name", "Description", List.of());

        assertEquals("redirect:/project/info/" + projectId, result);
        assertEquals(0, project.getMembers().size());
        verify(projectRepo).update(project);
    }

    @Test
    @DisplayName("updateProject should clear members when memberIds is null")
    void testUpdateProjectNullMembers() {
        long projectId = 1L;

        Project project = new Project();
        project.setId((int) projectId);

        when(projectRepo.find(projectId)).thenReturn(project);
        when(userRepo.getAll()).thenReturn(List.of());

        String result = sut.updateProject(projectId, "Name", "Description", null);

        assertEquals("redirect:/project/info/" + projectId, result);
        assertEquals(0, project.getMembers().size());
        verify(projectRepo).update(project);
    }

    @Test
    @DisplayName("editProject should return view with members")
    void testEditProjectWithMembers() {
        long projectId = 1L;

        Project project = new Project();
        project.setId((int) projectId);
        project.setName("Test Project");

        User user1 = new User();
        user1.setId(10);

        project.setMembers(List.of(user1));

        when(projectRepo.find(projectId)).thenReturn(project);
        when(userRepo.getAll()).thenReturn(List.of(user1));

        ModelAndView mav = sut.editProject(projectId);

        assertEquals("project-edit", mav.getViewName());
        assertEquals(project, mav.getModel().get("project"));
    }

    @Test
    @DisplayName("showProjectStories should handle null project")
    void testShowProjectStoriesNullProject() {
        long projectId = 999L;

        when(projectRepo.find(projectId)).thenReturn(null);

        ModelAndView mav = sut.showProjectStories(projectId);

        assertEquals("redirect:/", mav.getViewName());
    }

    @Test
    @DisplayName("editProject should handle null project")
    void testEditProject_WithNullProject() {
        Long projectId = 999L;
        
        when(projectRepo.find(projectId)).thenReturn(null);
        
        ModelAndView mav = sut.editProject(projectId);
        
        assertEquals("redirect:/project/list", mav.getViewName());
    }

    @Test
    @DisplayName("editProject should handle project with null members")
    void testEditProject_WithNullMembers() {
        Long projectId = 1L;
        Project project = new Project();
        project.setId(1);
        project.setMembers(null);
        
        when(projectRepo.find(projectId)).thenReturn(project);
        when(userRepo.getAll()).thenReturn(List.of());
        
        ModelAndView mav = sut.editProject(projectId);
        
        assertEquals("project-edit", mav.getViewName());
        List<Integer> memberIds = (List<Integer>) mav.getModel().get("memberIds");
        assertTrue(memberIds.isEmpty(), "memberIds should be empty when members is null");
    }

    @Test
    @DisplayName("showProject should redirect when project not found")
    void testShowProjectNotFound() {
        long projectId = 999L;

        when(projectRepo.find(projectId)).thenReturn(null);

        ModelAndView mav = sut.showProject(projectId);

        assertEquals("redirect:/project/list", mav.getViewName());
    }

    @Test
    @DisplayName("deleteProject should delete stories and columns before project")
    void testDeleteProjectWithStoriesAndColumns() {
        long projectId = 1L;

        fr.uha.ensisa.gl.entities.Story story1 = new fr.uha.ensisa.gl.entities.Story();
        story1.setId(10);
        fr.uha.ensisa.gl.entities.Story story2 = new fr.uha.ensisa.gl.entities.Story();
        story2.setId(20);

        fr.uha.ensisa.gl.entities.Column col1 = new fr.uha.ensisa.gl.entities.Column();
        col1.setId(1);
        fr.uha.ensisa.gl.entities.Column col2 = new fr.uha.ensisa.gl.entities.Column();
        col2.setId(2);

        fr.uha.ensisa.gl.tarnished.repos.StoryRepo mockStoryRepo = mock(fr.uha.ensisa.gl.tarnished.repos.StoryRepo.class);
        fr.uha.ensisa.gl.tarnished.repos.ColumnRepo mockColumnRepo = mock(fr.uha.ensisa.gl.tarnished.repos.ColumnRepo.class);

        when(repoFactory.getStoryRepo()).thenReturn(mockStoryRepo);
        when(repoFactory.getColumnRepo()).thenReturn(mockColumnRepo);
        when(mockStoryRepo.findByProject(projectId)).thenReturn(List.of(story1, story2));
        when(mockColumnRepo.findByProject(projectId)).thenReturn(List.of(col1, col2));

        String result = sut.deleteProject(projectId);

        assertEquals("redirect:/project/list", result);
        verify(mockStoryRepo).remove(10L);
        verify(mockStoryRepo).remove(20L);
        verify(mockColumnRepo).remove(1L);
        verify(mockColumnRepo).remove(2L);
        verify(projectRepo).remove(projectId);
    }

    @Test
    @DisplayName("createProject should set owner when provided")
    void testCreateProjectWithOwner() throws IOException {
        String name = "Test Project";
        String description = "Description";

        User owner = new User();
        owner.setId(1);
        owner.setName("Owner");

        when(userRepo.getAll()).thenReturn(List.of(owner));

        String result = sut.createProject(name, description);

        assertEquals("redirect:/project/list", result);

        ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepo).persist(projectCaptor.capture());

        Project captured = projectCaptor.getValue();
        assertNotNull(captured.getOwner());
    }

    @Test
    @DisplayName("createProject should handle null name")
    void testCreateProjectWithNullName() throws IOException {
        String result = sut.createProject(null, "Description");
        
        assertTrue(result.contains("error") || result.contains("redirect"));
        verify(projectRepo, never()).persist(any(Project.class));
    }

    @Test
    @DisplayName("createProject should handle name too long")
    void testCreateProjectWithNameTooLong() throws IOException {
        String longName = "This is a very long project name that exceeds 29 characters";
        String result = sut.createProject(longName, "Description");
        
        assertTrue(result.contains("error"));
        verify(projectRepo, never()).persist(any(Project.class));
    }

    @Test
    @DisplayName("createProject should create default user when userRepo is empty")
    void testCreateProjectWithEmptyUserRepo() throws IOException {
        User defaultUser = new User();
        defaultUser.setId(1);
        defaultUser.setName("user1");
        defaultUser.setEmail("email1@gmail.com");
        defaultUser.setPassword("password1");
        
        //First call (isEmpty check) returns empty list
        //Second call (get(0)) returns list with the added user
        when(userRepo.getAll())
            .thenReturn(new ArrayList<>())  // First call - empty for isEmpty()
            .thenReturn(List.of(defaultUser)); // Second call - with user for get(0)
        
        String result = sut.createProject("Test Project", "Description");
        
        assertEquals("redirect:/project/list", result);
        verify(userRepo).add(any(User.class));
        verify(projectRepo).persist(any(Project.class));
    }

    @Test
    @DisplayName("editProject should create default user when userRepo is empty")
    void testEditProjectWithEmptyUserRepo() {
        Project project = new Project();
        project.setId(1);
        project.setMembers(new ArrayList<>());
        
        User defaultUser = new User();
        defaultUser.setId(1);
        defaultUser.setName("user1");
        defaultUser.setEmail("email1@gmail.com");
        defaultUser.setPassword("password1");
        
        when(projectRepo.find(1L)).thenReturn(project);
        //First call (isEmpty check) returns empty, second call (addObject) returns user after add
        when(userRepo.getAll())
            .thenReturn(new ArrayList<>())  // First call - empty for isEmpty()
            .thenReturn(List.of(defaultUser)); // Second call - with user for addObject
        
        ModelAndView mav = sut.editProject(1L);
        
        assertEquals("project-edit", mav.getViewName());
        verify(userRepo).add(any(User.class));
    }

    @Test
    @DisplayName("editProject should handle project with null members")
    void testEditProjectWithNullMembers() {
        Project project = new Project();
        project.setId(1);
        project.setMembers(null);
        
        User user = new User();
        user.setId(10);
        
        when(projectRepo.find(1L)).thenReturn(project);
        when(userRepo.getAll()).thenReturn(List.of(user));
        
        ModelAndView mav = sut.editProject(1L);
        
        assertEquals("project-edit", mav.getViewName());
        List<Integer> memberIds = (List<Integer>) mav.getModel().get("memberIds");
        assertTrue(memberIds.isEmpty());
    }

}
