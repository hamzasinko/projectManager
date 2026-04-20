package fr.uha.ensisa.gl.tarnished.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import fr.uha.ensisa.gl.entities.User;
import fr.uha.ensisa.gl.tarnished.config.PathHelper;
import fr.uha.ensisa.gl.tarnished.repos.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.ArgumentCaptor;
import org.springframework.web.servlet.ModelAndView;

import fr.uha.ensisa.gl.entities.Story;
import fr.uha.ensisa.gl.entities.StoryStatus;
import fr.uha.ensisa.gl.entities.Project;
import fr.uha.ensisa.gl.entities.Column;
import fr.uha.ensisa.gl.entities.WorkLog;

import java.io.IOException;
import java.util.Collection;
import java.util.Arrays;
import java.util.List;

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

    @Mock
    private PathHelper pathHelper;

    private StoryController sut; // System Under Test
    
    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this); // Initialise les @Mock
        
        //Configure le mock pour retourner storyRepo et projectRepo
        when(repoFactory.getStoryRepo()).thenReturn(storyRepo);
        when(repoFactory.getProjectRepo()).thenReturn(projectRepo);
        when(repoFactory.getColumnRepo()).thenReturn(columnRepo);
        when(repoFactory.getUserRepo()).thenReturn(userRepo);

        //Crée le controller et injecte le mock
        sut = new StoryController();
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
    @DisplayName("showCreateForm should return correct view with projects")
    public void testShowCreateForm() {
        //Mock projects
        when(projectRepo.findAll()).thenReturn(Arrays.asList(new Project()));
        
        ModelAndView result = sut.showCreateForm(null, null, null);
        
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
        
        //Mock column repository
        fr.uha.ensisa.gl.tarnished.repos.ColumnRepo columnRepo = mock(fr.uha.ensisa.gl.tarnished.repos.ColumnRepo.class);
        when(repoFactory.getColumnRepo()).thenReturn(columnRepo);
        when(columnRepo.findByProject(projectId)).thenReturn(Arrays.asList());

        //Appelle la méthode
        String redirect = sut.createStory(testTitle, testDescription, projectId, null, null);

        //Vérifie la redirection vers le board du projet
        assertEquals("redirect:/board/" + projectId, redirect,
                     "Should redirect to project board");

        //Vérifie que persist a été appelé avec les bons paramètres
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
        String redirect = sut.createStory(null, "Description", 1L, null, null);
        
        assertTrue(redirect.contains("error"), "Should redirect with error parameter");
        verify(storyRepo, never()).persist(any(Story.class));
    }
    
    @Test
    @DisplayName("createStory should redirect with error when title is empty")
    public void testCreateStoryWithEmptyTitle() throws IOException {
        String redirect = sut.createStory("   ", "Description", 1L, null, null);
        
        assertTrue(redirect.contains("error"), "Should redirect with error parameter");
        verify(storyRepo, never()).persist(any(Story.class));
    }
    
    @Test
    @DisplayName("createStory should handle null description")
    public void testCreateStoryWithNullDescription() throws IOException {
        String testTitle = "Test Story";
        
        String redirect = sut.createStory(testTitle, null, null, null, null);

        //Controller now allows creating a story without a project and redirects to the story list
        assertEquals("redirect:/story/list", redirect);
        //Story should have been persisted
        verify(storyRepo).persist(any(Story.class));
    }
    
    @Test
    @DisplayName("listStories should return story-list view")
    public void testListStoriesEmpty() throws IOException {
        //listStories should return the story-list view with stories model
        org.springframework.web.servlet.ModelAndView result = sut.listStories();
        
        assertNotNull(result);
        assertEquals("story-list", result.getViewName(), "Should return story-list view");
        assertTrue(result.getModelMap().containsKey("stories"), "Model should contain stories");
    }
    
    @Test
    @DisplayName("listStories should return story-list view regardless of data")
    public void testListStoriesWithData() throws IOException {
        //listStories should return the story-list view even when data exists
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

        //Mock column repository
        fr.uha.ensisa.gl.tarnished.repos.ColumnRepo columnRepo = mock(fr.uha.ensisa.gl.tarnished.repos.ColumnRepo.class);
        when(repoFactory.getColumnRepo()).thenReturn(columnRepo);
        when(columnRepo.findByProject(projectId)).thenReturn(Arrays.asList());

        sut.createStory(testTitle, "Description", projectId, null, null);
        
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

        //Mock column repository
        fr.uha.ensisa.gl.tarnished.repos.ColumnRepo columnRepo = mock(fr.uha.ensisa.gl.tarnished.repos.ColumnRepo.class);
        when(repoFactory.getColumnRepo()).thenReturn(columnRepo);
        when(columnRepo.findByProject(projectId)).thenReturn(Arrays.asList());

        sut.createStory(testTitle, "Description", projectId, null, null);
        
        ArgumentCaptor<Story> storyCaptor = ArgumentCaptor.forClass(Story.class);
        verify(storyRepo).persist(storyCaptor.capture());
        
        Story capturedStory = storyCaptor.getValue();
        assertNotNull(capturedStory.getDateCreated(),
                      "Story should have creation date set");
    }

    @Test
    @DisplayName("editStory should return view with story and available users")
    public void testEditStory() throws IOException {
        //Given
        long storyId = 1L;
        Story mockStory = mock(Story.class);
        when(mockStory.getId()).thenReturn((int) storyId);
        when(mockStory.getProjectId()).thenReturn(1L);
        when(mockStory.getColumnId()).thenReturn(null);

        when(storyRepo.find(storyId)).thenReturn(mockStory);
        when(repoFactory.getUserRepo()).thenReturn(mock(fr.uha.ensisa.gl.tarnished.repos.UserRepo.class));
        when(repoFactory.getColumnRepo()).thenReturn(mock(fr.uha.ensisa.gl.tarnished.repos.ColumnRepo.class));

        //When
        ModelAndView result = sut.editStory(storyId);
        
        //Then
        assertNotNull(result);
        assertEquals("story-edit", result.getViewName());
        assertEquals(mockStory, result.getModelMap().get("story"));
        assertTrue(result.getModelMap().containsKey("users"));
        
        verify(storyRepo).find(storyId);
    }

    @Test
    @DisplayName("updateStory should update story and redirect to board")
    public void testUpdateStory() throws IOException {
        //Given
        long storyId = 1L;
        String newTitle = "Updated Title";
        String newDescription = "Updated Description";
        String newStatus = "IN_PROGRESS";
        
        Story mockStory = mock(Story.class);
        when(mockStory.getProjectId()).thenReturn(1L);
        when(mockStory.getColumnId()).thenReturn(null);
        when(storyRepo.find(storyId)).thenReturn(mockStory);
        when(repoFactory.getColumnRepo()).thenReturn(mock(fr.uha.ensisa.gl.tarnished.repos.ColumnRepo.class));

        //When
        String result = sut.updateStory(storyId, newTitle, newDescription, newStatus);
        
        //Then
        assertEquals("redirect:/board/1", result);
        
        verify(mockStory).setTitle(newTitle);
        verify(mockStory).setDescription(newDescription);
        verify(mockStory).setStatus(StoryStatus.valueOf(newStatus));
    }

    @Test
    @DisplayName("deleteStory should call remove on repository and redirect to board")
    public void testDeleteStory() {
        //Given
        long storyId = 1L;
        Story mockStory = new Story();
        mockStory.setProjectId(1L);
        when(storyRepo.find(storyId)).thenReturn(mockStory);

        //When
        String result = sut.deleteStory(storyId);
        
        //Then
        assertEquals("redirect:/board/1", result);
        verify(storyRepo).remove(storyId);
    }
    @Disabled("Temporarily disabled until fix")
    @Test
    @DisplayName("assignStory should assign user to story and redirect to board")
    public void testAssignStory() throws IOException {
        int storyId = 1;
        int userId = 10;

        Story story = new Story();
        story.setId(storyId);
        story.setProjectId(1L);

        User mockUser = mock(User.class);

        when(storyRepo.find(storyId)).thenReturn(story);
        when(userRepo.find(userId)).thenReturn(mockUser);

        String result = sut.assignStory((long)storyId, userId);

        assertEquals("redirect:/board/1", result);
        assertEquals(mockUser, story.getUserAssigned());
        verify(storyRepo).persist(story);
    }

    @Test
    @DisplayName("unassignStory should remove user assignment and redirect to board")
    public void testUnassignStory() throws IOException {
        //Given
        long storyId = 1L;
        
        Story mockStory = mock(Story.class);
        when(mockStory.getProjectId()).thenReturn(1L);
        when(storyRepo.find(storyId)).thenReturn(mockStory);
        
        //When
        String result = sut.unassignStory(storyId);
        
        //Then
        assertEquals("redirect:/board/1", result);
        verify(mockStory).setUserAssigned(null);
        verify(storyRepo).persist(mockStory);
    }
    
    @Test
    @DisplayName("startTimer should start timer and redirect to story detail")
    public void testStartTimer() {
        //Given
        long storyId = 1L;
        long userId = 1L;
        
        //When
        String result = sut.startTimer(storyId, userId);
        
        //Then
        assertEquals("redirect:/story/1", result);
        verify(storyRepo).startTimer(storyId, userId);
    }
    
    @Test
    @DisplayName("stopTimer should stop timer and redirect to story detail")
    public void testStopTimer() {
        //Given
        long storyId = 1L;
        long workLogId = 100L;
        
        //When
        String result = sut.stopTimer(storyId, workLogId);
        
        //Then
        assertEquals("redirect:/story/1", result);
        verify(storyRepo).stopTimer(storyId, workLogId);
    }
    
    @Test
    @DisplayName("addWorkLog should add work log and redirect to story detail")
    public void testAddWorkLog() {
        //Given
        long storyId = 1L;
        int days = 0;
        int hours = 0;
        int minutes = 30;
        String comment = "Fixed bug";
        long userId = 1L;
        
        //When
        String result = sut.addWorkLog(storyId, days, hours, minutes, comment, userId);
        
        //Then
        assertEquals("redirect:/story/1", result);
        verify(storyRepo).addWorkLog(eq(storyId), any());
    }
    
    @Test
    @DisplayName("addWorkLog should work with null comment")
    public void testAddWorkLogWithNullComment() {
        //Given
        long storyId = 1L;
        int days = 0;
        int hours = 0;
        int minutes = 45;
        long userId = 1L;
        
        //When
        String result = sut.addWorkLog(storyId, days, hours, minutes, null, userId);
        
        //Then
        assertEquals("redirect:/story/1", result);
        verify(storyRepo).addWorkLog(eq(storyId), any());
    }
    
    @Test
    @DisplayName("addWorkLog should calculate total minutes correctly with days and hours")
    public void testAddWorkLogWithDaysAndHours() {
        //Given
        long storyId = 1L;
        int days = 1;
        int hours = 2;
        int minutes = 30;
        String comment = "Complex task";
        long userId = 1L;
        
        //When
        String result = sut.addWorkLog(storyId, days, hours, minutes, comment, userId);
        
        //Then
        assertEquals("redirect:/story/1", result);
        verify(storyRepo).addWorkLog(eq(storyId), any());
    }
    
    @Test
    @DisplayName("addWorkLog should redirect with error if duration is zero")
    public void testAddWorkLogWithZeroDuration() {
        //Given
        long storyId = 1L;
        int days = 0;
        int hours = 0;
        int minutes = 0;
        String comment = "Invalid";
        long userId = 1L;
        
        //When
        String result = sut.addWorkLog(storyId, days, hours, minutes, comment, userId);
        
        //Then
        assertEquals("redirect:/story/1?error=Duration must be greater than 0", result);
        verify(storyRepo, never()).addWorkLog(any(), any());
    }
    
    @Test
    @DisplayName("deleteWorkLog should remove work log and redirect to story detail")
    public void testDeleteWorkLog() {
        //Given
        long storyId = 1L;
        long workLogId = 100L;
        
        //When
        String result = sut.deleteWorkLog(storyId, workLogId);
        
        //Then
        assertEquals("redirect:/story/1", result);
        verify(storyRepo).removeWorkLog(storyId, workLogId);
    }
    
    @Test
    @DisplayName("addWorkLog should truncate comment to 45 characters")
    public void testAddWorkLogTruncatesLongComment() {
        //Given
        long storyId = 1L;
        int days = 0;
        int hours = 1;
        int minutes = 0;
        String longComment = "This is a very long comment that exceeds the maximum allowed limit";
        long userId = 1L;
        
        //When
        String result = sut.addWorkLog(storyId, days, hours, minutes, longComment, userId);
        
        //Then
        assertEquals("redirect:/story/1", result);
        verify(storyRepo).addWorkLog(eq(storyId), argThat(wl -> 
            wl.getComment() != null && wl.getComment().length() == 45
        ));
    }

    @Test
    @DisplayName("showStory should redirect when story is null")
    void testShowStoryWithNullStory() throws IOException {
        long storyId = 999L;

        when(storyRepo.find(storyId)).thenReturn(null);

        ModelAndView mav = sut.showStory(storyId);

        assertEquals("redirect:/", mav.getViewName());
    }

    @Test
    @DisplayName("showCreateForm should include projectId when provided")
    void testShowCreateFormWithProjectId() {
        Long projectId = 1L;

        when(projectRepo.findAll()).thenReturn(Arrays.asList());

        ModelAndView mav = sut.showCreateForm(projectId, null, null);

        assertEquals("story-create", mav.getViewName());
        assertEquals(projectId, mav.getModel().get("projectId"));
    }

    @Test
    @DisplayName("showCreateForm should include columnId when provided")
    void testShowCreateFormWithColumnId() {
        Long columnId = 5L;

        when(projectRepo.findAll()).thenReturn(Arrays.asList());

        ModelAndView mav = sut.showCreateForm(null, columnId, null);

        assertEquals("story-create", mav.getViewName());
        assertEquals(columnId, mav.getModel().get("columnId"));
    }

    @Test
    @DisplayName("showCreateForm should include swimlaneId when provided")
    void testShowCreateFormWithSwimlaneId() {
        Long swimlaneId = 10L;

        when(projectRepo.findAll()).thenReturn(Arrays.asList());

        ModelAndView mav = sut.showCreateForm(null, null, swimlaneId);

        assertEquals("story-create", mav.getViewName());
        assertEquals(swimlaneId, mav.getModel().get("swimlaneId"));
    }

    @Test
    @DisplayName("createStory should handle title length validation")
    void testCreateStoryTitleTooLong() throws IOException {
        String longTitle = "This is a very long title that definitely exceeds fifty-nine characters limit";
        Long projectId = 1L;

        String result = sut.createStory(longTitle, "Description", projectId, null, null);

        assertTrue(result.contains("error"));
        verify(storyRepo, never()).persist(any(Story.class));
    }

    @Test
    @DisplayName("createStory should accept title with exactly 59 characters")
    void testCreateStoryTitleWith59Characters() throws IOException {
        String title59Chars = "12345678901234567890123456789012345678901234567890123456789"; // exactly 59 chars
        Long projectId = 1L;

        when(columnRepo.findByProject(projectId)).thenReturn(Arrays.asList());

        ArgumentCaptor<Story> storyCaptor = ArgumentCaptor.forClass(Story.class);
        String result = sut.createStory(title59Chars, "Description", projectId, null, null);

        assertFalse(result.contains("error"));
        verify(storyRepo).persist(storyCaptor.capture());
        assertEquals(title59Chars, storyCaptor.getValue().getTitle());
    }

    @Test
    @DisplayName("createStory should reject title with exactly 60 characters")
    void testCreateStoryTitleWith60Characters() throws IOException {
        String title60Chars = "123456789012345678901234567890123456789012345678901234567890"; // exactly 60 chars
        Long projectId = 1L;

        String result = sut.createStory(title60Chars, "Description", projectId, null, null);

        assertTrue(result.contains("error"));
        verify(storyRepo, never()).persist(any(Story.class));
    }

    @Test
    @DisplayName("createStory should handle column with null status mapping")
    void testCreateStoryWithColumnHavingNullStatusMapping() throws IOException {
        String title = "Test Story";
        Long projectId = 1L;
        Long columnId = 5L;

        Column column = new Column();
        column.setId(columnId.intValue());
        column.setName("UNKNOWN_COLUMN"); // Column name that doesn't map to a status

        when(columnRepo.find(columnId)).thenReturn(column);
        when(storyRepo.findByColumn(columnId)).thenReturn(List.of());

        ArgumentCaptor<Story> storyCaptor = ArgumentCaptor.forClass(Story.class);
        doAnswer(invocation -> {
            Story s = invocation.getArgument(0);
            s.setId(1);
            return null;
        }).when(storyRepo).persist(any(Story.class));

        String result = sut.createStory(title, "Description", projectId, columnId, null);

        assertTrue(result.contains("redirect:/board/"));
        verify(storyRepo).persist(storyCaptor.capture());
        Story captured = storyCaptor.getValue();
        assertEquals(columnId, captured.getColumnId());
        assertEquals(StoryStatus.BACKLOG, captured.getStatus()); // Should use default BACKLOG when status mapping is null
        assertEquals(0, captured.getPosition());
    }

    @Test
    @DisplayName("createStory should verify setColumnId, setPosition, and setSubColumn are called")
    void testCreateStoryVerifiesSettersCalled() throws IOException {
        String title = "Test Story";
        Long projectId = 1L;
        Long columnId = 5L;

        Column customColumn = new Column();
        customColumn.setId(columnId.intValue());
        customColumn.setName("Custom Column");

        when(columnRepo.find(columnId)).thenReturn(customColumn);
        when(storyRepo.findByColumn(columnId)).thenReturn(List.of());

        ArgumentCaptor<Story> storyCaptor = ArgumentCaptor.forClass(Story.class);
        doAnswer(invocation -> {
            Story s = invocation.getArgument(0);
            s.setId(1);
            return null;
        }).when(storyRepo).persist(any(Story.class));

        String result = sut.createStory(title, "Description", projectId, columnId, null);

        assertTrue(result.contains("redirect:/board/"));
        verify(storyRepo).persist(storyCaptor.capture());
        Story captured = storyCaptor.getValue();
        
        // Verify setters were called
        assertEquals(columnId, captured.getColumnId());
        assertEquals(0, captured.getPosition());
        assertEquals("BACKLOG", captured.getSubColumn());
    }

    @Test
    @DisplayName("editStory should return view for valid story")
    void testEditStoryValid() {
        long storyId = 1L;

        Story story = new Story();
        story.setId((int) storyId);
        story.setTitle("Test Story");
        story.setProjectId(10L);

        when(storyRepo.find(storyId)).thenReturn(story);
        when(userRepo.getAll()).thenReturn(List.of());

        ModelAndView mav = sut.editStory(storyId);

        assertEquals("story-edit", mav.getViewName());
        assertEquals(story, mav.getModel().get("story"));
    }

    @Test
    @DisplayName("editStory should redirect when story has no projectId")
    void testEditStoryNoProjectId() {
        long storyId = 1L;

        Story story = new Story();
        story.setId((int) storyId);
        story.setTitle("Test Story");
        story.setProjectId(null);

        when(storyRepo.find(storyId)).thenReturn(story);

        ModelAndView mav = sut.editStory(storyId);

        assertEquals("redirect:/", mav.getViewName());
    }

    @Test
    @DisplayName("editStory should include column info when story is in a column")
    void testEditStoryWithColumn() {
        long storyId = 1L;

        Story story = new Story();
        story.setId((int) storyId);
        story.setTitle("Test Story");
        story.setProjectId(10L);
        story.setColumnId(5L);

        fr.uha.ensisa.gl.entities.Column column = new fr.uha.ensisa.gl.entities.Column();
        column.setId(5);
        column.setName("BACKLOG");

        when(storyRepo.find(storyId)).thenReturn(story);
        when(userRepo.getAll()).thenReturn(List.of());
        when(columnRepo.find(5L)).thenReturn(column);

        ModelAndView mav = sut.editStory(storyId);

        assertEquals("story-edit", mav.getViewName());
        assertEquals(column, mav.getModel().get("column"));
        assertTrue((Boolean) mav.getModel().get("isDefaultColumn"));
    }

    @Test
    @DisplayName("createStory with columnId should set story to that column")
    void testCreateStoryWithColumnId() throws IOException {
        String title = "Test Story";
        String description = "Description";
        Long projectId = 1L;
        Long columnId = 5L;

        fr.uha.ensisa.gl.entities.Column column = new fr.uha.ensisa.gl.entities.Column();
        column.setId(5);
        column.setName("IN PROGRESS");

        when(columnRepo.find(columnId)).thenReturn(column);

        String result = sut.createStory(title, description, projectId, columnId, null);

        assertTrue(result.contains("redirect:/board/"));

        ArgumentCaptor<Story> storyCaptor = ArgumentCaptor.forClass(Story.class);
        verify(storyRepo).persist(storyCaptor.capture());

        Story capturedStory = storyCaptor.getValue();
        assertEquals(columnId, capturedStory.getColumnId());
    }

    @Test
    @DisplayName("createStory with projectId should find BACKLOG column")
    void testCreateStoryFindsBacklogColumn() throws IOException {
        String title = "Test Story";
        Long projectId = 1L;

        fr.uha.ensisa.gl.entities.Column backlogColumn = new fr.uha.ensisa.gl.entities.Column();
        backlogColumn.setId(3);
        backlogColumn.setName("BACKLOG");

        fr.uha.ensisa.gl.entities.Column otherColumn = new fr.uha.ensisa.gl.entities.Column();
        otherColumn.setId(4);
        otherColumn.setName("DONE");

        when(columnRepo.findByProject(projectId)).thenReturn(List.of(backlogColumn, otherColumn));

        String result = sut.createStory(title, null, projectId, null, null);

        assertTrue(result.contains("redirect:/board/"));

        ArgumentCaptor<Story> storyCaptor = ArgumentCaptor.forClass(Story.class);
        verify(storyRepo).persist(storyCaptor.capture());

        Story capturedStory = storyCaptor.getValue();
        assertEquals(3L, capturedStory.getColumnId());
    }

    @Test
    @DisplayName("updateStory should handle story in default column")
    void testUpdateStoryInDefaultColumn() {
        long storyId = 1L;

        Story story = new Story();
        story.setId((int) storyId);
        story.setProjectId(10L);
        story.setColumnId(3L);
        story.setStatus(StoryStatus.BACKLOG);

        fr.uha.ensisa.gl.entities.Column backlogColumn = new fr.uha.ensisa.gl.entities.Column();
        backlogColumn.setId(3);
        backlogColumn.setName("BACKLOG");

        when(storyRepo.find(storyId)).thenReturn(story);
        when(columnRepo.find(3L)).thenReturn(backlogColumn);

        String result = sut.updateStory(storyId, "Updated Title", "Description", "DONE");

        assertTrue(result.contains("redirect:/board/"));
        //Status should NOT change because it's in a default column
        assertEquals(StoryStatus.BACKLOG, story.getStatus());
        verify(storyRepo).persist(story);
    }

    @Test
    @DisplayName("listStories should return view with all stories")
    void testListStoriesWithStories() throws IOException {
        Story story1 = new Story();
        story1.setId(1);
        story1.setTitle("Story 1");

        Story story2 = new Story();
        story2.setId(2);
        story2.setTitle("Story 2");

        when(storyRepo.findAll()).thenReturn(List.of(story1, story2));

        ModelAndView mav = sut.listStories();

        assertEquals("story-list", mav.getViewName());
        Collection<Story> stories = (Collection<Story>) mav.getModel().get("stories");
        assertEquals(2, stories.size());
    }



    @Test
    @DisplayName("updateStory should handle null description")
    void testUpdateStoryWithNullDescription() {
        long storyId = 1L;
        String title = "Updated Title";
        String status = "IN_PROGRESS";

        Story story = new Story();
        story.setId((int) storyId);
        story.setProjectId(10L);

        when(storyRepo.find(storyId)).thenReturn(story);

        String result = sut.updateStory(storyId, title, null, status);

        assertTrue(result.contains("redirect:/board/"));
        assertEquals(title, story.getTitle());
        assertNull(story.getDescription());
        verify(storyRepo).persist(story);
    }

    @Test
    @DisplayName("updateStory should update description")
    void testUpdateStoryWithDescription() {
        long storyId = 1L;
        String title = "Updated Title";
        String description = "Updated Description";

        Story story = new Story();
        story.setId((int) storyId);
        story.setProjectId(10L);

        when(storyRepo.find(storyId)).thenReturn(story);

        String result = sut.updateStory(storyId, title, description, "BACKLOG");

        assertTrue(result.contains("redirect:/board/"));
        assertEquals(description, story.getDescription());
        verify(storyRepo).persist(story);
    }

    @Test
    @DisplayName("updateStory should redirect to story list when story not found")
    void testUpdateStoryNotFound() {
        long storyId = 999L;

        when(storyRepo.find(storyId)).thenReturn(null);

        String result = sut.updateStory(storyId, "Title", "Description", null);

        assertEquals("redirect:/story/list", result);
        verify(storyRepo, never()).persist(any(Story.class));
    }

    @Test
    @DisplayName("updateStory should handle empty title")
    void testUpdateStoryEmptyTitle() {
        long storyId = 1L;

        Story story = new Story();
        story.setId((int) storyId);
        story.setProjectId(10L);

        when(storyRepo.find(storyId)).thenReturn(story);

        String result = sut.updateStory(storyId, "", "Description", null);

        assertTrue(result.contains("error"));
        verify(storyRepo, never()).persist(any(Story.class));
    }

    @Test
    @DisplayName("updateStory should handle long title")
    void testUpdateStoryLongTitle() {
        long storyId = 1L;
        String longTitle = "This is a very long title that definitely exceeds fifty-nine characters";

        Story story = new Story();
        story.setId((int) storyId);
        story.setProjectId(10L);

        when(storyRepo.find(storyId)).thenReturn(story);

        String result = sut.updateStory(storyId, longTitle, "Description", null);

        assertTrue(result.contains("error"));
        verify(storyRepo, never()).persist(any(Story.class));
    }

    @Test
    @DisplayName("updateStory should allow status change when not in default column")
    void testUpdateStoryStatusChangeInCustomColumn() {
        long storyId = 1L;

        Story story = new Story();
        story.setId((int) storyId);
        story.setProjectId(10L);
        story.setColumnId(5L);
        story.setStatus(StoryStatus.BACKLOG);

        fr.uha.ensisa.gl.entities.Column customColumn = new fr.uha.ensisa.gl.entities.Column();
        customColumn.setId(5);
        customColumn.setName("Custom Column");

        when(storyRepo.find(storyId)).thenReturn(story);
        when(columnRepo.find(5L)).thenReturn(customColumn);

        String result = sut.updateStory(storyId, "Title", "Description", "DONE");

        assertTrue(result.contains("redirect:/board/"));
        assertEquals(StoryStatus.DONE, story.getStatus());
        verify(storyRepo).persist(story);
    }

    @Test
    @DisplayName("updateStory should ignore invalid status value")
    void testUpdateStoryInvalidStatus() {
        long storyId = 1L;

        Story story = new Story();
        story.setId((int) storyId);
        story.setProjectId(10L);
        story.setColumnId(5L);
        story.setStatus(StoryStatus.BACKLOG);

        fr.uha.ensisa.gl.entities.Column customColumn = new fr.uha.ensisa.gl.entities.Column();
        customColumn.setId(5);
        customColumn.setName("Custom");

        when(storyRepo.find(storyId)).thenReturn(story);
        when(columnRepo.find(5L)).thenReturn(customColumn);

        String result = sut.updateStory(storyId, "Title", "Description", "INVALID_STATUS");

        assertTrue(result.contains("redirect:/board/"));
        assertEquals(StoryStatus.BACKLOG, story.getStatus()); // Status unchanged
        verify(storyRepo).persist(story);
    }

    @Test
    @DisplayName("updateStory should redirect to story page when no projectId")
    void testUpdateStoryNoProjectId() {
        long storyId = 1L;

        Story story = new Story();
        story.setId((int) storyId);
        story.setProjectId(null);

        when(storyRepo.find(storyId)).thenReturn(story);

        String result = sut.updateStory(storyId, "Title", "Description", null);

        assertEquals("redirect:/story/" + storyId, result);
        verify(storyRepo).persist(story);
    }

    @Test
    @DisplayName("deleteStory should redirect to board when story has projectId")
    void testDeleteStoryWithProjectId() {
        long storyId = 1L;

        Story story = new Story();
        story.setId((int) storyId);
        story.setProjectId(10L);

        when(storyRepo.find(storyId)).thenReturn(story);

        String result = sut.deleteStory(storyId);

        assertEquals("redirect:/board/10", result);
        verify(storyRepo).remove(storyId);
    }


    @Test
    @DisplayName("assignStory should handle user assignment correctly")
    void testAssignStorySuccess() {
        long storyId = 1L;
        int userId = 10;

        Story story = new Story();
        story.setId((int) storyId);
        story.setProjectId(5L);

        fr.uha.ensisa.gl.entities.User user = new fr.uha.ensisa.gl.entities.User();
        user.setId(userId);
        user.setName("Test User");

        when(storyRepo.find(storyId)).thenReturn(story);
        when(userRepo.find(userId)).thenReturn(user);

        String result = sut.assignStory(storyId, userId);

        assertEquals("redirect:/board/5", result);
        assertEquals(user, story.getUserAssigned());
        verify(storyRepo).persist(story);
    }

    @Test
    @DisplayName("assignStory should redirect to home when no projectId")
    void testAssignStoryWithoutProjectId() {
        long storyId = 1L;
        int userId = 10;

        Story story = new Story();
        story.setId((int) storyId);
        story.setProjectId(null);

        fr.uha.ensisa.gl.entities.User user = new fr.uha.ensisa.gl.entities.User();
        user.setId(userId);

        when(storyRepo.find(storyId)).thenReturn(story);
        when(userRepo.find(userId)).thenReturn(user);

        String result = sut.assignStory(storyId, userId);

        assertEquals("redirect:/", result);
        verify(storyRepo).persist(story);
    }

    @Test
    @DisplayName("unassignStory should remove user assignment")
    void testUnassignStorySuccess() {
        long storyId = 1L;

        fr.uha.ensisa.gl.entities.User user = new fr.uha.ensisa.gl.entities.User();
        user.setId(10);

        Story story = new Story();
        story.setId((int) storyId);
        story.setProjectId(5L);
        story.setUserAssigned(user);

        when(storyRepo.find(storyId)).thenReturn(story);

        String result = sut.unassignStory(storyId);

        assertEquals("redirect:/board/5", result);
        assertNull(story.getUserAssigned());
        verify(storyRepo).persist(story);
    }

    @Test
    @DisplayName("showStory should redirect when story has null projectId")
    void testShowStoryWithNullProjectId() throws IOException {
        long storyId = 1L;
        
        Story story = new Story();
        story.setId((int) storyId);
        story.setProjectId(null);
        
        when(storyRepo.find(storyId)).thenReturn(story);
        
        ModelAndView mav = sut.showStory(storyId);
        
        assertEquals("redirect:/", mav.getViewName());
    }

    @Test
    @DisplayName("editStory should handle story with null columnId")
    void testEditStoryWithNullColumnId() {
        long storyId = 1L;
        
        Story story = new Story();
        story.setId((int) storyId);
        story.setProjectId(10L);
        story.setColumnId(null);
        
        when(storyRepo.find(storyId)).thenReturn(story);
        when(userRepo.getAll()).thenReturn(List.of());
        
        ModelAndView mav = sut.editStory(storyId);
        
        assertEquals("story-edit", mav.getViewName());
        assertFalse(mav.getModel().containsKey("column"));
    }

    @Test
    @DisplayName("editStory should handle story with column that doesn't exist")
    void testEditStoryWithNonExistentColumn() {
        long storyId = 1L;
        
        Story story = new Story();
        story.setId((int) storyId);
        story.setProjectId(10L);
        story.setColumnId(999L);
        
        when(storyRepo.find(storyId)).thenReturn(story);
        when(columnRepo.find(999L)).thenReturn(null);
        when(userRepo.getAll()).thenReturn(List.of());
        
        ModelAndView mav = sut.editStory(storyId);
        
        assertEquals("story-edit", mav.getViewName());
        assertFalse(mav.getModel().containsKey("column"));
    }

    @Test
    @DisplayName("createStory should redirect to story/list when projectId is null")
    void testCreateStoryWithoutProjectId() throws IOException {
        String title = "Test Story";
        
        Story story = new Story();
        story.setId(1);
        story.setProjectId(null);
        
        when(storyRepo.findByColumn(anyLong())).thenReturn(List.of());
        doAnswer(invocation -> {
            Story s = invocation.getArgument(0);
            s.setId(1);
            return null;
        }).when(storyRepo).persist(any(Story.class));
        
        String result = sut.createStory(title, "Description", null, null, null);
        
        assertEquals("redirect:/story/list", result);
    }

    @Test
    @DisplayName("createStory should set subColumn to BACKLOG for custom column")
    void testCreateStoryWithCustomColumn() throws IOException {
        String title = "Test Story";
        Long projectId = 1L;
        Long columnId = 5L;
        
        Column customColumn = new Column();
        customColumn.setId(columnId.intValue());
        customColumn.setName("Custom Column");
        
        Story story = new Story();
        story.setId(1);
        
        when(columnRepo.find(columnId)).thenReturn(customColumn);
        when(storyRepo.findByColumn(columnId)).thenReturn(List.of());
        doAnswer(invocation -> {
            Story s = invocation.getArgument(0);
            s.setId(1);
            s.setProjectId(projectId);
            return null;
        }).when(storyRepo).persist(any(Story.class));
        
        String result = sut.createStory(title, "Description", projectId, columnId, null);
        
        ArgumentCaptor<Story> storyCaptor = ArgumentCaptor.forClass(Story.class);
        verify(storyRepo).persist(storyCaptor.capture());
        assertEquals("BACKLOG", storyCaptor.getValue().getSubColumn());
    }

    @Test
    @DisplayName("createStory should set swimlaneId when provided")
    void testCreateStoryWithSwimlaneId() throws IOException {
        String title = "Test Story";
        Long projectId = 1L;
        Long swimlaneId = 10L;
        
        Story story = new Story();
        story.setId(1);
        
        when(storyRepo.findByColumn(anyLong())).thenReturn(List.of());
        doAnswer(invocation -> {
            Story s = invocation.getArgument(0);
            s.setId(1);
            s.setProjectId(projectId);
            return null;
        }).when(storyRepo).persist(any(Story.class));
        
        String result = sut.createStory(title, "Description", projectId, null, swimlaneId);
        
        ArgumentCaptor<Story> storyCaptor = ArgumentCaptor.forClass(Story.class);
        verify(storyRepo).persist(storyCaptor.capture());
        assertEquals(swimlaneId, storyCaptor.getValue().getSwimlaneId());
    }

    @Test
    @DisplayName("updateStory should handle invalid status value")
    void testUpdateStoryWithInvalidStatus() {
        long storyId = 1L;
        
        Story story = new Story();
        story.setId((int) storyId);
        story.setProjectId(10L);
        story.setColumnId(5L);
        story.setStatus(fr.uha.ensisa.gl.entities.StoryStatus.BACKLOG);
        
        Column customColumn = new Column();
        customColumn.setId(5);
        customColumn.setName("Custom Column");
        
        when(storyRepo.find(storyId)).thenReturn(story);
        when(columnRepo.find(5L)).thenReturn(customColumn);
        
        String result = sut.updateStory(storyId, "New Title", "Description", "INVALID_STATUS");
        
        assertEquals("redirect:/board/10", result);
        //Status should remain unchanged
        assertEquals(fr.uha.ensisa.gl.entities.StoryStatus.BACKLOG, story.getStatus());
        verify(storyRepo).persist(story);
    }

    @Test
    @DisplayName("updateStory should redirect to story/id when projectId is null")
    void testUpdateStoryWithoutProjectId() {
        long storyId = 1L;
        
        Story story = new Story();
        story.setId((int) storyId);
        story.setProjectId(null);
        story.setColumnId(5L);
        
        Column customColumn = new Column();
        customColumn.setId(5);
        customColumn.setName("Custom Column");
        
        when(storyRepo.find(storyId)).thenReturn(story);
        when(columnRepo.find(5L)).thenReturn(customColumn);
        
        String result = sut.updateStory(storyId, "New Title", "Description", null);
        
        assertEquals("redirect:/story/" + storyId, result);
        verify(storyRepo).persist(story);
    }

    @Test
    @DisplayName("deleteStory should handle null story")
    void testDeleteStoryWithNullStory() {
        long storyId = 999L;
        
        when(storyRepo.find(storyId)).thenReturn(null);
        
        String result = sut.deleteStory(storyId);
        
        assertEquals("redirect:/story/list", result);
        verify(storyRepo).remove(storyId);
    }

    @Test
    @DisplayName("assignStory should handle null user")
    void testAssignStoryWithNullUser() {
        long storyId = 1L;
        int userId = 999;
        
        Story story = new Story();
        story.setId((int) storyId);
        story.setProjectId(10L);
        
        when(storyRepo.find(storyId)).thenReturn(story);
        when(userRepo.find(userId)).thenReturn(null);
        
        String result = sut.assignStory(storyId, userId);
        
        assertEquals("redirect:/board/10", result);
        assertNull(story.getUserAssigned());
        verify(storyRepo, never()).persist(story);
    }


    @Test
    @DisplayName("mapColumnNameToStatus should return correct status for all default columns")
    void testMapColumnNameToStatus() throws IOException {
        //Test through createStory which uses mapColumnNameToStatus
        String title = "Test Story";
        Long projectId = 1L;
        Long columnId = 2L;
        
        Column backlogColumn = new Column();
        backlogColumn.setId(columnId.intValue());
        backlogColumn.setName("BACKLOG");
        
        when(columnRepo.find(columnId)).thenReturn(backlogColumn);
        when(storyRepo.findByColumn(columnId)).thenReturn(List.of());
        doAnswer(invocation -> {
            Story s = invocation.getArgument(0);
            s.setId(1);
            s.setProjectId(projectId);
            return null;
        }).when(storyRepo).persist(any(Story.class));
        
        String result = sut.createStory(title, "Description", projectId, columnId, null);
        
        ArgumentCaptor<Story> storyCaptor = ArgumentCaptor.forClass(Story.class);
        verify(storyRepo).persist(storyCaptor.capture());
        assertEquals(fr.uha.ensisa.gl.entities.StoryStatus.BACKLOG, storyCaptor.getValue().getStatus());
    }

    @Test
    @DisplayName("unassignStory should redirect to home when no projectId")
    void testUnassignStoryWithoutProjectId() {
        long storyId = 1L;

        Story story = new Story();
        story.setId((int) storyId);
        story.setProjectId(null);

        when(storyRepo.find(storyId)).thenReturn(story);

        String result = sut.unassignStory(storyId);

        assertEquals("redirect:/", result);
        verify(storyRepo).persist(story);
    }

    @Test
    @DisplayName("createStory should set columnId to null when projectId is null and columnId is null")
    void testCreateStory_SetColumnIdToNull() throws IOException {
        String title = "Test Story";
        
        when(storyRepo.findByColumn(anyLong())).thenReturn(List.of());
        doAnswer(invocation -> {
            Story s = invocation.getArgument(0);
            s.setId(1);
            return null;
        }).when(storyRepo).persist(any(Story.class));
        
        String result = sut.createStory(title, "Description", null, null, null);
        
        ArgumentCaptor<Story> storyCaptor = ArgumentCaptor.forClass(Story.class);
        verify(storyRepo).persist(storyCaptor.capture());
        assertNull(storyCaptor.getValue().getColumnId(), "ColumnId should be null when projectId is null and columnId is null");
    }

    @Test
    @DisplayName("createStory should set subColumn to null for default BACKLOG column")
    void testCreateStory_SetSubColumnToNullForDefaultColumn() throws IOException {
        String title = "Test Story";
        Long projectId = 1L;
        Long columnId = 2L;
        
        Column backlogColumn = new Column();
        backlogColumn.setId(columnId.intValue());
        backlogColumn.setName("BACKLOG");
        
        Story story = new Story();
        story.setId(1);
        
        when(columnRepo.find(columnId)).thenReturn(backlogColumn);
        when(storyRepo.findByColumn(columnId)).thenReturn(List.of());
        doAnswer(invocation -> {
            Story s = invocation.getArgument(0);
            s.setId(1);
            s.setProjectId(projectId);
            return null;
        }).when(storyRepo).persist(any(Story.class));
        
        String result = sut.createStory(title, "Description", projectId, columnId, null);
        
        ArgumentCaptor<Story> storyCaptor = ArgumentCaptor.forClass(Story.class);
        verify(storyRepo).persist(storyCaptor.capture());
        assertNull(storyCaptor.getValue().getSubColumn(), "SubColumn should be null for default BACKLOG column");
    }

    @Test
    @DisplayName("createStory should set subColumn to null for default DONE column")
    void testCreateStory_SetSubColumnToNullForDoneColumn() throws IOException {
        String title = "Test Story";
        Long projectId = 1L;
        Long columnId = 4L;
        
        Column doneColumn = new Column();
        doneColumn.setId(columnId.intValue());
        doneColumn.setName("DONE");
        
        when(columnRepo.find(columnId)).thenReturn(doneColumn);
        when(storyRepo.findByColumn(columnId)).thenReturn(List.of());
        doAnswer(invocation -> {
            Story s = invocation.getArgument(0);
            s.setId(1);
            s.setProjectId(projectId);
            return null;
        }).when(storyRepo).persist(any(Story.class));
        
        String result = sut.createStory(title, "Description", projectId, columnId, null);
        
        ArgumentCaptor<Story> storyCaptor = ArgumentCaptor.forClass(Story.class);
        verify(storyRepo).persist(storyCaptor.capture());
        assertNull(storyCaptor.getValue().getSubColumn(), "SubColumn should be null for default DONE column");
    }

    @Test
    @DisplayName("mapColumnNameToStatus should return IN_PROGRESS for IN_PROGRESS column")
    void testMapColumnNameToStatus_ReturnsInProgress() throws IOException {
        String title = "Test Story";
        Long projectId = 1L;
        Long columnId = 2L;
        
        Column inProgressColumn = new Column();
        inProgressColumn.setId(columnId.intValue());
        inProgressColumn.setName("IN PROGRESS");
        
        when(columnRepo.find(columnId)).thenReturn(inProgressColumn);
        when(storyRepo.findByColumn(columnId)).thenReturn(List.of());
        doAnswer(invocation -> {
            Story s = invocation.getArgument(0);
            s.setId(1);
            s.setProjectId(projectId);
            return null;
        }).when(storyRepo).persist(any(Story.class));
        
        String result = sut.createStory(title, "Description", projectId, columnId, null);
        
        ArgumentCaptor<Story> storyCaptor = ArgumentCaptor.forClass(Story.class);
        verify(storyRepo).persist(storyCaptor.capture());
        assertEquals(fr.uha.ensisa.gl.entities.StoryStatus.IN_PROGRESS, storyCaptor.getValue().getStatus());
    }

    @Test
    @DisplayName("mapColumnNameToStatus should return REVIEW for REVIEW column")
    void testMapColumnNameToStatus_ReturnsReview() throws IOException {
        String title = "Test Story";
        Long projectId = 1L;
        Long columnId = 3L;
        
        Column reviewColumn = new Column();
        reviewColumn.setId(columnId.intValue());
        reviewColumn.setName("REVIEW");
        
        when(columnRepo.find(columnId)).thenReturn(reviewColumn);
        when(storyRepo.findByColumn(columnId)).thenReturn(List.of());
        doAnswer(invocation -> {
            Story s = invocation.getArgument(0);
            s.setId(1);
            s.setProjectId(projectId);
            return null;
        }).when(storyRepo).persist(any(Story.class));
        
        String result = sut.createStory(title, "Description", projectId, columnId, null);
        
        ArgumentCaptor<Story> storyCaptor = ArgumentCaptor.forClass(Story.class);
        verify(storyRepo).persist(storyCaptor.capture());
        assertEquals(fr.uha.ensisa.gl.entities.StoryStatus.REVIEW, storyCaptor.getValue().getStatus());
    }

    @Test
    @DisplayName("mapColumnNameToStatus should return DONE for DONE column")
    void testMapColumnNameToStatus_ReturnsDone() throws IOException {
        String title = "Test Story";
        Long projectId = 1L;
        Long columnId = 4L;
        
        Column doneColumn = new Column();
        doneColumn.setId(columnId.intValue());
        doneColumn.setName("DONE");
        
        when(columnRepo.find(columnId)).thenReturn(doneColumn);
        when(storyRepo.findByColumn(columnId)).thenReturn(List.of());
        doAnswer(invocation -> {
            Story s = invocation.getArgument(0);
            s.setId(1);
            s.setProjectId(projectId);
            return null;
        }).when(storyRepo).persist(any(Story.class));
        
        String result = sut.createStory(title, "Description", projectId, columnId, null);
        
        ArgumentCaptor<Story> storyCaptor = ArgumentCaptor.forClass(Story.class);
        verify(storyRepo).persist(storyCaptor.capture());
        assertEquals(fr.uha.ensisa.gl.entities.StoryStatus.DONE, storyCaptor.getValue().getStatus());
    }

    @Test
    @DisplayName("mapColumnNameToStatus should return BLOCKED for BLOCKED column")
    void testMapColumnNameToStatus_ReturnsBlocked() throws IOException {
        String title = "Test Story";
        Long projectId = 1L;
        Long columnId = 5L;
        
        Column blockedColumn = new Column();
        blockedColumn.setId(columnId.intValue());
        blockedColumn.setName("BLOCKED");
        
        when(columnRepo.find(columnId)).thenReturn(blockedColumn);
        when(storyRepo.findByColumn(columnId)).thenReturn(List.of());
        doAnswer(invocation -> {
            Story s = invocation.getArgument(0);
            s.setId(1);
            s.setProjectId(projectId);
            return null;
        }).when(storyRepo).persist(any(Story.class));
        
        String result = sut.createStory(title, "Description", projectId, columnId, null);
        
        ArgumentCaptor<Story> storyCaptor = ArgumentCaptor.forClass(Story.class);
        verify(storyRepo).persist(storyCaptor.capture());
        assertEquals(fr.uha.ensisa.gl.entities.StoryStatus.BLOCKED, storyCaptor.getValue().getStatus());
    }

    @Test
    @DisplayName("createStory should call setPosition(0) to place story at top")
    void testCreateStory_CallsSetPositionZero() throws IOException {
        String title = "Test Story";
        Long projectId = 1L;
        Long columnId = 2L;
        
        Column column = new Column();
        column.setId(columnId.intValue());
        column.setName("BACKLOG");
        
        Story existingStory = new Story();
        existingStory.setId(2);
        existingStory.setPosition(5);
        
        when(columnRepo.find(columnId)).thenReturn(column);
        when(storyRepo.findByColumn(columnId)).thenReturn(List.of(existingStory));
        doAnswer(invocation -> {
            Story s = invocation.getArgument(0);
            s.setId(1);
            s.setProjectId(projectId);
            return null;
        }).when(storyRepo).persist(any(Story.class));
        
        String result = sut.createStory(title, "Description", projectId, columnId, null);
        
        ArgumentCaptor<Story> newStoryCaptor = ArgumentCaptor.forClass(Story.class);
        verify(storyRepo, times(2)).persist(newStoryCaptor.capture()); // existing story + new story
        
        // Vérifie que la nouvelle story a position = 0
        Story newStory = newStoryCaptor.getAllValues().get(1); // La dernière story persistée est la nouvelle
        assertEquals(0, newStory.getPosition(), "New story should have position 0");
    }

    @Test
    @DisplayName("createStory should increment position of existing stories in column")
    void testCreateStory_IncrementsExistingStoriesPosition() throws IOException {
        String title = "Test Story";
        Long projectId = 1L;
        Long columnId = 2L;
        
        Column column = new Column();
        column.setId(columnId.intValue());
        column.setName("BACKLOG");
        
        Story existingStory1 = new Story();
        existingStory1.setId(2);
        existingStory1.setPosition(0);
        
        Story existingStory2 = new Story();
        existingStory2.setId(3);
        existingStory2.setPosition(1);
        
        when(columnRepo.find(columnId)).thenReturn(column);
        when(storyRepo.findByColumn(columnId)).thenReturn(List.of(existingStory1, existingStory2));
        doAnswer(invocation -> {
            Story s = invocation.getArgument(0);
            s.setId(1);
            s.setProjectId(projectId);
            return null;
        }).when(storyRepo).persist(any(Story.class));
        
        String result = sut.createStory(title, "Description", projectId, columnId, null);
        
        // Vérifie que les positions des stories existantes ont été incrémentées
        assertEquals(1, existingStory1.getPosition(), "First existing story position should be incremented");
        assertEquals(2, existingStory2.getPosition(), "Second existing story position should be incremented");
        verify(storyRepo, times(3)).persist(any(Story.class)); // 2 existing + 1 new
    }

    @Test
    @DisplayName("createStory should call setSwimlaneId when swimlaneId is provided")
    void testCreateStory_CallsSetSwimlaneId() throws IOException {
        String title = "Test Story";
        Long projectId = 1L;
        Long swimlaneId = 10L;
        
        when(storyRepo.findByColumn(anyLong())).thenReturn(List.of());
        doAnswer(invocation -> {
            Story s = invocation.getArgument(0);
            s.setId(1);
            s.setProjectId(projectId);
            return null;
        }).when(storyRepo).persist(any(Story.class));
        
        String result = sut.createStory(title, "Description", projectId, null, swimlaneId);
        
        ArgumentCaptor<Story> storyCaptor = ArgumentCaptor.forClass(Story.class);
        verify(storyRepo).persist(storyCaptor.capture());
        assertEquals(swimlaneId, storyCaptor.getValue().getSwimlaneId(), "Story should have swimlaneId set");
    }

    @Test
    @DisplayName("updateStory should accept title with exactly 59 characters")
    void testUpdateStory_WithExact59Characters() {
        long storyId = 1L;
        String titleExactly59 = "A".repeat(59); // exactly 59 chars
        
        Story story = new Story();
        story.setId((int) storyId);
        story.setProjectId(10L);
        story.setColumnId(5L);
        
        Column customColumn = new Column();
        customColumn.setId(5);
        customColumn.setName("Custom Column");
        
        when(storyRepo.find(storyId)).thenReturn(story);
        when(columnRepo.find(5L)).thenReturn(customColumn);
        
        String result = sut.updateStory(storyId, titleExactly59, "Description", null);
        
        assertEquals("redirect:/board/10", result);
        assertEquals(titleExactly59, story.getTitle().trim());
        verify(storyRepo).persist(story);
    }

    @Test
    @DisplayName("createStory should accept title with exactly 59 characters")
    void testCreateStory_WithExact59Characters() throws IOException {
        String titleExactly59 = "A".repeat(59); // exactly 59 chars
        Long projectId = 1L;
        
        when(storyRepo.findByColumn(anyLong())).thenReturn(List.of());
        doAnswer(invocation -> {
            Story s = invocation.getArgument(0);
            s.setId(1);
            s.setProjectId(projectId);
            return null;
        }).when(storyRepo).persist(any(Story.class));
        
        String result = sut.createStory(titleExactly59, "Description", projectId, null, null);
        
        ArgumentCaptor<Story> storyCaptor = ArgumentCaptor.forClass(Story.class);
        verify(storyRepo).persist(storyCaptor.capture());
        assertEquals(titleExactly59, storyCaptor.getValue().getTitle().trim());
    }

    @Test
    @DisplayName("createStory should explicitly call setColumnId(null) when projectId is null")
    void testCreateStory_ExplicitlyCallsSetColumnIdNull() throws IOException {
        String title = "Test Story";
        
        when(storyRepo.findByColumn(anyLong())).thenReturn(List.of());
        doAnswer(invocation -> {
            Story s = invocation.getArgument(0);
            s.setId(1);
            return null;
        }).when(storyRepo).persist(any(Story.class));
        
        ArgumentCaptor<Story> storyCaptor = ArgumentCaptor.forClass(Story.class);
        String result = sut.createStory(title, "Description", null, null, null);
        
        verify(storyRepo).persist(storyCaptor.capture());
        Story captured = storyCaptor.getValue();
        assertNull(captured.getColumnId(), "setColumnId(null) should be called when projectId is null");
    }

    @Test
    @DisplayName("createStory should explicitly call setPosition(0) for new story")
    void testCreateStory_ExplicitlyCallsSetPositionZero() throws IOException {
        String title = "Test Story";
        Long projectId = 1L;
        
        when(storyRepo.findByColumn(anyLong())).thenReturn(List.of());
        doAnswer(invocation -> {
            Story s = invocation.getArgument(0);
            s.setId(1);
            s.setProjectId(projectId);
            return null;
        }).when(storyRepo).persist(any(Story.class));
        
        ArgumentCaptor<Story> storyCaptor = ArgumentCaptor.forClass(Story.class);
        String result = sut.createStory(title, "Description", projectId, null, null);
        
        verify(storyRepo).persist(storyCaptor.capture());
        Story captured = storyCaptor.getValue();
        assertEquals(0, captured.getPosition(), "setPosition(0) should be called for new story");
    }

    @Test
    @DisplayName("createStory should explicitly call setSubColumn(null) for default column")
    void testCreateStory_ExplicitlyCallsSetSubColumnNull() throws IOException {
        String title = "Test Story";
        Long projectId = 1L;
        Long columnId = 2L;
        
        Column backlogColumn = new Column();
        backlogColumn.setId(columnId.intValue());
        backlogColumn.setName("BACKLOG");
        
        when(columnRepo.find(columnId)).thenReturn(backlogColumn);
        when(storyRepo.findByColumn(columnId)).thenReturn(List.of());
        doAnswer(invocation -> {
            Story s = invocation.getArgument(0);
            s.setId(1);
            s.setProjectId(projectId);
            return null;
        }).when(storyRepo).persist(any(Story.class));
        
        ArgumentCaptor<Story> storyCaptor = ArgumentCaptor.forClass(Story.class);
        String result = sut.createStory(title, "Description", projectId, columnId, null);
        
        verify(storyRepo).persist(storyCaptor.capture());
        Story captured = storyCaptor.getValue();
        assertNull(captured.getSubColumn(), "setSubColumn(null) should be called for default column");
    }

    @Test
    @DisplayName("deleteStory should handle null story")
    void testDeleteStory_WithNullStory() {
        Long storyId = 999L;
        
        when(storyRepo.find(storyId)).thenReturn(null);
        
        String result = sut.deleteStory(storyId);
        
        assertEquals("redirect:/story/list", result);
        verify(storyRepo).remove(storyId);
    }

    @Test
    @DisplayName("deleteStory should redirect to board when projectId exists")
    void testDeleteStory_WithProjectId() {
        Long storyId = 1L;
        Long projectId = 10L;
        
        Story story = new Story();
        story.setId(1);
        story.setProjectId(projectId);
        
        when(storyRepo.find(storyId)).thenReturn(story);
        
        String result = sut.deleteStory(storyId);
        
        assertEquals("redirect:/board/" + projectId, result);
        verify(storyRepo).remove(storyId);
    }

    @Test
    @DisplayName("unassignStory should handle null story")
    void testUnassignStory_WithNullStory() {
        Long storyId = 999L;
        
        when(storyRepo.find(storyId)).thenReturn(null);
        
        String result = sut.unassignStory(storyId);
        
        assertEquals("redirect:/", result);
        verify(storyRepo, never()).persist(any(Story.class));
    }

    @Test
    @DisplayName("unassignStory should redirect to board when projectId exists")
    void testUnassignStory_WithProjectId() {
        Long storyId = 1L;
        Long projectId = 10L;
        
        Story story = new Story();
        story.setId(1);
        story.setProjectId(projectId);
        story.setUserAssigned(new User());
        
        when(storyRepo.find(storyId)).thenReturn(story);
        
        String result = sut.unassignStory(storyId);
        
        assertEquals("redirect:/board/" + projectId, result);
        assertNull(story.getUserAssigned());
        verify(storyRepo).persist(story);
    }

    @Test
    @DisplayName("unassignStory should redirect to home when projectId is null")
    void testUnassignStory_WithNullProjectId() {
        Long storyId = 1L;
        
        Story story = new Story();
        story.setId(1);
        story.setProjectId(null);
        story.setUserAssigned(new User());
        
        when(storyRepo.find(storyId)).thenReturn(story);
        
        String result = sut.unassignStory(storyId);
        
        assertEquals("redirect:/", result);
        assertNull(story.getUserAssigned());
        verify(storyRepo).persist(story);
    }

    @Test
    @DisplayName("addWorkLog should reject zero or negative duration")
    void testAddWorkLog_WithZeroDuration() {
        Long storyId = 1L;
        
        String result = sut.addWorkLog(storyId, 0, 0, 0, "Comment", 1L);
        
        assertTrue(result.contains("error"));
        assertTrue(result.contains("Duration must be greater than 0"));
        verify(storyRepo, never()).addWorkLog(anyLong(), any());
    }

    @Test
    @DisplayName("addWorkLog should truncate comment longer than 45 characters")
    void testAddWorkLog_WithLongComment() {
        Long storyId = 1L;
        String longComment = "A".repeat(50);
        
        String result = sut.addWorkLog(storyId, 0, 1, 0, longComment, 1L);
        
        assertTrue(result.contains("redirect:/story/" + storyId));
        ArgumentCaptor<WorkLog> workLogCaptor = ArgumentCaptor.forClass(WorkLog.class);
        verify(storyRepo).addWorkLog(eq(storyId), workLogCaptor.capture());
        assertEquals(45, workLogCaptor.getValue().getComment().length());
    }

    @Test
    @DisplayName("addWorkLog should handle null comment")
    void testAddWorkLog_WithNullComment() {
        Long storyId = 1L;
        
        String result = sut.addWorkLog(storyId, 0, 1, 0, null, 1L);
        
        assertTrue(result.contains("redirect:/story/" + storyId));
        ArgumentCaptor<WorkLog> workLogCaptor = ArgumentCaptor.forClass(WorkLog.class);
        verify(storyRepo).addWorkLog(eq(storyId), workLogCaptor.capture());
        assertNull(workLogCaptor.getValue().getComment());
    }

    @Test
    @DisplayName("addWorkLog should calculate total minutes correctly")
    void testAddWorkLog_CalculatesTotalMinutes() {
        Long storyId = 1L;
        
        String result = sut.addWorkLog(storyId, 1, 2, 30, "Comment", 1L);
        
        assertTrue(result.contains("redirect:/story/" + storyId));
        ArgumentCaptor<WorkLog> workLogCaptor = ArgumentCaptor.forClass(WorkLog.class);
        verify(storyRepo).addWorkLog(eq(storyId), workLogCaptor.capture());
        // 1 day = 1440 min, 2 hours = 120 min, 30 minutes = 30 min, total = 1590
        assertEquals(1590, workLogCaptor.getValue().getDuration());
    }

    @Test
    @DisplayName("listStories should return all stories")
    void testListStories() throws IOException {
        Story story1 = new Story();
        story1.setId(1);
        Story story2 = new Story();
        story2.setId(2);
        
        when(storyRepo.findAll()).thenReturn(List.of(story1, story2));
        
        ModelAndView mav = sut.listStories();
        
        assertEquals("story-list", mav.getViewName());
        assertEquals(List.of(story1, story2), mav.getModel().get("stories"));
        verify(storyRepo).findAll();
    }

    @Test
    @DisplayName("showStory should redirect when story has null projectId")
    void testShowStory_WithNullProjectId() throws IOException {
        Long storyId = 1L;
        Story story = new Story();
        story.setId(1);
        story.setProjectId(null);
        
        when(storyRepo.find(storyId)).thenReturn(story);
        
        ModelAndView mav = sut.showStory(storyId);
        
        assertEquals("redirect:/", mav.getViewName());
    }

    @Test
    @DisplayName("editStory should handle column with null name")
    void testEditStory_WithColumnHavingNullName() {
        Long storyId = 1L;
        Story story = new Story();
        story.setId(1);
        story.setProjectId(10L);
        story.setColumnId(5L);
        
        Column column = new Column();
        column.setId(5);
        column.setName(null);
        
        when(storyRepo.find(storyId)).thenReturn(story);
        when(columnRepo.find(5L)).thenReturn(column);
        when(userRepo.getAll()).thenReturn(List.of());
        
        ModelAndView mav = sut.editStory(storyId);
        
        assertEquals("story-edit", mav.getViewName());
        assertEquals(column, mav.getModel().get("column"));
        assertFalse((Boolean) mav.getModel().get("isDefaultColumn"));
    }

    @Test
    @DisplayName("updateStory should handle invalid status value in catch block")
    void testUpdateStory_WithInvalidStatusValue() {
        Long storyId = 1L;
        Story story = new Story();
        story.setId(1);
        story.setProjectId(10L);
        story.setColumnId(5L);
        StoryStatus originalStatus = StoryStatus.BACKLOG;
        story.setStatus(originalStatus);
        
        Column column = new Column();
        column.setId(5);
        column.setName("Custom Column");
        
        when(storyRepo.find(storyId)).thenReturn(story);
        when(columnRepo.find(5L)).thenReturn(column);
        
        String result = sut.updateStory(storyId, "Title", "Description", "INVALID_STATUS");
        
        assertEquals("redirect:/board/10", result);
        assertEquals(originalStatus, story.getStatus(), "Status should remain unchanged after invalid value");
        verify(storyRepo).persist(story);
    }

    @Test
    @DisplayName("assignStory should redirect to home when story is null")
    void testAssignStory_WithNullStory() {
        Long storyId = 999L;
        
        when(storyRepo.find(storyId)).thenReturn(null);
        
        String result = sut.assignStory(storyId, 10);
        
        assertEquals("redirect:/", result);
        verify(storyRepo, never()).persist(any(Story.class));
    }

    @Test
    @DisplayName("assignStory should handle user not found")
    void testAssignStory_WithUserNotFound() {
        Long storyId = 1L;
        Story story = new Story();
        story.setId(1);
        story.setProjectId(10L);
        
        when(storyRepo.find(storyId)).thenReturn(story);
        when(userRepo.find(999)).thenReturn(null);
        
        String result = sut.assignStory(storyId, 999);
        
        assertEquals("redirect:/board/10", result);
        verify(storyRepo, never()).persist(any(Story.class));
    }

}
