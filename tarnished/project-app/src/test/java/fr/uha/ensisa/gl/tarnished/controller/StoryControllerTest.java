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
import java.util.Arrays;
import java.util.ArrayList;

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
        
        assertEquals("redirect:/story/new?error=Project is required", redirect);
        verify(storyRepo, never()).persist(any(Story.class));
    }
    
    @Test
    @DisplayName("listStories should redirect to home")
    public void testListStoriesEmpty() throws IOException {
        // listStories now redirects to home as stories should be accessed via project boards
        String result = sut.listStories();
        
        assertNotNull(result);
        assertEquals("redirect:/", result, "Should redirect to home page");
    }
    
    @Test
    @DisplayName("listStories should redirect to home regardless of data")
    public void testListStoriesWithData() throws IOException {
        // listStories now always redirects to home
        String result = sut.listStories();
        
        assertNotNull(result);
        assertEquals("redirect:/", result, "Should redirect to home page");
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
        when(mockUser.getName()).thenReturn("Test User");
        when(mockUser.getId()).thenReturn(userId);
        
        when(storyRepo.find(storyId)).thenReturn(mockStory);
        when(mockStory.getUserAssigned()).thenReturn(mockUser);
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

    @Test
    @DisplayName("showCreateForm with projectId should include it in model")
    public void testShowCreateFormWithProjectId() {
        when(projectRepo.findAll()).thenReturn(Arrays.asList(new Project()));
        
        ModelAndView result = sut.showCreateForm(123L, null);
        
        assertEquals(123L, result.getModel().get("projectId"));
    }

    @Test
    @DisplayName("showCreateForm with columnId should include it in model")
    public void testShowCreateFormWithColumnId() {
        when(projectRepo.findAll()).thenReturn(Arrays.asList(new Project()));
        
        ModelAndView result = sut.showCreateForm(null, 456L);
        
        assertEquals(456L, result.getModel().get("columnId"));
    }

    @Test
    @DisplayName("createStory should trim title")
    public void testCreateStoryTrimTitle() throws IOException {
        when(columnRepo.findByProject(1L)).thenReturn(Arrays.asList());
        
        sut.createStory("  Title  ", "desc", 1L, null);
        
        verify(storyRepo).persist(argThat(story -> "Title".equals(story.getTitle())));
    }

    @Test
    @DisplayName("createStory with title length 59 should succeed")
    public void testCreateStoryTitleLength59() throws IOException {
        String title = "A".repeat(59);
        when(columnRepo.findByProject(1L)).thenReturn(Arrays.asList());
        
        String result = sut.createStory(title, "desc", 1L, null);
        
        assertEquals("redirect:/board/1", result);
    }

    @Test
    @DisplayName("showStory should return story detail view")
    public void testShowStorySuccess() throws IOException {
        Story story = new Story();
        story.setProjectId(1L);
        when(storyRepo.find(1L)).thenReturn(story);
        
        ModelAndView result = sut.showStory(1L);
        
        assertEquals("story-detail", result.getViewName());
        assertEquals(story, result.getModel().get("story"));
    }

    @Test
    @DisplayName("editStory should include users in model")
    public void testEditStoryIncludesUsers() {
        Story story = new Story();
        story.setProjectId(1L);
        when(storyRepo.find(1L)).thenReturn(story);
        when(userRepo.getAll()).thenReturn(Arrays.asList(new fr.uha.ensisa.gl.entities.User()));
        
        ModelAndView result = sut.editStory(1L);
        
        assertTrue(result.getModel().containsKey("users"));
    }

    @Test
    @DisplayName("updateStory with null title should return error")
    public void testUpdateStoryNullTitle() {
        Story story = new Story();
        story.setProjectId(1L);
        when(storyRepo.find(1L)).thenReturn(story);
        
        String result = sut.updateStory(1L, null, "desc", null);
        
        assertTrue(result.contains("error=Title is required"));
    }

    @Test
    @DisplayName("updateStory with empty title should return error")
    public void testUpdateStoryEmptyTitle() {
        Story story = new Story();
        story.setProjectId(1L);
        when(storyRepo.find(1L)).thenReturn(story);
        
        String result = sut.updateStory(1L, "   ", "desc", null);
        
        assertTrue(result.contains("error=Title is required"));
    }

    @Test
    @DisplayName("updateStory with title too long should return error")
    public void testUpdateStoryTitleTooLong() {
        Story story = new Story();
        story.setProjectId(1L);
        when(storyRepo.find(1L)).thenReturn(story);
        String longTitle = "A".repeat(60);
        
        String result = sut.updateStory(1L, longTitle, "desc", null);
        
        assertTrue(result.contains("error=Title must be less than 59"));
    }

    @Test
    @DisplayName("updateStory with non-existent story should redirect to list")
    public void testUpdateStoryNotFound() {
        when(storyRepo.find(999L)).thenReturn(null);
        
        String result = sut.updateStory(999L, "Title", "desc", null);
        
        assertEquals("redirect:/story/list", result);
    }

    @Test
    @DisplayName("updateStory should trim title and redirect to board")
    public void testUpdateStorySuccess() {
        Story story = new Story();
        story.setProjectId(1L);
        when(storyRepo.find(1L)).thenReturn(story);
        
        String result = sut.updateStory(1L, "  NewTitle  ", "NewDesc", null);
        
        assertEquals("NewTitle", story.getTitle());
        assertEquals("NewDesc", story.getDescription());
        verify(storyRepo).persist(story);
        assertEquals("redirect:/board/1", result);
    }

    @Test
    @DisplayName("updateStory without projectId should redirect to story detail")
    public void testUpdateStoryNoProjectId() {
        Story story = new Story();
        story.setProjectId(null);
        when(storyRepo.find(1L)).thenReturn(story);
        
        String result = sut.updateStory(1L, "Title", "desc", null);
        
        assertEquals("redirect:/story/1", result);
    }

    @Test
    @DisplayName("updateStory in default column should not change status")
    public void testUpdateStoryInDefaultColumn() {
        Story story = new Story();
        story.setProjectId(1L);
        story.setColumnId(5L);
        story.setStatus(StoryStatus.BACKLOG);
        
        fr.uha.ensisa.gl.entities.Column column = new fr.uha.ensisa.gl.entities.Column();
        column.setName("BACKLOG");
        
        when(storyRepo.find(1L)).thenReturn(story);
        when(columnRepo.find(5L)).thenReturn(column);
        
        String result = sut.updateStory(1L, "Title", "desc", "DONE");
        
        assertEquals(StoryStatus.BACKLOG, story.getStatus());
    }

    @Test
    @DisplayName("updateStory in custom column should allow status change")
    public void testUpdateStoryInCustomColumn() {
        Story story = new Story();
        story.setProjectId(1L);
        story.setColumnId(5L);
        story.setStatus(StoryStatus.BACKLOG);
        
        fr.uha.ensisa.gl.entities.Column column = new fr.uha.ensisa.gl.entities.Column();
        column.setName("CUSTOM");
        
        when(storyRepo.find(1L)).thenReturn(story);
        when(columnRepo.find(5L)).thenReturn(column);
        
        String result = sut.updateStory(1L, "Title", "desc", "DONE");
        
        assertEquals(StoryStatus.DONE, story.getStatus());
    }

    @Test
    @DisplayName("updateStory with invalid status should keep current status")
    public void testUpdateStoryInvalidStatus() {
        Story story = new Story();
        story.setProjectId(1L);
        story.setColumnId(5L);
        story.setStatus(StoryStatus.BACKLOG);
        
        fr.uha.ensisa.gl.entities.Column column = new fr.uha.ensisa.gl.entities.Column();
        column.setName("CUSTOM");
        
        when(storyRepo.find(1L)).thenReturn(story);
        when(columnRepo.find(5L)).thenReturn(column);
        
        String result = sut.updateStory(1L, "Title", "desc", "INVALID");
        
        assertEquals(StoryStatus.BACKLOG, story.getStatus());
    }

    @Test
    @DisplayName("deleteStory should redirect to board if story has project")
    public void testDeleteStoryWithProject() {
        Story story = new Story();
        story.setProjectId(1L);
        when(storyRepo.find(1L)).thenReturn(story);
        
        String result = sut.deleteStory(1L);
        
        verify(storyRepo).remove(1L);
        assertEquals("redirect:/board/1", result);
    }

    @Test
    @DisplayName("deleteStory should redirect to home if no project")
    public void testDeleteStoryWithoutProject() {
        Story story = new Story();
        story.setProjectId(null);
        when(storyRepo.find(1L)).thenReturn(story);
        
        String result = sut.deleteStory(1L);
        
        verify(storyRepo).remove(1L);
        assertEquals("redirect:/", result);
    }

    @Test
    @DisplayName("deleteStory with null story should redirect to home")
    public void testDeleteStoryNotFound() {
        when(storyRepo.find(999L)).thenReturn(null);
        
        String result = sut.deleteStory(999L);
        
        verify(storyRepo).remove(999L);
        assertEquals("redirect:/", result);
    }

    @Test
    @DisplayName("assignStory with non-existent user should not assign")
    public void testAssignStoryUserNotFound() throws IOException {
        Story story = new Story();
        story.setProjectId(1L);
        when(storyRepo.find(1L)).thenReturn(story);
        when(userRepo.find(999)).thenReturn(null);
        
        String result = sut.assignStory(1L, 999);
        
        verify(storyRepo, never()).persist(story);
        assertEquals("redirect:/board/1", result);
    }

    @Test
    @DisplayName("assignStory with null story should redirect to home")
    public void testAssignStoryNotFound() throws IOException {
        when(storyRepo.find(999L)).thenReturn(null);
        
        String result = sut.assignStory(999L, 1);
        
        assertEquals("redirect:/", result);
    }

    @Test
    @DisplayName("unassignStory with null story should redirect to home")
    public void testUnassignStoryNotFound() throws IOException {
        when(storyRepo.find(999L)).thenReturn(null);
        
        String result = sut.unassignStory(999L);
        
        assertEquals("redirect:/", result);
    }

    @Test
    @DisplayName("unassignStory without project should redirect to home")
    public void testUnassignStoryNoProject() throws IOException {
        Story story = mock(Story.class);
        when(story.getProjectId()).thenReturn(null);
        when(storyRepo.find(1L)).thenReturn(story);
        
        String result = sut.unassignStory(1L);
        
        verify(story).setUserAssigned(null);
        assertEquals("redirect:/", result);
    }

    @Test
    @DisplayName("startTimer should call repo and redirect")
    public void testStartTimer() {
        String result = sut.startTimer(1L, 10L);
        
        verify(storyRepo).startTimer(1L, 10L);
        assertEquals("redirect:/story/list", result);
    }

    @Test
    @DisplayName("startTimer with default userId")
    public void testStartTimerDefaultUser() {
        String result = sut.startTimer(1L, 1L);
        
        verify(storyRepo).startTimer(1L, 1L);
        assertEquals("redirect:/story/list", result);
    }

    @Test
    @DisplayName("stopTimer should call repo and redirect")
    public void testStopTimer() {
        String result = sut.stopTimer(1L, 5L);
        
        verify(storyRepo).stopTimer(1L, 5L);
        assertEquals("redirect:/story/list", result);
    }

    // ====== NEW TESTS FOR showStory edge cases ======

    @Test
    @DisplayName("showStory without projectId should redirect to home")
    public void testShowStoryWithoutProjectId() throws IOException {
        Story story = new Story();
        story.setId(1);
        story.setProjectId(null);
        
        when(storyRepo.find(1L)).thenReturn(story);
        
        ModelAndView result = sut.showStory(1L);
        
        assertEquals("redirect:/", result.getViewName());
    }

    // ====== NEW TESTS FOR editStory edge cases ======

    @Test
    @DisplayName("editStory without projectId should redirect to home")
    public void testEditStoryWithoutProjectId() {
        Story story = new Story();
        story.setId(1);
        story.setProjectId(null);
        
        when(storyRepo.find(1L)).thenReturn(story);
        
        ModelAndView result = sut.editStory(1L);
        
        assertEquals("redirect:/", result.getViewName());
    }

    @Test
    @DisplayName("editStory with null story should redirect to home")
    public void testEditStoryNotFound() {
        when(storyRepo.find(999L)).thenReturn(null);
        
        ModelAndView result = sut.editStory(999L);
        
        assertEquals("redirect:/", result.getViewName());
    }

    @Test
    @DisplayName("editStory with column should include column in model")
    public void testEditStoryWithColumn() {
        Story story = new Story();
        story.setId(1);
        story.setProjectId(5L);
        story.setColumnId(10L);
        
        fr.uha.ensisa.gl.entities.Column column = new fr.uha.ensisa.gl.entities.Column();
        column.setId(10);
        column.setName("IN_PROGRESS");
        
        when(storyRepo.find(1L)).thenReturn(story);
        when(columnRepo.find(10L)).thenReturn(column);
        when(userRepo.getAll()).thenReturn(Arrays.asList());
        
        ModelAndView result = sut.editStory(1L);
        
        assertEquals("story-edit", result.getViewName());
        assertTrue(result.getModelMap().containsKey("column"));
        assertTrue(result.getModelMap().containsKey("isDefaultColumn"));
        assertEquals(Boolean.TRUE, result.getModelMap().get("isDefaultColumn"));
    }

    @Test
    @DisplayName("editStory with non-default column should set isDefaultColumn to false")
    public void testEditStoryWithNonDefaultColumn() {
        Story story = new Story();
        story.setId(1);
        story.setProjectId(5L);
        story.setColumnId(10L);
        
        fr.uha.ensisa.gl.entities.Column column = new fr.uha.ensisa.gl.entities.Column();
        column.setId(10);
        column.setName("CUSTOM_COLUMN");
        
        when(storyRepo.find(1L)).thenReturn(story);
        when(columnRepo.find(10L)).thenReturn(column);
        when(userRepo.getAll()).thenReturn(Arrays.asList());
        
        ModelAndView result = sut.editStory(1L);
        
        assertEquals("story-edit", result.getViewName());
        assertEquals(Boolean.FALSE, result.getModelMap().get("isDefaultColumn"));
    }

    // ====== NEW TESTS FOR createStory with columnId ======

    @Test
    @DisplayName("createStory with columnId should assign story to that column")
    public void testCreateStoryWithColumnId() throws IOException {
        String testTitle = "Test Story";
        Long projectId = 1L;
        Long columnId = 5L;
        
        fr.uha.ensisa.gl.entities.Column column = new fr.uha.ensisa.gl.entities.Column();
        column.setId(5);
        column.setName("IN_PROGRESS");
        
        when(columnRepo.find(columnId)).thenReturn(column);
        when(storyRepo.findByColumn(columnId)).thenReturn(Arrays.asList());
        
        String result = sut.createStory(testTitle, null, projectId, columnId);
        
        assertEquals("redirect:/board/" + projectId, result);
        
        ArgumentCaptor<Story> captor = ArgumentCaptor.forClass(Story.class);
        verify(storyRepo).persist(captor.capture());
        
        Story captured = captor.getValue();
        assertEquals(columnId, captured.getColumnId());
        assertEquals(StoryStatus.IN_PROGRESS, captured.getStatus());
    }

    @Test
    @DisplayName("createStory should set BACKLOG for custom column")
    public void testCreateStoryWithCustomColumn() throws IOException {
        String testTitle = "Test Story";
        Long projectId = 1L;
        Long columnId = 5L;
        
        fr.uha.ensisa.gl.entities.Column column = new fr.uha.ensisa.gl.entities.Column();
        column.setId(5);
        column.setName("CUSTOM");
        
        when(columnRepo.find(columnId)).thenReturn(column);
        when(storyRepo.findByColumn(columnId)).thenReturn(Arrays.asList());
        
        String result = sut.createStory(testTitle, null, projectId, columnId);
        
        ArgumentCaptor<Story> captor = ArgumentCaptor.forClass(Story.class);
        verify(storyRepo).persist(captor.capture());
        
        Story captured = captor.getValue();
        assertEquals("BACKLOG", captured.getSubColumn());
    }

    @Test
    @DisplayName("createStory should find BACKLOG column when no columnId provided")
    public void testCreateStoryFindsBacklogColumn() throws IOException {
        String testTitle = "Test Story";
        Long projectId = 1L;
        
        fr.uha.ensisa.gl.entities.Column backlogColumn = new fr.uha.ensisa.gl.entities.Column();
        backlogColumn.setId(1);
        backlogColumn.setName("BACKLOG");
        
        fr.uha.ensisa.gl.entities.Column otherColumn = new fr.uha.ensisa.gl.entities.Column();
        otherColumn.setId(2);
        otherColumn.setName("DONE");
        
        when(columnRepo.findByProject(projectId)).thenReturn(Arrays.asList(backlogColumn, otherColumn));
        when(storyRepo.findByColumn(1L)).thenReturn(Arrays.asList());
        
        String result = sut.createStory(testTitle, null, projectId, null);
        
        ArgumentCaptor<Story> captor = ArgumentCaptor.forClass(Story.class);
        verify(storyRepo).persist(captor.capture());
        
        Story captured = captor.getValue();
        assertEquals(1L, captured.getColumnId());
    }

    @Test
    @DisplayName("createStory should shift existing stories positions")
    public void testCreateStoryShiftsPositions() throws IOException {
        String testTitle = "New Story";
        Long projectId = 1L;
        Long columnId = 5L;
        
        Story existingStory1 = new Story();
        existingStory1.setPosition(0);
        Story existingStory2 = new Story();
        existingStory2.setPosition(1);
        
        fr.uha.ensisa.gl.entities.Column column = new fr.uha.ensisa.gl.entities.Column();
        column.setId(5);
        column.setName("BACKLOG");
        
        when(columnRepo.find(columnId)).thenReturn(column);
        when(storyRepo.findByColumn(columnId)).thenReturn(Arrays.asList(existingStory1, existingStory2));
        
        String result = sut.createStory(testTitle, null, projectId, columnId);
        
        assertEquals(1, existingStory1.getPosition());
        assertEquals(2, existingStory2.getPosition());
        verify(storyRepo, times(3)).persist(any(Story.class)); // 2 existing + 1 new
    }

    // ====== NEW TESTS FOR updateStory null handling ======

    @Test
    @DisplayName("updateStory with empty description")
    public void testUpdateStoryEmptyDescription() {
        Story story = new Story();
        story.setId(1);
        story.setProjectId(5L);
        story.setColumnId(null);
        
        when(storyRepo.find(1L)).thenReturn(story);
        
        String result = sut.updateStory(1L, "Title", "", null);
        
        assertEquals("redirect:/board/5", result);
        assertEquals("", story.getDescription());
        verify(storyRepo).persist(story);
    }

    // ====== Additional branch coverage tests ======

    @Test
    @DisplayName("createStory with only whitespace title should redirect with error")
    public void testCreateStoryWhitespaceTitle() throws IOException {
        String result = sut.createStory("   ", "Description", 1L, null);
        
        assertTrue(result.contains("error=Title is required"));
        verify(storyRepo, never()).persist(any());
    }

    @Test
    @DisplayName("createStory with title longer than 59 chars should redirect with error")
    public void testCreateStoryTitleTooLong() throws IOException {
        String longTitle = "A".repeat(60);
        
        String result = sut.createStory(longTitle, "Description", 1L, null);
        
        assertTrue(result.contains("error=Title must be less than 59 characters"));
        verify(storyRepo, never()).persist(any());
    }

    @Test
    @DisplayName("createStory without projectId should redirect with error")
    public void testCreateStoryNoProjectId() throws IOException {
        String result = sut.createStory("Title", "Description", null, null);
        
        assertTrue(result.contains("error=Project is required"));
        verify(storyRepo, never()).persist(any());
    }

    @Test
    @DisplayName("createStory with null column should search for BACKLOG")
    public void testCreateStoryNullColumnSearchesBacklog() throws IOException {
        fr.uha.ensisa.gl.entities.Column backlogCol = new fr.uha.ensisa.gl.entities.Column();
        backlogCol.setId(1);
        backlogCol.setName("BACKLOG");
        
        when(columnRepo.findByProject(1L)).thenReturn(Arrays.asList(backlogCol));
        when(storyRepo.findByColumn(1L)).thenReturn(Arrays.asList());
        
        String result = sut.createStory("Test", null, 1L, null);
        
        ArgumentCaptor<Story> captor = ArgumentCaptor.forClass(Story.class);
        verify(storyRepo).persist(captor.capture());
        
        Story saved = captor.getValue();
        assertEquals(1L, saved.getColumnId());
        assertEquals("redirect:/board/1", result);
    }

    @Test
    @DisplayName("updateStory with whitespace-only title should redirect with error")
    public void testUpdateStoryWhitespaceTitle() {
        Story story = new Story();
        story.setId(1);
        story.setProjectId(5L);
        
        when(storyRepo.find(1L)).thenReturn(story);
        
        String result = sut.updateStory(1L, "   ", "desc", null);
        
        assertTrue(result.contains("error=Title is required"));
        verify(storyRepo, never()).persist(any());
    }

    @Test
    @DisplayName("showStory with null story should redirect to home")
    public void testShowStoryNull() throws IOException {
        when(storyRepo.find(999L)).thenReturn(null);
        
        ModelAndView result = sut.showStory(999L);
        
        assertEquals("redirect:/", result.getViewName());
    }

    @Test
    @DisplayName("editStory with column should load column details")
    public void testEditStoryLoadsColumn() {
        Story story = new Story();
        story.setId(1);
        story.setProjectId(5L);
        story.setColumnId(10L);
        
        fr.uha.ensisa.gl.entities.Column column = new fr.uha.ensisa.gl.entities.Column();
        column.setId(10);
        column.setName("DONE");
        
        when(storyRepo.find(1L)).thenReturn(story);
        when(columnRepo.find(10L)).thenReturn(column);
        when(userRepo.getAll()).thenReturn(Arrays.asList());
        
        ModelAndView result = sut.editStory(1L);
        
        assertEquals("story-edit", result.getViewName());
        assertTrue(result.getModelMap().containsKey("column"));
    }

    // ====== NEW TESTS FOR BRANCH COVERAGE ======
    
    @Test
    @DisplayName("createStory should find non-BACKLOG column when columnId is null")
    public void testCreateStoryFindsNonBacklogColumn() throws IOException {
        Long projectId = 5L;
        
        fr.uha.ensisa.gl.entities.Column inProgressColumn = new fr.uha.ensisa.gl.entities.Column();
        inProgressColumn.setId(10);
        inProgressColumn.setName("IN_PROGRESS");
        
        fr.uha.ensisa.gl.entities.Column reviewColumn = new fr.uha.ensisa.gl.entities.Column();
        reviewColumn.setId(11);
        reviewColumn.setName("REVIEW");
        
        when(columnRepo.findByProject(projectId)).thenReturn(Arrays.asList(inProgressColumn, reviewColumn));
        when(storyRepo.findByColumn(anyLong())).thenReturn(new ArrayList<>());
        
        String result = sut.createStory("New Story", "Description", projectId, null);
        
        assertEquals("redirect:/board/5", result);
        verify(storyRepo).persist(any(Story.class));
    }
    
    @Test
    @DisplayName("createStory should handle column DONE status mapping")
    public void testCreateStoryWithDoneColumn() throws IOException {
        Long projectId = 5L;
        Long columnId = 10L;
        
        fr.uha.ensisa.gl.entities.Column doneColumn = new fr.uha.ensisa.gl.entities.Column();
        doneColumn.setId(10);
        doneColumn.setName("DONE");
        
        when(columnRepo.find(columnId)).thenReturn(doneColumn);
        when(storyRepo.findByColumn(columnId)).thenReturn(new ArrayList<>());
        
        String result = sut.createStory("New Story", "Description", projectId, columnId);
        
        assertEquals("redirect:/board/5", result);
        verify(storyRepo).persist(any(Story.class));
    }
    
    @Test
    @DisplayName("createStory should handle null column when columnId provided")
    public void testCreateStoryWithNullColumn() throws IOException {
        Long projectId = 5L;
        Long columnId = 10L;
        
        when(columnRepo.find(columnId)).thenReturn(null);
        when(storyRepo.findByColumn(columnId)).thenReturn(new ArrayList<>());
        
        String result = sut.createStory("New Story", "Description", projectId, columnId);
        
        assertEquals("redirect:/board/5", result);
        verify(storyRepo).persist(any(Story.class));
    }
    
    @Test
    @DisplayName("createStory should not set subColumn for DONE column")
    public void testCreateStoryNoSubColumnForDone() throws IOException {
        Long projectId = 5L;
        Long columnId = 10L;
        
        fr.uha.ensisa.gl.entities.Column doneColumn = new fr.uha.ensisa.gl.entities.Column();
        doneColumn.setId(10);
        doneColumn.setName("DONE");
        
        when(columnRepo.find(columnId)).thenReturn(doneColumn);
        when(storyRepo.findByColumn(columnId)).thenReturn(new ArrayList<>());
        
        String result = sut.createStory("New Story", "Description", projectId, columnId);
        
        assertEquals("redirect:/board/5", result);
        verify(storyRepo).persist(any(Story.class));
    }
    
    @Test
    @DisplayName("updateStory should handle column null when checking default")
    public void testUpdateStoryColumnNullCheck() {
        Story story = new Story();
        story.setId(1);
        story.setProjectId(5L);
        story.setColumnId(10L);
        
        when(storyRepo.find(1L)).thenReturn(story);
        when(columnRepo.find(10L)).thenReturn(null);
        
        String result = sut.updateStory(1L, "Updated Title", "Description", null);
        
        assertEquals("redirect:/board/5", result);
        verify(storyRepo).persist(story);
    }
    
    @Test
    @DisplayName("updateStory should not change status in default column")
    public void testUpdateStoryNoStatusChangeInDefaultColumn() {
        Story story = new Story();
        story.setId(1);
        story.setProjectId(5L);
        story.setColumnId(10L);
        story.setStatus(StoryStatus.IN_PROGRESS);
        
        fr.uha.ensisa.gl.entities.Column column = new fr.uha.ensisa.gl.entities.Column();
        column.setId(10);
        column.setName("IN_PROGRESS");
        
        when(storyRepo.find(1L)).thenReturn(story);
        when(columnRepo.find(10L)).thenReturn(column);
        
        String result = sut.updateStory(1L, "Updated Title", "Description", "DONE");
        
        assertEquals("redirect:/board/5", result);
        assertEquals(StoryStatus.IN_PROGRESS, story.getStatus());
        verify(storyRepo).persist(story);
    }
    
    @Test
    @DisplayName("assignStory should handle null user")
    public void testAssignStoryNullUser() {
        Story story = new Story();
        story.setId(1);
        story.setProjectId(5L);
        
        when(storyRepo.find(1L)).thenReturn(story);
        when(userRepo.find(99)).thenReturn(null);
        
        String result = sut.assignStory(1L, 99);
        
        assertEquals("redirect:/board/5", result);
        verify(storyRepo, never()).persist(story);
    }
    
    @Test
    @DisplayName("assignStory should handle null story")
    public void testAssignStoryNullStory() {
        when(storyRepo.find(1L)).thenReturn(null);
        
        String result = sut.assignStory(1L, 99);
        
        assertEquals("redirect:/", result);
    }
    
    @Test
    @DisplayName("assignStory should redirect to home if no projectId")
    public void testAssignStoryNoProjectId() {
        Story story = new Story();
        story.setId(1);
        story.setProjectId(null);
        
        fr.uha.ensisa.gl.entities.User user = new fr.uha.ensisa.gl.entities.User();
        user.setId(99);
        user.setName("Test User");
        
        when(storyRepo.find(1L)).thenReturn(story);
        when(userRepo.find(99)).thenReturn(user);
        
        String result = sut.assignStory(1L, 99);
        
        assertEquals("redirect:/", result);
        verify(storyRepo).persist(story);
    }
    
    @Test
    @DisplayName("unassignStory should handle null story")
    public void testUnassignStoryNullStory() {
        when(storyRepo.find(1L)).thenReturn(null);
        
        String result = sut.unassignStory(1L);
        
        assertEquals("redirect:/", result);
    }
    
    @Test
    @DisplayName("unassignStory should redirect to home if no projectId")
    public void testUnassignStoryNoProjectId() {
        Story story = new Story();
        story.setId(1);
        story.setProjectId(null);
        
        when(storyRepo.find(1L)).thenReturn(story);
        
        String result = sut.unassignStory(1L);
        
        assertEquals("redirect:/", result);
        verify(storyRepo).persist(story);
    }

    // ===== TESTS POUR AMÉLIORER MUTATION COVERAGE =====

    @Test
    @DisplayName("showStory should call find successfully")
    public void testShowStoryDebugPrint() throws IOException {
        Story story = new Story();
        story.setId(1);
        story.setProjectId(5L);
        story.setColumnId(2L);
        
        fr.uha.ensisa.gl.entities.Column column = new fr.uha.ensisa.gl.entities.Column();
        column.setId(2);
        column.setName("IN_PROGRESS");
        
        when(storyRepo.find(1L)).thenReturn(story);
        when(columnRepo.find(2L)).thenReturn(column);
        when(userRepo.getAll()).thenReturn(Arrays.asList());
        
        ModelAndView result = sut.showStory(1L);
        
        assertNotNull(result);
        assertEquals("story-detail", result.getViewName());
        verify(storyRepo).find(1L);
    }

    @Test
    @DisplayName("updateStory should verify all debug statements")
    public void testUpdateStoryDebugStatements() throws IOException {
        Story story = new Story();
        story.setId(1);
        story.setTitle("Old Title");
        story.setProjectId(5L);
        
        when(storyRepo.find(1L)).thenReturn(story);
        
        String result = sut.updateStory(1L, "New Title", "New Desc", null);
        
        assertEquals("redirect:/board/5", result);  // Redirects to board when projectId exists
        verify(storyRepo).find(1L);
        verify(storyRepo).persist(story);
        assertEquals("New Title", story.getTitle());
        assertEquals("New Desc", story.getDescription());
    }

    @Test
    @DisplayName("createStory should call persist with BACKLOG column")
    public void testCreateStoryBacklogColumn() throws IOException {
        fr.uha.ensisa.gl.entities.Column backlogColumn = new fr.uha.ensisa.gl.entities.Column();
        backlogColumn.setId(1);
        backlogColumn.setName("BACKLOG");
        
        when(columnRepo.findByProject(1L)).thenReturn(Arrays.asList(backlogColumn));
        
        sut.createStory("Title", "Desc", 1L, null);
        
        ArgumentCaptor<Story> storyCaptor = ArgumentCaptor.forClass(Story.class);
        verify(storyRepo).persist(storyCaptor.capture());
        
        Story captured = storyCaptor.getValue();
        assertEquals(StoryStatus.BACKLOG, captured.getStatus());
        assertNull(captured.getSubColumn());  // SubColumn not set for BACKLOG
        assertEquals(1L, captured.getColumnId());
    }

    @Test
    @DisplayName("createStory with DONE column should set status DONE")
    public void testCreateStoryDoneColumn() throws IOException {
        fr.uha.ensisa.gl.entities.Column doneColumn = new fr.uha.ensisa.gl.entities.Column();
        doneColumn.setId(3);
        doneColumn.setName("DONE");
        
        when(columnRepo.findByProject(1L)).thenReturn(Arrays.asList(doneColumn));
        when(columnRepo.find(3L)).thenReturn(doneColumn);
        
        sut.createStory("Title", "Desc", 1L, 3L);
        
        ArgumentCaptor<Story> storyCaptor = ArgumentCaptor.forClass(Story.class);
        verify(storyRepo).persist(storyCaptor.capture());
        
        Story captured = storyCaptor.getValue();
        assertEquals(StoryStatus.DONE, captured.getStatus());
        assertEquals(3L, captured.getColumnId());
    }

    @Test
    @DisplayName("createStory with REVIEW column should set status REVIEW")
    public void testCreateStoryReviewColumn() throws IOException {
        fr.uha.ensisa.gl.entities.Column reviewColumn = new fr.uha.ensisa.gl.entities.Column();
        reviewColumn.setId(4);
        reviewColumn.setName("REVIEW");
        
        when(columnRepo.findByProject(1L)).thenReturn(Arrays.asList(reviewColumn));
        when(columnRepo.find(4L)).thenReturn(reviewColumn);
        
        sut.createStory("Title", "Desc", 1L, 4L);
        
        ArgumentCaptor<Story> storyCaptor = ArgumentCaptor.forClass(Story.class);
        verify(storyRepo).persist(storyCaptor.capture());
        
        Story captured = storyCaptor.getValue();
        assertEquals(StoryStatus.REVIEW, captured.getStatus());
    }

    @Test
    @DisplayName("createStory with BLOCKED column should set status BLOCKED")
    public void testCreateStoryBlockedColumn() throws IOException {
        fr.uha.ensisa.gl.entities.Column blockedColumn = new fr.uha.ensisa.gl.entities.Column();
        blockedColumn.setId(5);
        blockedColumn.setName("BLOCKED");
        
        when(columnRepo.findByProject(1L)).thenReturn(Arrays.asList(blockedColumn));
        when(columnRepo.find(5L)).thenReturn(blockedColumn);
        
        sut.createStory("Title", "Desc", 1L, 5L);
        
        ArgumentCaptor<Story> storyCaptor = ArgumentCaptor.forClass(Story.class);
        verify(storyRepo).persist(storyCaptor.capture());
        
        Story captured = storyCaptor.getValue();
        assertEquals(StoryStatus.BLOCKED, captured.getStatus());
    }

    @Test
    @DisplayName("createStory with custom column should default to BACKLOG status")
    public void testCreateStoryCustomColumn() throws IOException {
        fr.uha.ensisa.gl.entities.Column customColumn = new fr.uha.ensisa.gl.entities.Column();
        customColumn.setId(6);
        customColumn.setName("Custom Column");
        
        when(columnRepo.findByProject(1L)).thenReturn(Arrays.asList(customColumn));
        when(columnRepo.find(6L)).thenReturn(customColumn);
        
        sut.createStory("Title", "Desc", 1L, 6L);
        
        ArgumentCaptor<Story> storyCaptor = ArgumentCaptor.forClass(Story.class);
        verify(storyRepo).persist(storyCaptor.capture());
        
        Story captured = storyCaptor.getValue();
        // Custom columns set subColumn to BACKLOG but status stays BACKLOG (default)
        assertEquals(StoryStatus.BACKLOG, captured.getStatus());
        assertEquals("BACKLOG", captured.getSubColumn());
    }

    @Test
    @DisplayName("updateStory boundary test: exactly 59 chars should be accepted")
    public void testUpdateStoryExactly59Chars() throws IOException {
        Story story = new Story();
        story.setId(1);
        story.setProjectId(5L);
        
        when(storyRepo.find(1L)).thenReturn(story);
        
        String title59 = "A".repeat(59);
        String result = sut.updateStory(1L, title59, "Desc", null);
        
        assertEquals("redirect:/board/5", result);  // Redirects to board when projectId exists
        assertEquals(title59, story.getTitle());
        verify(storyRepo).persist(story);
    }

    @Test
    @DisplayName("updateStory boundary test: 60 chars should fail")
    public void testUpdateStory60Chars() throws IOException {
        Story story = new Story();
        story.setId(1);
        story.setProjectId(5L);
        
        when(storyRepo.find(1L)).thenReturn(story);
        
        String title60 = "A".repeat(60);
        String result = sut.updateStory(1L, title60, "Desc", null);
        
        assertTrue(result.contains("error"));
        assertTrue(result.contains("Title must be less than 59 characters"));
        verify(storyRepo, never()).persist(any());
    }
}
