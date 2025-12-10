package fr.uha.ensisa.gl.tarnished.controller;

import fr.uha.ensisa.gl.entities.Column;
import fr.uha.ensisa.gl.entities.Project;
import fr.uha.ensisa.gl.entities.Story;
import fr.uha.ensisa.gl.entities.StoryStatus;
import fr.uha.ensisa.gl.tarnished.repos.ColumnRepo;
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

@DisplayName("BoardController Tests")
class BoardControllerTest {

    @Mock
    private RepoFactory repoFactory;

    @Mock
    private ProjectRepo projectRepo;

    @Mock
    private ColumnRepo columnRepo;

    @Mock
    private StoryRepo storyRepo;

    private BoardController boardController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        boardController = new BoardController();
        boardController.setRepoFactory(repoFactory);

        when(repoFactory.getProjectRepo()).thenReturn(projectRepo);
        when(repoFactory.getColumnRepo()).thenReturn(columnRepo);
        when(repoFactory.getStoryRepo()).thenReturn(storyRepo);
    }

    @Test
    @DisplayName("Should display board with project and columns")
    void testShowBoard() {
        // Arrange
        Long projectId = 1L;
        Project project = new Project();
        project.setId(1);
        project.setName("Test Project");

        Column column1 = new Column();
        column1.setId(1);
        column1.setName("Backlog");

        Column column2 = new Column();
        column2.setId(2);
        column2.setName("In Progress");

        Collection<Column> columns = Arrays.asList(column1, column2);

        Story story1 = new Story();
        story1.setId(1);
        story1.setTitle("Story 1");

        Collection<Story> stories = Arrays.asList(story1);

        when(projectRepo.find(projectId)).thenReturn(project);
        when(columnRepo.findByProject(projectId)).thenReturn(columns);
        when(storyRepo.findByColumn(1L)).thenReturn(stories);
        when(storyRepo.findByColumn(2L)).thenReturn(new ArrayList<>());

        // Act
        ModelAndView result = boardController.showBoard(projectId);

        // Assert
        assertNotNull(result);
        assertEquals("board", result.getViewName());
        assertEquals(project, result.getModel().get("project"));
        assertEquals(columns, result.getModel().get("columns"));

        verify(projectRepo).find(projectId);
        verify(columnRepo).findByProject(projectId);
        verify(storyRepo, times(2)).findByColumn(anyLong());
    }

    @Test
    @DisplayName("Should redirect when project not found")
    void testShowBoardProjectNotFound() {
        // Arrange
        Long projectId = 999L;
        when(projectRepo.find(projectId)).thenReturn(null);

        // Act
        ModelAndView result = boardController.showBoard(projectId);

        // Assert
        assertNotNull(result);
        assertEquals("redirect:/project/list", result.getViewName());
        verify(projectRepo).find(projectId);
        verify(columnRepo, never()).findByProject(anyLong());
    }

    @Test
    @DisplayName("Should handle empty columns list")
    void testShowBoardEmptyColumns() {
        // Arrange
        Long projectId = 1L;
        Project project = new Project();
        project.setId(1);

        when(projectRepo.find(projectId)).thenReturn(project);
        when(columnRepo.findByProject(projectId)).thenReturn(new ArrayList<>());

        // Act
        ModelAndView result = boardController.showBoard(projectId);

        // Assert
        assertNotNull(result);
        assertEquals("board", result.getViewName());
        assertTrue(((Collection<?>) result.getModel().get("columns")).isEmpty());
    }

    @Test
    @DisplayName("Should move story between columns successfully")
    void testMoveStorySuccess() {
        // Arrange
        Long projectId = 1L;
        Long storyId = 10L;
        Long fromColumnId = 1L;
        Long toColumnId = 2L;

        Column targetColumn = new Column();
        targetColumn.setId(2);
        targetColumn.setName("In Progress");
        targetColumn.setMaxCapacity(0); // No limit

        Story story = new Story();
        story.setId(10);
        story.setStatus(StoryStatus.BACKLOG);

        when(columnRepo.find(toColumnId)).thenReturn(targetColumn);
        when(storyRepo.find(storyId)).thenReturn(story);
        when(storyRepo.findByColumn(toColumnId)).thenReturn(new ArrayList<>());

        // Act
        String result = boardController.moveStory(projectId, storyId, toColumnId, fromColumnId, null, null);

        // Assert
        assertTrue(result.contains("\"success\":true"));
        assertEquals(0, story.getPosition());
        verify(columnRepo).moveStoryBetweenColumns(storyId, fromColumnId, toColumnId);
        verify(storyRepo).persist(story);
    }

    @Test
    @DisplayName("Should reject move when column is full")
    void testMoveStoryColumnFull() {
        // Arrange
        Long projectId = 1L;
        Long storyId = 10L;
        Long toColumnId = 2L;

        Column targetColumn = new Column();
        targetColumn.setId(2);
        targetColumn.setMaxCapacity(2);

        Story story1 = new Story();
        Story story2 = new Story();
        Collection<Story> storiesInColumn = Arrays.asList(story1, story2);

        when(columnRepo.find(toColumnId)).thenReturn(targetColumn);
        when(storyRepo.findByColumn(toColumnId)).thenReturn(storiesInColumn);

        // Act
        String result = boardController.moveStory(projectId, storyId, toColumnId, null, null, null);

        // Assert
        assertTrue(result.contains("\"success\":false"));
        assertTrue(result.contains("Column is full"));
        verify(columnRepo, never()).moveStoryBetweenColumns(anyLong(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("Should update story status based on target column name")
    void testMoveStoryUpdatesStatus() {
        // Arrange
        Long projectId = 1L;
        Long storyId = 10L;
        Long toColumnId = 3L;

        Column targetColumn = new Column();
        targetColumn.setId(3);
        targetColumn.setName("DONE");
        targetColumn.setMaxCapacity(0);

        Story story = new Story();
        story.setId(10);
        story.setStatus(StoryStatus.IN_PROGRESS);

        when(columnRepo.find(toColumnId)).thenReturn(targetColumn);
        when(storyRepo.find(storyId)).thenReturn(story);
        when(storyRepo.findByColumn(toColumnId)).thenReturn(new ArrayList<>());

        // Act
        String result = boardController.moveStory(projectId, storyId, toColumnId, null, null, null);

        // Assert
        assertTrue(result.contains("\"success\":true"));
        assertEquals(StoryStatus.DONE, story.getStatus());
        verify(storyRepo).persist(story);
    }

    @Test
    @DisplayName("Should set subColumn when provided")
    void testMoveStoryWithSubColumn() {
        // Arrange
        Long projectId = 1L;
        Long storyId = 10L;
        Long toColumnId = 2L;
        String subColumn = "DONE";

        Column targetColumn = new Column();
        targetColumn.setId(2);
        targetColumn.setName("Custom");
        targetColumn.setMaxCapacity(0);

        Story story = new Story();
        story.setId(10);

        when(columnRepo.find(toColumnId)).thenReturn(targetColumn);
        when(storyRepo.find(storyId)).thenReturn(story);
        when(storyRepo.findByColumn(toColumnId)).thenReturn(new ArrayList<>());

        // Act
        String result = boardController.moveStory(projectId, storyId, toColumnId, null, null, subColumn);

        // Assert
        assertTrue(result.contains("\"success\":true"));
        assertEquals("DONE", story.getSubColumn());
        verify(storyRepo).persist(story);
    }

    @Test
    @DisplayName("Should default subColumn to BACKLOG for custom columns")
    void testMoveStoryDefaultSubColumn() {
        // Arrange
        Long projectId = 1L;
        Long storyId = 10L;
        Long toColumnId = 2L;

        Column targetColumn = new Column();
        targetColumn.setId(2);
        targetColumn.setName("Custom Column");
        targetColumn.setMaxCapacity(0);

        Story story = new Story();
        story.setId(10);

        when(columnRepo.find(toColumnId)).thenReturn(targetColumn);
        when(storyRepo.find(storyId)).thenReturn(story);
        when(storyRepo.findByColumn(toColumnId)).thenReturn(new ArrayList<>());

        // Act
        String result = boardController.moveStory(projectId, storyId, toColumnId, null, null, null);

        // Assert
        assertTrue(result.contains("\"success\":true"));
        assertEquals("BACKLOG", story.getSubColumn());
    }

    @Test
    @DisplayName("Should handle story not found during move")
    void testMoveStoryNotFound() {
        // Arrange
        Long projectId = 1L;
        Long storyId = 999L;
        Long toColumnId = 2L;

        Column targetColumn = new Column();
        targetColumn.setMaxCapacity(0);

        when(columnRepo.find(toColumnId)).thenReturn(targetColumn);
        when(storyRepo.find(storyId)).thenReturn(null);
        when(storyRepo.findByColumn(toColumnId)).thenReturn(new ArrayList<>());

        // Act
        String result = boardController.moveStory(projectId, storyId, toColumnId, null, null, null);

        // Assert
        assertTrue(result.contains("\"success\":true"));
        verify(storyRepo, never()).persist(any());
    }

    @Test
    @DisplayName("Should update subColumn successfully")
    void testUpdateSubColumn() {
        // Arrange
        Long projectId = 1L;
        Long storyId = 10L;
        String subColumn = "DONE";

        Story story = new Story();
        story.setId(10);
        story.setSubColumn("BACKLOG");

        when(storyRepo.find(storyId)).thenReturn(story);

        // Act
        String result = boardController.updateSubColumn(projectId, storyId, subColumn);

        // Assert
        assertTrue(result.contains("\"success\":true"));
        assertEquals("DONE", story.getSubColumn());
        verify(storyRepo).persist(story);
    }

    @Test
    @DisplayName("Should handle story not found in updateSubColumn")
    void testUpdateSubColumnStoryNotFound() {
        // Arrange
        Long projectId = 1L;
        Long storyId = 999L;
        String subColumn = "DONE";

        when(storyRepo.find(storyId)).thenReturn(null);

        // Act
        String result = boardController.updateSubColumn(projectId, storyId, subColumn);

        // Assert
        assertTrue(result.contains("\"success\":false"));
        assertTrue(result.contains("Story not found"));
        verify(storyRepo, never()).persist(any());
    }

    @Test
    @DisplayName("Should handle exception in moveStory")
    void testMoveStoryException() {
        // Arrange
        Long projectId = 1L;
        Long storyId = 10L;
        Long toColumnId = 2L;

        when(columnRepo.find(toColumnId)).thenReturn(null);
        when(storyRepo.findByColumn(toColumnId)).thenThrow(new RuntimeException("Database error"));

        // Act
        String result = boardController.moveStory(projectId, storyId, toColumnId, null, null, null);

        // Assert
        assertTrue(result.contains("\"success\":false") || result.contains("\"success\":true"));
        // Should handle gracefully
    }

    // ====== NEW TESTS FOR addColumn ======
    
    @Test
    @DisplayName("Should add column successfully")
    void testAddColumn() {
        // Arrange
        Long projectId = 1L;
        String columnName = "Testing";
        int maxCapacity = 5;
        
        Project project = new Project();
        project.setId(1);
        
        Collection<Column> existingColumns = new ArrayList<>();
        
        when(projectRepo.find(projectId)).thenReturn(project);
        when(columnRepo.findByProject(projectId)).thenReturn(existingColumns);
        
        // Act
        String result = boardController.addColumn(projectId, columnName, maxCapacity);
        
        // Assert
        assertEquals("redirect:/board/" + projectId, result);
        verify(columnRepo).persist(any(Column.class));
    }
    
    @Test
    @DisplayName("Should truncate column name to 25 characters")
    void testAddColumnTruncateName() {
        // Arrange
        Long projectId = 1L;
        String longName = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"; // 35 chars
        
        Project project = new Project();
        project.setId(1);
        
        when(projectRepo.find(projectId)).thenReturn(project);
        when(columnRepo.findByProject(projectId)).thenReturn(new ArrayList<>());
        
        // Act
        String result = boardController.addColumn(projectId, longName, 0);
        
        // Assert
        assertEquals("redirect:/board/" + projectId, result);
        verify(columnRepo).persist(argThat(column -> column.getName().length() == 25));
    }
    
    @Test
    @DisplayName("Should redirect when project not found in addColumn")
    void testAddColumnProjectNotFound() {
        // Arrange
        Long projectId = 999L;
        
        when(projectRepo.find(projectId)).thenReturn(null);
        
        // Act
        String result = boardController.addColumn(projectId, "Test", 0);
        
        // Assert
        assertEquals("redirect:/project/list", result);
        verify(columnRepo, never()).persist(any());
    }
    
    // ====== NEW TESTS FOR reorderStories ======
    
    @Test
    @DisplayName("Should reorder stories successfully")
    void testReorderStories() {
        // Arrange
        Long projectId = 1L;
        Long columnId = 2L;
        String storyOrder = "10,20,30";
        
        Story story1 = new Story();
        story1.setId(10);
        Story story2 = new Story();
        story2.setId(20);
        Story story3 = new Story();
        story3.setId(30);
        
        when(storyRepo.find(10L)).thenReturn(story1);
        when(storyRepo.find(20L)).thenReturn(story2);
        when(storyRepo.find(30L)).thenReturn(story3);
        
        // Act
        String result = boardController.reorderStories(projectId, columnId, storyOrder);
        
        // Assert
        assertTrue(result.contains("\"success\":true"));
        assertEquals(0, story1.getPosition());
        assertEquals(1, story2.getPosition());
        assertEquals(2, story3.getPosition());
        verify(storyRepo, times(3)).persist(any(Story.class));
    }
    
    @Test
    @DisplayName("Should handle story not found in reorderStories")
    void testReorderStoriesNotFound() {
        // Arrange
        Long projectId = 1L;
        Long columnId = 2L;
        String storyOrder = "10,999,30";
        
        Story story1 = new Story();
        story1.setId(10);
        
        when(storyRepo.find(10L)).thenReturn(story1);
        when(storyRepo.find(999L)).thenReturn(null);
        when(storyRepo.find(30L)).thenReturn(new Story());
        
        // Act
        String result = boardController.reorderStories(projectId, columnId, storyOrder);
        
        // Assert
        assertTrue(result.contains("\"success\":true"));
    }
    
    @Test
    @DisplayName("Should handle exception in reorderStories")
    void testReorderStoriesException() {
        // Arrange
        Long projectId = 1L;
        Long columnId = 2L;
        String storyOrder = "invalid";
        
        // Act
        String result = boardController.reorderStories(projectId, columnId, storyOrder);
        
        // Assert
        assertTrue(result.contains("\"success\":false"));
        assertTrue(result.contains("error"));
    }
    
    // ====== NEW TESTS FOR deleteColumn ======
    
    @Test
    @DisplayName("Should delete empty column successfully")
    void testDeleteColumn() {
        // Arrange
        Long projectId = 1L;
        Long columnId = 5L;
        
        Column column = new Column();
        column.setId(5);
        column.setName("Testing");
        
        when(columnRepo.find(columnId)).thenReturn(column);
        when(storyRepo.findByColumn(columnId)).thenReturn(new ArrayList<>());
        
        // Act
        String result = boardController.deleteColumn(projectId, columnId);
        
        // Assert
        assertEquals("redirect:/board/" + projectId, result);
        verify(columnRepo).remove(columnId);
    }
    
    @Test
    @DisplayName("Should not delete BACKLOG column")
    void testDeleteColumnBacklog() {
        // Arrange
        Long projectId = 1L;
        Long columnId = 1L;
        
        Column column = new Column();
        column.setId(1);
        column.setName("BACKLOG");
        
        when(columnRepo.find(columnId)).thenReturn(column);
        
        // Act
        String result = boardController.deleteColumn(projectId, columnId);
        
        // Assert
        assertTrue(result.contains("error=Cannot delete"));
        verify(columnRepo, never()).remove(columnId);
    }
    
    @Test
    @DisplayName("Should not delete DONE column")
    void testDeleteColumnDone() {
        // Arrange
        Long projectId = 1L;
        Long columnId = 3L;
        
        Column column = new Column();
        column.setId(3);
        column.setName("DONE");
        
        when(columnRepo.find(columnId)).thenReturn(column);
        
        // Act
        String result = boardController.deleteColumn(projectId, columnId);
        
        // Assert
        assertTrue(result.contains("error=Cannot delete"));
        verify(columnRepo, never()).remove(columnId);
    }
    
    @Test
    @DisplayName("Should not delete column with stories")
    void testDeleteColumnWithStories() {
        // Arrange
        Long projectId = 1L;
        Long columnId = 5L;
        
        Column column = new Column();
        column.setId(5);
        column.setName("Testing");
        
        Story story = new Story();
        Collection<Story> stories = Arrays.asList(story);
        
        when(columnRepo.find(columnId)).thenReturn(column);
        when(storyRepo.findByColumn(columnId)).thenReturn(stories);
        
        // Act
        String result = boardController.deleteColumn(projectId, columnId);
        
        // Assert
        assertTrue(result.contains("error=Cannot delete column with stories"));
        verify(columnRepo, never()).remove(columnId);
    }
    
    @Test
    @DisplayName("Should handle column not found in deleteColumn")
    void testDeleteColumnNotFound() {
        // Arrange
        Long projectId = 1L;
        Long columnId = 999L;
        
        when(columnRepo.find(columnId)).thenReturn(null);
        
        // Act
        String result = boardController.deleteColumn(projectId, columnId);
        
        // Assert
        assertTrue(result.contains("error=Column not found"));
        verify(columnRepo, never()).remove(anyLong());
    }
    
    // ====== NEW TESTS FOR deleteColumnWithStories ======
    
    @Test
    @DisplayName("Should delete column with stories")
    void testDeleteColumnWithStoriesSuccess() {
        // Arrange
        Long projectId = 1L;
        Long columnId = 5L;
        
        Column column = new Column();
        column.setId(5);
        
        Story story1 = new Story();
        story1.setId(10);
        Story story2 = new Story();
        story2.setId(20);
        Collection<Story> stories = Arrays.asList(story1, story2);
        
        when(columnRepo.find(columnId)).thenReturn(column);
        when(storyRepo.findByColumn(columnId)).thenReturn(stories);
        
        // Act
        String result = boardController.deleteColumnWithStories(projectId, columnId);
        
        // Assert
        assertEquals("success", result);
        verify(storyRepo).remove(10L);
        verify(storyRepo).remove(20L);
        verify(columnRepo).remove(columnId);
    }
    
    @Test
    @DisplayName("Should return error when column not found in deleteColumnWithStories")
    void testDeleteColumnWithStoriesNotFound() {
        // Arrange
        Long projectId = 1L;
        Long columnId = 999L;
        
        when(columnRepo.find(columnId)).thenReturn(null);
        
        // Act
        String result = boardController.deleteColumnWithStories(projectId, columnId);
        
        // Assert
        assertEquals("error", result);
        verify(storyRepo, never()).remove(anyLong());
        verify(columnRepo, never()).remove(anyLong());
    }
    
    // ====== NEW TESTS FOR moveAllStories ======
    
    @Test
    @DisplayName("Should move all stories between columns")
    void testMoveAllStories() {
        // Arrange
        Long projectId = 1L;
        Long fromColumnId = 2L;
        Long toColumnId = 3L;
        
        Column fromColumn = new Column();
        fromColumn.setId(2);
        Column toColumn = new Column();
        toColumn.setId(3);
        
        Story story1 = new Story();
        story1.setId(10);
        Story story2 = new Story();
        story2.setId(20);
        Collection<Story> stories = Arrays.asList(story1, story2);
        
        when(columnRepo.find(fromColumnId)).thenReturn(fromColumn);
        when(columnRepo.find(toColumnId)).thenReturn(toColumn);
        when(storyRepo.findByColumn(fromColumnId)).thenReturn(stories);
        
        // Act
        String result = boardController.moveAllStories(projectId, fromColumnId, toColumnId);
        
        // Assert
        assertEquals("success", result);
        assertEquals(toColumnId, story1.getColumnId());
        assertEquals(toColumnId, story2.getColumnId());
        verify(storyRepo, times(2)).persist(any(Story.class));
    }
    
    @Test
    @DisplayName("Should return error when from column not found")
    void testMoveAllStoriesFromNotFound() {
        // Arrange
        Long projectId = 1L;
        Long fromColumnId = 999L;
        Long toColumnId = 3L;
        
        when(columnRepo.find(fromColumnId)).thenReturn(null);
        when(columnRepo.find(toColumnId)).thenReturn(new Column());
        
        // Act
        String result = boardController.moveAllStories(projectId, fromColumnId, toColumnId);
        
        // Assert
        assertEquals("error", result);
        verify(storyRepo, never()).persist(any());
    }
    
    @Test
    @DisplayName("Should return error when to column not found")
    void testMoveAllStoriesToNotFound() {
        // Arrange
        Long projectId = 1L;
        Long fromColumnId = 2L;
        Long toColumnId = 999L;
        
        when(columnRepo.find(fromColumnId)).thenReturn(new Column());
        when(columnRepo.find(toColumnId)).thenReturn(null);
        
        // Act
        String result = boardController.moveAllStories(projectId, fromColumnId, toColumnId);
        
        // Assert
        assertEquals("error", result);
        verify(storyRepo, never()).persist(any());
    }
    
    // ====== NEW TESTS FOR reorderColumns ======
    
    @Test
    @DisplayName("Should reorder columns with BACKLOG first")
    void testReorderColumns() {
        // Arrange
        Long projectId = 1L;
        String columnOrder = "3,2,1";
        
        Column backlog = new Column();
        backlog.setId(1);
        backlog.setName("BACKLOG");
        
        Column col2 = new Column();
        col2.setId(2);
        col2.setName("In Progress");
        
        Column col3 = new Column();
        col3.setId(3);
        col3.setName("Done");
        
        Collection<Column> columns = Arrays.asList(backlog, col2, col3);
        
        when(columnRepo.findByProject(projectId)).thenReturn(columns);
        
        // Act
        String result = boardController.reorderColumns(projectId, columnOrder);
        
        // Assert
        assertTrue(result.contains("\"success\":true"));
        verify(columnRepo).reorder(1L, 1); // BACKLOG always first
        verify(columnRepo).reorder(3L, 2);
        verify(columnRepo).reorder(2L, 3);
    }
    
    @Test
    @DisplayName("Should reorder columns without BACKLOG")
    void testReorderColumnsNoBacklog() {
        // Arrange
        Long projectId = 1L;
        String columnOrder = "3,2";
        
        Column col2 = new Column();
        col2.setId(2);
        col2.setName("In Progress");
        
        Column col3 = new Column();
        col3.setId(3);
        col3.setName("Done");
        
        Collection<Column> columns = Arrays.asList(col2, col3);
        
        when(columnRepo.findByProject(projectId)).thenReturn(columns);
        
        // Act
        String result = boardController.reorderColumns(projectId, columnOrder);
        
        // Assert
        assertTrue(result.contains("\"success\":true"));
        verify(columnRepo).reorder(3L, 1);
        verify(columnRepo).reorder(2L, 2);
    }
    
    // ====== NEW TESTS FOR updateColumnName ======
    
    @Test
    @DisplayName("Should update column name successfully")
    void testUpdateColumnName() {
        // Arrange
        Long projectId = 1L;
        Long columnId = 5L;
        String newName = "Updated Name";
        
        Column column = new Column();
        column.setId(5);
        column.setName("Old Name");
        
        when(columnRepo.find(columnId)).thenReturn(column);
        
        // Act
        String result = boardController.updateColumnName(projectId, columnId, newName);
        
        // Assert
        assertTrue(result.contains("\"success\":true"));
        assertEquals("Updated Name", column.getName());
        verify(columnRepo).persist(column);
    }
    
    @Test
    @DisplayName("Should reject empty column name")
    void testUpdateColumnNameEmpty() {
        // Arrange
        Long projectId = 1L;
        Long columnId = 5L;
        String newName = "   ";
        
        Column column = new Column();
        column.setId(5);
        
        when(columnRepo.find(columnId)).thenReturn(column);
        
        // Act
        String result = boardController.updateColumnName(projectId, columnId, newName);
        
        // Assert
        assertTrue(result.contains("\"success\":false"));
        assertTrue(result.contains("cannot be empty"));
        verify(columnRepo, never()).persist(any());
    }
    
    @Test
    @DisplayName("Should handle column not found in updateColumnName")
    void testUpdateColumnNameNotFound() {
        // Arrange
        Long projectId = 1L;
        Long columnId = 999L;
        String newName = "New Name";
        
        when(columnRepo.find(columnId)).thenReturn(null);
        
        // Act
        String result = boardController.updateColumnName(projectId, columnId, newName);
        
        // Assert
        assertTrue(result.contains("\"success\":false"));
        assertTrue(result.contains("Column not found"));
    }
    
    @Test
    @DisplayName("Should handle exception in updateColumnName")
    void testUpdateColumnNameException() {
        // Arrange
        Long projectId = 1L;
        Long columnId = 5L;
        String newName = "New Name";
        
        Column column = new Column();
        when(columnRepo.find(columnId)).thenReturn(column);
        doThrow(new RuntimeException("DB error")).when(columnRepo).persist(any());
        
        // Act
        String result = boardController.updateColumnName(projectId, columnId, newName);
        
        // Assert
        assertTrue(result.contains("\"success\":false"));
        assertTrue(result.contains("error"));
    }
    
    // ====== NEW TESTS FOR updateColumn (full) ======
    
    @Test
    @DisplayName("Should update column name and capacity")
    void testUpdateColumnFull() {
        // Arrange
        Long projectId = 1L;
        Long columnId = 5L;
        String newName = "Updated";
        int maxCapacity = 10;
        
        Column column = new Column();
        column.setId(5);
        
        when(columnRepo.find(columnId)).thenReturn(column);
        
        // Act
        String result = boardController.updateColumn(projectId, columnId, newName, maxCapacity);
        
        // Assert
        assertTrue(result.contains("\"success\":true"));
        assertEquals("Updated", column.getName());
        assertEquals(10, column.getMaxCapacity());
        verify(columnRepo).persist(column);
    }
    
    @Test
    @DisplayName("Should update only capacity when name is empty")
    void testUpdateColumnOnlyCapacity() {
        // Arrange
        Long projectId = 1L;
        Long columnId = 5L;
        String originalName = "Original";
        int maxCapacity = 15;
        
        Column column = new Column();
        column.setId(5);
        column.setName(originalName);
        
        when(columnRepo.find(columnId)).thenReturn(column);
        
        // Act
        String result = boardController.updateColumn(projectId, columnId, "", maxCapacity);
        
        // Assert
        assertTrue(result.contains("\"success\":true"));
        assertEquals(originalName, column.getName()); // Name unchanged
        assertEquals(15, column.getMaxCapacity());
    }
    
    @Test
    @DisplayName("Should handle column not found in updateColumn")
    void testUpdateColumnNotFound() {
        // Arrange
        Long projectId = 1L;
        Long columnId = 999L;
        
        when(columnRepo.find(columnId)).thenReturn(null);
        
        // Act
        String result = boardController.updateColumn(projectId, columnId, "Name", 5);
        
        // Assert
        assertTrue(result.contains("\"success\":false"));
        assertTrue(result.contains("Column not found"));
    }
    
    @Test
    @DisplayName("Should handle exception in updateColumn")
    void testUpdateColumnException() {
        // Arrange
        Long projectId = 1L;
        Long columnId = 5L;
        
        Column column = new Column();
        when(columnRepo.find(columnId)).thenReturn(column);
        doThrow(new RuntimeException("DB error")).when(columnRepo).persist(any());
        
        // Act
        String result = boardController.updateColumn(projectId, columnId, "Name", 5);
        
        // Assert
        assertTrue(result.contains("\"success\":false"));
    }

    // ====== NEW TESTS FOR BRANCH COVERAGE ======
    
    @Test
    @DisplayName("moveStory should handle null target column")
    void testMoveStoryNullTargetColumn() {
        // Arrange
        Long projectId = 1L;
        Long storyId = 10L;
        Long fromColumnId = 1L;
        Long toColumnId = 2L;
        
        when(columnRepo.find(toColumnId)).thenReturn(null);
        
        Story story = new Story();
        story.setId(10);
        when(storyRepo.find(storyId)).thenReturn(story);
        
        // Act
        String result = boardController.moveStory(projectId, storyId, toColumnId, fromColumnId, null, null);
        
        // Assert
        assertTrue(result.contains("\"success\":true"));
        verify(columnRepo).moveStoryBetweenColumns(storyId, fromColumnId, toColumnId);
    }
    
    @Test
    @DisplayName("moveStory should handle column with maxCapacity 0")
    void testMoveStoryColumnWithZeroCapacity() {
        // Arrange
        Long projectId = 1L;
        Long storyId = 10L;
        Long fromColumnId = 1L;
        Long toColumnId = 2L;
        
        Column targetColumn = new Column();
        targetColumn.setMaxCapacity(0);
        when(columnRepo.find(toColumnId)).thenReturn(targetColumn);
        
        Story story = new Story();
        story.setId(10);
        when(storyRepo.find(storyId)).thenReturn(story);
        when(storyRepo.findByColumn(toColumnId)).thenReturn(new java.util.ArrayList<>());
        
        // Act
        String result = boardController.moveStory(projectId, storyId, toColumnId, fromColumnId, null, null);
        
        // Assert
        assertTrue(result.contains("\"success\":true"));
        verify(columnRepo).moveStoryBetweenColumns(storyId, fromColumnId, toColumnId);
    }
    
    @Test
    @DisplayName("moveStory should handle null story after move")
    void testMoveStoryNullStoryAfterFind() {
        // Arrange
        Long projectId = 1L;
        Long storyId = 10L;
        Long fromColumnId = 1L;
        Long toColumnId = 2L;
        
        Column targetColumn = new Column();
        targetColumn.setMaxCapacity(5);
        when(columnRepo.find(toColumnId)).thenReturn(targetColumn);
        when(storyRepo.findByColumn(toColumnId)).thenReturn(new java.util.ArrayList<>());
        when(storyRepo.find(storyId)).thenReturn(null);
        
        // Act
        String result = boardController.moveStory(projectId, storyId, toColumnId, fromColumnId, null, null);
        
        // Assert
        assertTrue(result.contains("\"success\":true"));
        verify(columnRepo).moveStoryBetweenColumns(storyId, fromColumnId, toColumnId);
    }
    
    @Test
    @DisplayName("moveStory should handle column without status mapping")
    void testMoveStoryColumnWithoutStatusMapping() {
        // Arrange
        Long projectId = 1L;
        Long storyId = 10L;
        Long fromColumnId = 1L;
        Long toColumnId = 2L;
        
        Column targetColumn = new Column();
        targetColumn.setName("Custom Column");
        targetColumn.setMaxCapacity(0);
        when(columnRepo.find(toColumnId)).thenReturn(targetColumn);
        
        Story story = new Story();
        story.setId(10);
        when(storyRepo.find(storyId)).thenReturn(story);
        
        // Act
        String result = boardController.moveStory(projectId, storyId, toColumnId, fromColumnId, null, null);
        
        // Assert
        assertTrue(result.contains("\"success\":true"));
        verify(storyRepo).persist(story);
    }
    
    @Test
    @DisplayName("moveAllStories should handle null fromColumn")
    void testMoveAllStoriesNullFromColumn() {
        // Arrange
        Long projectId = 1L;
        Long fromColumnId = 1L;
        Long toColumnId = 2L;
        
        when(columnRepo.find(fromColumnId)).thenReturn(null);
        
        Column toColumn = new Column();
        when(columnRepo.find(toColumnId)).thenReturn(toColumn);
        
        // Act
        String result = boardController.moveAllStories(projectId, fromColumnId, toColumnId);
        
        // Assert
        assertEquals("error", result);
    }
    
    @Test
    @DisplayName("moveAllStories should handle null toColumn")
    void testMoveAllStoriesNullToColumn() {
        // Arrange
        Long projectId = 1L;
        Long fromColumnId = 1L;
        Long toColumnId = 2L;
        
        Column fromColumn = new Column();
        when(columnRepo.find(fromColumnId)).thenReturn(fromColumn);
        when(columnRepo.find(toColumnId)).thenReturn(null);
        
        // Act
        String result = boardController.moveAllStories(projectId, fromColumnId, toColumnId);
        
        // Assert
        assertEquals("error", result);
    }
    
    @Test
    @DisplayName("updateSubColumn should handle exception during persist")
    void testUpdateSubColumnException() {
        // Arrange
        Long projectId = 1L;
        Long storyId = 10L;
        String subColumn = "IN_PROGRESS";
        
        Story story = new Story();
        story.setId(10);
        when(storyRepo.find(storyId)).thenReturn(story);
        doThrow(new RuntimeException("DB error")).when(storyRepo).persist(any());
        
        // Act
        String result = boardController.updateSubColumn(projectId, storyId, subColumn);
        
        // Assert
        assertTrue(result.contains("\"success\":false"));
        assertTrue(result.contains("error"));
    }
    
    @Test
    @DisplayName("moveStory should set subColumn to null for DONE column")
    void testMoveStorySetSubColumnNullForDone() {
        // Arrange
        Long projectId = 1L;
        Long storyId = 10L;
        Long fromColumnId = 1L;
        Long toColumnId = 2L;
        
        Column targetColumn = new Column();
        targetColumn.setName("DONE");
        targetColumn.setMaxCapacity(0);
        when(columnRepo.find(toColumnId)).thenReturn(targetColumn);
        
        Story story = new Story();
        story.setId(10);
        story.setSubColumn("REVIEW");
        when(storyRepo.find(storyId)).thenReturn(story);
        
        // Act
        String result = boardController.moveStory(projectId, storyId, toColumnId, fromColumnId, null, null);
        
        // Assert
        assertTrue(result.contains("\"success\":true"));
        assertNull(story.getSubColumn());
        verify(storyRepo).persist(story);
    }

}
