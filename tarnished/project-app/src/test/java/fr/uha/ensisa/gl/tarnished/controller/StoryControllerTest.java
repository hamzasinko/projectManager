package fr.uha.ensisa.gl.tarnished.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import fr.uha.ensisa.gl.tarnished.repos.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.ArgumentCaptor;
import org.springframework.web.servlet.ModelAndView;

import fr.uha.ensisa.gl.entities.Story;
import fr.uha.ensisa.gl.entities.StoryStatus;
import fr.uha.ensisa.gl.entities.Project;

import java.io.IOException;
import java.util.Collection;
import java.util.Arrays;

/**
 * Tests unitaires avec Mockito pour StoryController
 * Ces tests vérifient le controller indépendamment du repository
 */
public class StoryControllerTest {
    
    @Mock 
    private RepoFactory repoFactory;
    
    @Mock 
    private StoryRepo storyRepo;
    
    @Mock
    private ProjectRepo projectRepo;

    @Mock
    private ColumnRepo columnRepo;

    @Mock
    private UserRepo userRepo;

    private StoryController sut; // System Under Test
    
    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this); // Initialise les @Mock
        
        // Configure le mock pour retourner storyRepo et projectRepo
        when(repoFactory.getStoryRepo()).thenReturn(storyRepo);
        when(repoFactory.getProjectRepo()).thenReturn(projectRepo);
        when(repoFactory.getColumnRepo()).thenReturn(columnRepo);
        when(repoFactory.getUserRepo()).thenReturn(userRepo);

        // Crée le controller et injecte le mock
        sut = new StoryController();
        sut.repoFactory = repoFactory;
    }
    
    @Test
    @DisplayName("showCreateForm should return correct view with projects")
    public void testShowCreateForm() {
        // Mock projects
        when(projectRepo.findAll()).thenReturn(Arrays.asList(new Project()));
        
        ModelAndView result = sut.showCreateForm(null, null);
        
        assertNotNull(result);
        assertEquals("story-create", result.getViewName(), 
                     "Should return story-create view");
        assertTrue(result.getModelMap().containsKey("projects"),
                   "Model should contain projects");
        verify(projectRepo).findAll();
    }
    
    @Test
    @DisplayName("createStory should call persist with valid title and redirect to board")
    public void testCreateStorySuccess() throws IOException {
        String testTitle = "Test Story";
        String testDescription = "Test Description";
        Long projectId = 1L;
        
        // Mock column repository
        fr.uha.ensisa.gl.tarnished.repos.ColumnRepo columnRepo = mock(fr.uha.ensisa.gl.tarnished.repos.ColumnRepo.class);
        when(repoFactory.getColumnRepo()).thenReturn(columnRepo);
        when(columnRepo.findByProject(projectId)).thenReturn(Arrays.asList());

        // Appelle la méthode
        String redirect = sut.createStory(testTitle, testDescription, projectId, null);

        // Vérifie la redirection vers le board du projet
        assertEquals("redirect:/board/" + projectId, redirect,
                     "Should redirect to project board");

        // Vérifie que persist a été appelé avec les bons paramètres
        ArgumentCaptor<Story> storyCaptor = ArgumentCaptor.forClass(Story.class);
        verify(storyRepo).persist(storyCaptor.capture());

        Story capturedStory = storyCaptor.getValue();
        assertEquals(testTitle, capturedStory.getTitle());
        assertEquals(testDescription, capturedStory.getDescription());
        assertEquals(StoryStatus.BACKLOG, capturedStory.getStatus());
        assertNotNull(capturedStory.getDateCreated());
        assertEquals(projectId, capturedStory.getProjectId());
    }
    
    @Test
    @DisplayName("createStory should redirect with error when title is null")
    public void testCreateStoryWithNullTitle() throws IOException {
        String redirect = sut.createStory(null, "Description", 1L, null);
        
        assertTrue(redirect.contains("error"), "Should redirect with error parameter");
        verify(storyRepo, never()).persist(any(Story.class));
    }
    
    @Test
    @DisplayName("createStory should redirect with error when title is empty")
    public void testCreateStoryWithEmptyTitle() throws IOException {
        String redirect = sut.createStory("   ", "Description", 1L, null);
        
        assertTrue(redirect.contains("error"), "Should redirect with error parameter");
        verify(storyRepo, never()).persist(any(Story.class));
    }
    
    @Test
    @DisplayName("createStory should handle null description")
    public void testCreateStoryWithNullDescription() throws IOException {
        String testTitle = "Test Story";
        
        String redirect = sut.createStory(testTitle, null, null, null);

        // Controller now allows creating a story without a project and redirects to the story list
        assertEquals("redirect:/story/list", redirect);
        // Story should have been persisted
        verify(storyRepo).persist(any(Story.class));
    }
    
    @Test
    @DisplayName("listStories should return story-list view")
    public void testListStoriesEmpty() throws IOException {
        // listStories should return the story-list view with stories model
        org.springframework.web.servlet.ModelAndView result = sut.listStories();
        
        assertNotNull(result);
        assertEquals("story-list", result.getViewName(), "Should return story-list view");
        assertTrue(result.getModelMap().containsKey("stories"), "Model should contain stories");
    }
    
    @Test
    @DisplayName("listStories should return story-list view regardless of data")
    public void testListStoriesWithData() throws IOException {
        // listStories should return the story-list view even when data exists
        when(repoFactory.getStoryRepo()).thenReturn(storyRepo);
        when(storyRepo.findAll()).thenReturn(Arrays.asList(new Story()));

        org.springframework.web.servlet.ModelAndView result = sut.listStories();

        assertNotNull(result);
        assertEquals("story-list", result.getViewName(), "Should return story-list view");
        assertTrue(result.getModelMap().containsKey("stories"), "Model should contain stories");
    }
    
    @Test
    @DisplayName("showStory should return story detail view when story exists with projectId")
    public void testShowStoryExists() throws IOException {
        Story story = new Story();
        story.setId(1);
        story.setTitle("Test Story");
        story.setProjectId(1L);

        when(storyRepo.find(1L)).thenReturn(story);
        
        ModelAndView result = sut.showStory(1L);
        
        assertNotNull(result);
        assertEquals("story-detail", result.getViewName());
        assertTrue(result.getModelMap().containsKey("story"));
        
        Story modelStory = (Story) result.getModelMap().get("story");
        assertEquals("Test Story", modelStory.getTitle());
        verify(storyRepo).find(1L);
    }
    
    @Test
    @DisplayName("showStory should redirect to home when story does not exist")
    public void testShowStoryNotFound() throws IOException {
        when(storyRepo.find(999L)).thenReturn(null);
        
        ModelAndView result = sut.showStory(999L);
        
        assertNotNull(result);
        assertEquals("redirect:/", result.getViewName());
        verify(storyRepo).find(999L);
    }
    
    @Test
    @DisplayName("createStory should set default status to BACKLOG")
    public void testCreateStoryDefaultStatus() throws IOException {
        String testTitle = "New Story";
        Long projectId = 1L;

        // Mock column repository
        fr.uha.ensisa.gl.tarnished.repos.ColumnRepo columnRepo = mock(fr.uha.ensisa.gl.tarnished.repos.ColumnRepo.class);
        when(repoFactory.getColumnRepo()).thenReturn(columnRepo);
        when(columnRepo.findByProject(projectId)).thenReturn(Arrays.asList());

        sut.createStory(testTitle, "Description", projectId, null);
        
        ArgumentCaptor<Story> storyCaptor = ArgumentCaptor.forClass(Story.class);
        verify(storyRepo).persist(storyCaptor.capture());
        
        Story capturedStory = storyCaptor.getValue();
        assertEquals(StoryStatus.BACKLOG, capturedStory.getStatus(),
                     "New story should have BACKLOG status by default");
    }
    
    @Test
    @DisplayName("createStory should set creation date")
    public void testCreateStoryCreationDate() throws IOException {
        String testTitle = "New Story";
        Long projectId = 1L;

        // Mock column repository
        fr.uha.ensisa.gl.tarnished.repos.ColumnRepo columnRepo = mock(fr.uha.ensisa.gl.tarnished.repos.ColumnRepo.class);
        when(repoFactory.getColumnRepo()).thenReturn(columnRepo);
        when(columnRepo.findByProject(projectId)).thenReturn(Arrays.asList());

        sut.createStory(testTitle, "Description", projectId, null);
        
        ArgumentCaptor<Story> storyCaptor = ArgumentCaptor.forClass(Story.class);
        verify(storyRepo).persist(storyCaptor.capture());
        
        Story capturedStory = storyCaptor.getValue();
        assertNotNull(capturedStory.getDateCreated(),
                      "Story should have creation date set");
    }

    @Test
    @DisplayName("editStory should return view with story and available users")
    public void testEditStory() throws IOException {
        // Given
        long storyId = 1L;
        Story mockStory = mock(Story.class);
        when(mockStory.getId()).thenReturn((int) storyId);
        when(mockStory.getProjectId()).thenReturn(1L);
        when(mockStory.getColumnId()).thenReturn(null);

        when(storyRepo.find(storyId)).thenReturn(mockStory);
        when(repoFactory.getUserRepo()).thenReturn(mock(fr.uha.ensisa.gl.tarnished.repos.UserRepo.class));
        when(repoFactory.getColumnRepo()).thenReturn(mock(fr.uha.ensisa.gl.tarnished.repos.ColumnRepo.class));

        // When
        ModelAndView result = sut.editStory(storyId);
        
        // Then
        assertNotNull(result);
        assertEquals("story-edit", result.getViewName());
        assertEquals(mockStory, result.getModelMap().get("story"));
        assertTrue(result.getModelMap().containsKey("users"));
        
        verify(storyRepo).find(storyId);
    }

    @Test
    @DisplayName("updateStory should update story and redirect to board")
    public void testUpdateStory() throws IOException {
        // Given
        long storyId = 1L;
        String newTitle = "Updated Title";
        String newDescription = "Updated Description";
        String newStatus = "IN_PROGRESS";
        
        Story mockStory = mock(Story.class);
        when(mockStory.getProjectId()).thenReturn(1L);
        when(mockStory.getColumnId()).thenReturn(null);
        when(storyRepo.find(storyId)).thenReturn(mockStory);
        when(repoFactory.getColumnRepo()).thenReturn(mock(fr.uha.ensisa.gl.tarnished.repos.ColumnRepo.class));

        // When
        String result = sut.updateStory(storyId, newTitle, newDescription, newStatus);
        
        // Then
        assertEquals("redirect:/board/1", result);
        
        verify(mockStory).setTitle(newTitle);
        verify(mockStory).setDescription(newDescription);
        verify(mockStory).setStatus(StoryStatus.valueOf(newStatus));
    }

    @Test
    @DisplayName("deleteStory should call remove on repository and redirect to board")
    public void testDeleteStory() {
        // Given
        long storyId = 1L;
        Story mockStory = new Story();
        mockStory.setProjectId(1L);
        when(storyRepo.find(storyId)).thenReturn(mockStory);

        // When
        String result = sut.deleteStory(storyId);
        
        // Then
        assertEquals("redirect:/board/1", result);
        verify(storyRepo).remove(storyId);
    }

    @Test
    @DisplayName("assignStory should assign user to story and redirect to board")
    public void testAssignStory() throws IOException {
        // Given
        long storyId = 1L;
        int userId = 10;
        
        Story mockStory = mock(Story.class);
        when(mockStory.getProjectId()).thenReturn(1L);
        fr.uha.ensisa.gl.entities.User mockUser = mock(fr.uha.ensisa.gl.entities.User.class);
        
        when(storyRepo.find(storyId)).thenReturn(mockStory);
        fr.uha.ensisa.gl.tarnished.repos.UserRepo userRepo = mock(fr.uha.ensisa.gl.tarnished.repos.UserRepo.class);
        when(repoFactory.getUserRepo()).thenReturn(userRepo);
        when(userRepo.find(userId)).thenReturn(mockUser);
        
        // When
        String result = sut.assignStory(storyId, userId);
        
        // Then
        assertEquals("redirect:/board/1", result);
        verify(mockStory).setUserAssigned(mockUser);
        verify(storyRepo).persist(mockStory);
    }

    @Test
    @DisplayName("unassignStory should remove user assignment and redirect to board")
    public void testUnassignStory() throws IOException {
        // Given
        long storyId = 1L;
        
        Story mockStory = mock(Story.class);
        when(mockStory.getProjectId()).thenReturn(1L);
        when(storyRepo.find(storyId)).thenReturn(mockStory);
        
        // When
        String result = sut.unassignStory(storyId);
        
        // Then
        assertEquals("redirect:/board/1", result);
        verify(mockStory).setUserAssigned(null);
        verify(storyRepo).persist(mockStory);
    }
}
