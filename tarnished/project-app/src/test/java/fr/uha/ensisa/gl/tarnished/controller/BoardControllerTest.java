package fr.uha.ensisa.gl.tarnished.controller;

import fr.uha.ensisa.gl.entities.Column;
import fr.uha.ensisa.gl.entities.Project;
import fr.uha.ensisa.gl.entities.Story;
import fr.uha.ensisa.gl.entities.StoryStatus;
import fr.uha.ensisa.gl.entities.Swimlane;
import fr.uha.ensisa.gl.tarnished.config.PathHelper;
import fr.uha.ensisa.gl.tarnished.repos.ColumnRepo;
import fr.uha.ensisa.gl.tarnished.repos.ProjectRepo;
import fr.uha.ensisa.gl.tarnished.repos.RepoFactory;
import fr.uha.ensisa.gl.tarnished.repos.StoryRepo;
import fr.uha.ensisa.gl.tarnished.repos.SwimlaneRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.servlet.ModelAndView;
import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BoardControllerTest {

    @Mock
    private RepoFactory repoFactory;

    @Mock
    private ProjectRepo projectRepo;

    @Mock
    private ColumnRepo columnRepo;

    @Mock
    private StoryRepo storyRepo;

    @Mock
    private SwimlaneRepo swimlaneRepo;

    @Mock
    private PathHelper pathHelper;

    @InjectMocks
    private BoardController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(repoFactory.getProjectRepo()).thenReturn(projectRepo);
        when(repoFactory.getColumnRepo()).thenReturn(columnRepo);
        when(repoFactory.getStoryRepo()).thenReturn(storyRepo);
        when(repoFactory.getSwimlaneRepo()).thenReturn(swimlaneRepo);
        controller.setPathHelper(pathHelper);
        
        // Configure PathHelper mock
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
    void testShowBoard() {
        Long projectId = 1L;
        Project project = new Project();
        project.setId(1);
        project.setName("Test Project");

        Column column1 = new Column();
        column1.setId(1);
        column1.setName("BACKLOG");
        column1.setHasSubColumns(false);

        Column column2 = new Column();
        column2.setId(2);
        column2.setName("IN PROGRESS");
        column2.setHasSubColumns(false);

        List<Column> columns = new ArrayList<>();
        columns.add(column1);
        columns.add(column2);

        List<Story> stories = new ArrayList<>();
        Story story1 = new Story();
        story1.setId(1);
        story1.setTitle("Test Story");
        stories.add(story1);

        when(projectRepo.find(projectId)).thenReturn(project);
        when(columnRepo.findByProject(projectId)).thenReturn(columns);
        when(storyRepo.findByColumn(anyLong())).thenReturn(stories);
        when(swimlaneRepo.findAll()).thenReturn(new ArrayList<>());

        ModelAndView mav = controller.showBoard(projectId);

        assertNotNull(mav);
        assertEquals("board", mav.getViewName());
        assertEquals(project, mav.getModel().get("project"));
        assertNotNull(mav.getModel().get("columns"));

        verify(projectRepo).find(projectId);
        verify(columnRepo).findByProject(projectId);

        // Verify setStories was called
        ArgumentCaptor<Column> columnCaptor = ArgumentCaptor.forClass(Column.class);
        verify(columnRepo, atLeastOnce()).persist(columnCaptor.capture());

        Collection<Column> capturedColumns = columnCaptor.getAllValues();
        assertTrue(capturedColumns.stream().anyMatch(col -> col.getStories() != null));
    }

    @Test
    void testShowBoard_WithEmptySwimlanesList() {
        Long projectId = 1L;
        Project project = new Project();
        project.setId(1);

        when(projectRepo.find(projectId)).thenReturn(project);
        when(columnRepo.findByProject(projectId)).thenReturn(new ArrayList<>());
        when(swimlaneRepo.findAll()).thenReturn(new ArrayList<>());

        ModelAndView mav = controller.showBoard(projectId);

        assertNotNull(mav);
        assertEquals("board", mav.getViewName());
        assertFalse((Boolean) mav.getModel().get("hasSwimlanes"));
    }


    @Test
    void testShowBoardWithNonExistentProject() {
        Long projectId = 999L;
        when(projectRepo.find(projectId)).thenReturn(null);
        when(swimlaneRepo.findAll()).thenReturn(new ArrayList<>());

        ModelAndView mav = controller.showBoard(projectId);

        assertNotNull(mav);
        assertEquals("redirect:/project/list", mav.getViewName());
        verify(projectRepo).find(projectId);
    }

    @Test
    void testShowBoardInitializesSubColumns() {
        Long projectId = 1L;
        Project project = new Project();
        project.setId(1);

        Column inProgressColumn = new Column();
        inProgressColumn.setId(1);
        inProgressColumn.setName("IN PROGRESS");
        inProgressColumn.setHasSubColumns(false);

        List<Column> columns = new ArrayList<>();
        columns.add(inProgressColumn);

        when(projectRepo.find(projectId)).thenReturn(project);
        when(columnRepo.findByProject(projectId)).thenReturn(columns);
        when(storyRepo.findByColumn(anyLong())).thenReturn(new ArrayList<>());
        when(swimlaneRepo.findAll()).thenReturn(new ArrayList<>());

        controller.showBoard(projectId);

        verify(columnRepo).persist(inProgressColumn);
        assertTrue(inProgressColumn.isHasSubColumns());
    }

    @Test
    void testMoveStory() {
        Long projectId = 1L;
        Long storyId = 1L;
        Long toColumnId = 2L;
        Long fromColumnId = 1L;

        Column targetColumn = new Column();
        targetColumn.setId(2);
        targetColumn.setName("IN PROGRESS");
        targetColumn.setMaxCapacity(0);

        Story story = new Story();
        story.setId(1);
        story.setTitle("Test Story");
        story.setStatus(StoryStatus.BACKLOG);

        when(columnRepo.find(toColumnId)).thenReturn(targetColumn);
        when(storyRepo.find(storyId)).thenReturn(story);
        when(storyRepo.findByColumn(toColumnId)).thenReturn(new ArrayList<>());

        String result = controller.moveStory(projectId, storyId, toColumnId, fromColumnId, null, null, null);

        assertTrue(result.contains("success"));
        verify(columnRepo).moveStoryBetweenColumns(storyId, fromColumnId, toColumnId);
        verify(storyRepo).persist(story);
    }

    @Test
    void testMoveStoryToFullColumn() {
        Long projectId = 1L;
        Long storyId = 1L;
        Long toColumnId = 2L;

        Column targetColumn = new Column();
        targetColumn.setId(2);
        targetColumn.setName("IN PROGRESS");
        targetColumn.setMaxCapacity(1);

        List<Story> stories = new ArrayList<>();
        Story existingStory = new Story();
        existingStory.setId(2);
        stories.add(existingStory);

        when(columnRepo.find(toColumnId)).thenReturn(targetColumn);
        when(storyRepo.findByColumn(toColumnId)).thenReturn(stories);

        String result = controller.moveStory(projectId, storyId, toColumnId, null, null, null, null);

        assertTrue(result.contains("success\":false"));
        assertTrue(result.contains("Column is full"));
        verify(columnRepo, never()).moveStoryBetweenColumns(anyLong(), anyLong(), anyLong());
    }

    @Test
    void testMoveStoryWithSubColumn() {
        Long projectId = 1L;
        Long storyId = 1L;
        Long toColumnId = 2L;
        String subColumn = "DOING";

        Column targetColumn = new Column();
        targetColumn.setId(2);
        targetColumn.setName("IN PROGRESS");
        targetColumn.setMaxCapacity(0);

        Story story = new Story();
        story.setId(1);
        story.setTitle("Test Story");

        when(columnRepo.find(toColumnId)).thenReturn(targetColumn);
        when(storyRepo.find(storyId)).thenReturn(story);
        when(storyRepo.findByColumn(toColumnId)).thenReturn(new ArrayList<>());

        String result = controller.moveStory(projectId, storyId, toColumnId, null, null, subColumn, null);

        assertTrue(result.contains("success"));
        assertEquals(subColumn, story.getSubColumn());
        verify(storyRepo).persist(story);
    }

    @Test
    void testMoveStory_VerifiesSetSubColumnCalled() {
        Long projectId = 1L;
        Long storyId = 1L;
        Long toColumnId = 2L;
        String subColumn = "DOING";

        Column targetColumn = new Column();
        targetColumn.setId(2);
        targetColumn.setName("Custom Column");
        targetColumn.setMaxCapacity(0);

        Story story = new Story();
        story.setId(1);
        story.setSubColumn(null);

        when(columnRepo.find(toColumnId)).thenReturn(targetColumn);
        when(storyRepo.find(storyId)).thenReturn(story);
        when(storyRepo.findByColumn(toColumnId)).thenReturn(new ArrayList<>());

        String result = controller.moveStory(projectId, storyId, toColumnId, null, null, subColumn, null);

        assertTrue(result.contains("success"));
        assertEquals(subColumn, story.getSubColumn());
        verify(storyRepo).persist(story);
    }

    @Test
    void testMoveStory_WithCustomColumn_VerifiesSetSubColumnDefaultToBacklog() {
        Long projectId = 1L;
        Long storyId = 1L;
        Long toColumnId = 2L;

        Column customColumn = new Column();
        customColumn.setId(2);
        customColumn.setName("Custom Column"); // Not BACKLOG or DONE
        customColumn.setMaxCapacity(0);

        Story story = new Story();
        story.setId(1);
        story.setSubColumn(null);

        when(columnRepo.find(toColumnId)).thenReturn(customColumn);
        when(storyRepo.find(storyId)).thenReturn(story);
        when(storyRepo.findByColumn(toColumnId)).thenReturn(new ArrayList<>());

        String result = controller.moveStory(projectId, storyId, toColumnId, null, null, null, null);

        assertTrue(result.contains("success"));
        assertEquals("BACKLOG", story.getSubColumn()); // Vérifie que setSubColumn("BACKLOG") a été appelé
        verify(storyRepo).persist(story);
    }

    @Test
    void testUpdateSubColumn() {
        Long projectId = 1L;
        Long storyId = 1L;
        String subColumn = "DOING";

        Story story = new Story();
        story.setId(1);
        story.setTitle("Test Story");

        when(storyRepo.find(storyId)).thenReturn(story);

        String result = controller.updateSubColumn(projectId, storyId, subColumn);

        assertTrue(result.contains("success"));
        assertEquals(subColumn, story.getSubColumn());
        verify(storyRepo).persist(story);
    }

    @Test
    void testUpdateSubColumnWithNonExistentStory() {
        Long projectId = 1L;
        Long storyId = 999L;
        String subColumn = "DOING";

        when(storyRepo.find(storyId)).thenReturn(null);

        String result = controller.updateSubColumn(projectId, storyId, subColumn);

        assertTrue(result.contains("success\":false"));
    }

    @Test
    void testReorderStories() {
        Long projectId = 1L;
        Long columnId = 1L;
        String storyIds = "1,2,3";

        Story story1 = new Story();
        story1.setId(1);
        Story story2 = new Story();
        story2.setId(2);
        Story story3 = new Story();
        story3.setId(3);

        when(storyRepo.find(1L)).thenReturn(story1);
        when(storyRepo.find(2L)).thenReturn(story2);
        when(storyRepo.find(3L)).thenReturn(story3);

        String result = controller.reorderStories(projectId, columnId, storyIds);

        assertTrue(result.contains("success"));
        verify(storyRepo, times(3)).persist(any(Story.class));
    }

    @Test
    void testUpdateColumn() {
        Long projectId = 1L;
        Long columnId = 1L;
        String columnName = "New Name";
        int maxCapacity = 10;

        Column column = new Column();
        column.setId(1);
        column.setName("Old Name");

        when(columnRepo.find(columnId)).thenReturn(column);

        String result = controller.updateColumn(projectId, columnId, columnName, maxCapacity);

        assertTrue(result.contains("success"));
        assertEquals(columnName, column.getName());
        assertEquals(maxCapacity, column.getMaxCapacity());
        verify(columnRepo).persist(column);
    }

    @Test
    void testUpdateColumnName() {
        Long projectId = 1L;
        Long columnId = 1L;
        String newName = "Updated Name";

        Column column = new Column();
        column.setId(1);
        column.setName("Old Name");

        when(columnRepo.find(columnId)).thenReturn(column);

        String result = controller.updateColumnName(projectId, columnId, newName);

        assertTrue(result.contains("success"));
        assertEquals(newName, column.getName());
        verify(columnRepo).persist(column);
    }

    @Test
    void testAddColumn() {
        Long projectId = 1L;
        String name = "New Column";
        int maxCapacity = 5;
        boolean hasSubColumns = true;

        Project project = new Project();
        project.setId(1);
        project.setName("Test Project");

        List<Column> existingColumns = new ArrayList<>();
        Column col1 = new Column();
        col1.setId(1);
        existingColumns.add(col1);

        when(projectRepo.find(projectId)).thenReturn(project);
        when(columnRepo.findByProject(projectId)).thenReturn(existingColumns);

        String result = controller.addColumn(projectId, name, maxCapacity, hasSubColumns);

        assertEquals("redirect:/board/" + projectId, result);
        verify(columnRepo).persist(any(Column.class));
    }

    @Test
    void testAddColumnWithLongName() {
        Long projectId = 1L;
        String longName = "This is a very long column name that exceeds 25 characters";

        Project project = new Project();
        project.setId(1);

        when(projectRepo.find(projectId)).thenReturn(project);
        when(columnRepo.findByProject(projectId)).thenReturn(new ArrayList<>());

        String result = controller.addColumn(projectId, longName, 0, false);

        assertEquals("redirect:/board/" + projectId, result);
        verify(columnRepo).persist(any(Column.class));
    }

    @Test
    void testAddColumnWithNonExistentProject() {
        Long projectId = 999L;
        String name = "New Column";

        when(projectRepo.find(projectId)).thenReturn(null);

        String result = controller.addColumn(projectId, name, 0, false);

        assertEquals("redirect:/project/list", result);
        verify(columnRepo, never()).persist(any(Column.class));
    }

    @Test
    void testAddColumnGet() {
        Long projectId = 1L;
        String name = "New Column";

        Project project = new Project();
        project.setId(1);

        when(projectRepo.find(projectId)).thenReturn(project);
        when(columnRepo.findByProject(projectId)).thenReturn(new ArrayList<>());

        String result = controller.addColumnGet(projectId, name, 0, false);

        assertEquals("redirect:/board/" + projectId, result);
    }

    @Test
    void testDeleteColumn() {
        Long projectId = 1L;
        Long columnId = 1L;

        Column column = new Column();
        column.setId(1);
        column.setName("Custom Column");

        when(columnRepo.find(columnId)).thenReturn(column);
        when(storyRepo.findByColumn(columnId)).thenReturn(new ArrayList<>());

        String result = controller.deleteColumn(projectId, columnId);

        assertEquals("redirect:/board/" + projectId, result);
        verify(columnRepo).remove(columnId);
    }

    @Test
    void testDeleteColumnWithNonExistentColumn() {
        Long projectId = 1L;
        Long columnId = 999L;

        when(columnRepo.find(columnId)).thenReturn(null);

        String result = controller.deleteColumn(projectId, columnId);

        assertTrue(result.contains("error"));
        verify(columnRepo, never()).remove(anyLong());
    }

    @Test
    void testDeleteBacklogColumn() {
        Long projectId = 1L;
        Long columnId = 1L;

        Column column = new Column();
        column.setId(1);
        column.setName("BACKLOG");

        when(columnRepo.find(columnId)).thenReturn(column);

        String result = controller.deleteColumn(projectId, columnId);

        assertTrue(result.contains("error"));
        assertTrue(result.contains("Cannot delete"));
        verify(columnRepo, never()).remove(anyLong());
    }

    @Test
    void testDeleteColumnWithStories() {
        Long projectId = 1L;
        Long columnId = 1L;

        Column column = new Column();
        column.setId(1);
        column.setName("Custom Column");

        List<Story> stories = new ArrayList<>();
        Story story = new Story();
        story.setId(1);
        stories.add(story);

        when(columnRepo.find(columnId)).thenReturn(column);
        when(storyRepo.findByColumn(columnId)).thenReturn(stories);

        String result = controller.deleteColumn(projectId, columnId);

        assertTrue(result.contains("error"));
        assertTrue(result.contains("move stories first"));
        verify(columnRepo, never()).remove(anyLong());
    }

    @Test
    void testDeleteColumnWithStoriesAjax() {
        Long projectId = 1L;
        Long columnId = 1L;

        Column column = new Column();
        column.setId(1);
        column.setName("Custom Column");

        List<Story> stories = new ArrayList<>();
        Story story = new Story();
        story.setId(1);
        stories.add(story);

        when(columnRepo.find(columnId)).thenReturn(column);
        when(storyRepo.findByColumn(columnId)).thenReturn(stories);

        String result = controller.deleteColumnWithStories(projectId, columnId);

        assertEquals("success", result);
        verify(storyRepo).remove(1L);
        verify(columnRepo).remove(columnId);
    }

    @Test
    void testMoveAllStories() {
        Long projectId = 1L;
        Long fromColumnId = 1L;
        Long toColumnId = 2L;

        Column fromColumn = new Column();
        fromColumn.setId(1);
        fromColumn.setName("From Column");

        Column toColumn = new Column();
        toColumn.setId(2);
        toColumn.setName("To Column");

        List<Story> stories = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Story story = new Story();
            story.setId(i);
            story.setColumnId(fromColumnId);
            stories.add(story);
        }

        when(columnRepo.find(fromColumnId)).thenReturn(fromColumn);
        when(columnRepo.find(toColumnId)).thenReturn(toColumn);
        when(storyRepo.findByColumn(fromColumnId)).thenReturn(stories);

        String result = controller.moveAllStories(projectId, fromColumnId, toColumnId);

        assertEquals("success", result);
        verify(storyRepo, times(3)).persist(any(Story.class));
        for (Story story : stories) {
            assertEquals(toColumnId, story.getColumnId());
        }
    }

    @Test
    void testMoveAllStoriesWithNonExistentColumn() {
        Long projectId = 1L;
        Long fromColumnId = 1L;
        Long toColumnId = 999L;

        Column fromColumn = new Column();
        fromColumn.setId(1);

        when(columnRepo.find(fromColumnId)).thenReturn(fromColumn);
        when(columnRepo.find(toColumnId)).thenReturn(null);

        String result = controller.moveAllStories(projectId, fromColumnId, toColumnId);

        assertEquals("error", result);
        verify(storyRepo, never()).persist(any(Story.class));
    }

    @Test
    void testReorderColumns() {
        Long projectId = 1L;
        String columnOrder = "3,1,2";

        Column backlog = new Column();
        backlog.setId(10);
        backlog.setName("BACKLOG");

        Column col1 = new Column();
        col1.setId(1);
        col1.setName("Column 1");

        Column col2 = new Column();
        col2.setId(2);
        col2.setName("Column 2");

        Column col3 = new Column();
        col3.setId(3);
        col3.setName("Column 3");

        List<Column> allColumns = new ArrayList<>();
        allColumns.add(backlog);
        allColumns.add(col1);
        allColumns.add(col2);
        allColumns.add(col3);

        when(columnRepo.findByProject(projectId)).thenReturn(allColumns);

        String result = controller.reorderColumns(projectId, columnOrder);

        assertTrue(result.contains("success"));
        verify(columnRepo).reorder(10L, 1); // BACKLOG first
        verify(columnRepo).reorder(3L, 2);  // Then col3
        verify(columnRepo).reorder(1L, 3);  // Then col1
        verify(columnRepo).reorder(2L, 4);  // Then col2
    }

    @Test
    void testUpdateColumnNameGet() {
        Long projectId = 1L;
        Long columnId = 1L;
        String newName = "New Name";

        Column column = new Column();
        column.setId(1);
        column.setName("Old Name");

        when(columnRepo.find(columnId)).thenReturn(column);

        String result = controller.updateColumnNameGet(projectId, columnId, newName);

        assertTrue(result.contains("redirect:/board/"));
        assertEquals(newName, column.getName());
        verify(columnRepo).persist(column);
    }

    @Test
    void testUpdateColumnNameGetWithNonExistentColumn() {
        Long projectId = 1L;
        Long columnId = 999L;
        String newName = "New Name";

        when(columnRepo.find(columnId)).thenReturn(null);

        String result = controller.updateColumnNameGet(projectId, columnId, newName);

        assertTrue(result.contains("error"));
    }

    @Test
    void testMoveStoryUpdatesPosition() {
        Long projectId = 1L;
        Long storyId = 1L;
        Long toColumnId = 2L;

        Column targetColumn = new Column();
        targetColumn.setId(2);
        targetColumn.setName("IN PROGRESS");
        targetColumn.setMaxCapacity(0);

        Story story = new Story();
        story.setId(1);
        story.setTitle("Test Story");
        story.setPosition(5);

        when(columnRepo.find(toColumnId)).thenReturn(targetColumn);
        when(storyRepo.find(storyId)).thenReturn(story);
        when(storyRepo.findByColumn(toColumnId)).thenReturn(new ArrayList<>());

        String result = controller.moveStory(projectId, storyId, toColumnId, null, null, null, null);

        assertTrue(result.contains("success"));
        assertEquals(0, story.getPosition());
        verify(storyRepo).persist(story);
    }

    @Test
    void testMoveStoryToDefaultColumn() {
        Long projectId = 1L;
        Long storyId = 1L;
        Long toColumnId = 2L;

        Column targetColumn = new Column();
        targetColumn.setId(2);
        targetColumn.setName("DONE");
        targetColumn.setMaxCapacity(0);

        Story story = new Story();
        story.setId(1);
        story.setTitle("Test Story");
        story.setStatus(StoryStatus.BACKLOG);

        when(columnRepo.find(toColumnId)).thenReturn(targetColumn);
        when(storyRepo.find(storyId)).thenReturn(story);
        when(storyRepo.findByColumn(toColumnId)).thenReturn(new ArrayList<>());

        String result = controller.moveStory(projectId, storyId, toColumnId, null, null, null, null);

        assertTrue(result.contains("success"));
        assertEquals(StoryStatus.DONE, story.getStatus());
        verify(storyRepo).persist(story);
    }

    @Test
    void testDeleteColumnDoneNotAllowed() {
        Long projectId = 1L;
        Long columnId = 1L;

        Column column = new Column();
        column.setId(1);
        column.setName("DONE");

        when(columnRepo.find(columnId)).thenReturn(column);

        String result = controller.deleteColumn(projectId, columnId);

        assertTrue(result.contains("error"));
        assertTrue(result.contains("Cannot delete"));
        verify(columnRepo, never()).remove(anyLong());
    }

    @Test
    void testReorderColumnsWithoutBacklog() {
        Long projectId = 1L;
        String columnOrder = "1,2,3";

        Column col1 = new Column();
        col1.setId(1);
        col1.setName("Column 1");

        Column col2 = new Column();
        col2.setId(2);
        col2.setName("Column 2");

        Column col3 = new Column();
        col3.setId(3);
        col3.setName("Column 3");

        List<Column> allColumns = new ArrayList<>();
        allColumns.add(col1);
        allColumns.add(col2);
        allColumns.add(col3);

        when(columnRepo.findByProject(projectId)).thenReturn(allColumns);

        String result = controller.reorderColumns(projectId, columnOrder);

        assertTrue(result.contains("success"));
        verify(columnRepo, atLeast(3)).reorder(anyLong(), anyInt());
    }

    @Test
    void testAddColumnWithSubColumns() {
        Long projectId = 1L;
        String columnName = "Testing";
        int maxCapacity = 5;
        boolean hasSubColumns = true;

        Project project = new Project();
        project.setId(1);

        when(projectRepo.find(projectId)).thenReturn(project);
        when(columnRepo.findByProject(projectId)).thenReturn(List.of());

        String result = controller.addColumn(projectId, columnName, maxCapacity, hasSubColumns);

        assertEquals("redirect:/board/" + projectId, result);

        ArgumentCaptor<Column> captor = ArgumentCaptor.forClass(Column.class);
        verify(columnRepo).persist(captor.capture());

        Column captured = captor.getValue();
        assertEquals(columnName, captured.getName());
        assertEquals(maxCapacity, captured.getMaxCapacity());
        assertTrue(captured.isHasSubColumns());
    }

    @Test
    void testShowBoardWithSubColumnsInProgress() {
        Long projectId = 1L;

        Project project = new Project();
        project.setId(1);

        Column inProgressCol = new Column();
        inProgressCol.setId(2);
        inProgressCol.setName("IN PROGRESS");
        inProgressCol.setHasSubColumns(true);

        when(projectRepo.find(projectId)).thenReturn(project);
        when(columnRepo.findByProject(projectId)).thenReturn(List.of(inProgressCol));
        when(storyRepo.findByColumn(2L)).thenReturn(List.of());
        when(swimlaneRepo.findAll()).thenReturn(new ArrayList<>());

        ModelAndView mav = controller.showBoard(projectId);

        assertEquals("board", mav.getViewName());
        Collection<Column> columns = (Collection<Column>) mav.getModel().get("columns");
        assertNotNull(columns);
        assertTrue(columns.stream().anyMatch(c -> c.isHasSubColumns()));
    }

    @Test
    void testMoveStoryToBlockedColumn() {
        Long projectId = 1L;
        Long storyId = 10L;
        Long blockedColumnId = 5L;

        Column blockedColumn = new Column();
        blockedColumn.setId(5);
        blockedColumn.setName("BLOCKED");
        blockedColumn.setMaxCapacity(0);

        Story story = new Story();
        story.setId(10);
        story.setStatus(StoryStatus.IN_PROGRESS);

        when(columnRepo.find(blockedColumnId)).thenReturn(blockedColumn);
        when(storyRepo.find(storyId)).thenReturn(story);
        when(storyRepo.findByColumn(blockedColumnId)).thenReturn(new ArrayList<>());

        String result = controller.moveStory(projectId, storyId, blockedColumnId, null, null, null, null);

        assertTrue(result.contains("success"));
        assertEquals(StoryStatus.BLOCKED, story.getStatus());
        verify(storyRepo).persist(story);
    }

    @Test
    void testReorderStoriesWithEmptyOrder() {
        Long projectId = 1L;
        Long columnId = 1L;
        String storyOrder = "";

        String result = controller.reorderStories(projectId, columnId, storyOrder);

        assertTrue(result.contains("success"));
        verify(storyRepo, never()).persist(any(Story.class));
    }

    @Test
    void testMoveAllStoriesToDifferentColumn() {
        Long projectId = 1L;
        Long fromColumnId = 1L;
        Long toColumnId = 2L;

        Story story1 = new Story();
        story1.setId(1);
        story1.setColumnId(fromColumnId);

        Story story2 = new Story();
        story2.setId(2);
        story2.setColumnId(fromColumnId);

        Column fromColumn = new Column();
        fromColumn.setId(1);

        Column toColumn = new Column();
        toColumn.setId(2);

        when(columnRepo.find(fromColumnId)).thenReturn(fromColumn);
        when(columnRepo.find(toColumnId)).thenReturn(toColumn);
        when(storyRepo.findByColumn(fromColumnId)).thenReturn(Arrays.asList(story1, story2));

        String result = controller.moveAllStories(projectId, fromColumnId, toColumnId);

        assertEquals("success", result);
        verify(storyRepo, times(2)).persist(any(Story.class));
        assertEquals(toColumnId, story1.getColumnId());
        assertEquals(toColumnId, story2.getColumnId());
    }

    @Test
    void testMoveStoryToCustomColumn() {
        Long projectId = 1L;
        Long storyId = 1L;
        Long toColumnId = 2L;

        Column targetColumn = new Column();
        targetColumn.setId(2);
        targetColumn.setName("Custom Column");
        targetColumn.setMaxCapacity(0);

        Story story = new Story();
        story.setId(1);
        story.setTitle("Test Story");
        story.setStatus(StoryStatus.BACKLOG);

        when(columnRepo.find(toColumnId)).thenReturn(targetColumn);
        when(storyRepo.find(storyId)).thenReturn(story);
        when(storyRepo.findByColumn(toColumnId)).thenReturn(new ArrayList<>());

        String result = controller.moveStory(projectId, storyId, toColumnId, null, null, null, null);

        assertTrue(result.contains("success"));
        //status should remain unchanged for custom columns
        assertEquals(StoryStatus.BACKLOG, story.getStatus());
        verify(storyRepo).persist(story);
    }

    @Test
    void testMoveStoryToReviewColumn() {
        Long projectId = 1L;
        Long storyId = 1L;
        Long toColumnId = 2L;

        Column targetColumn = new Column();
        targetColumn.setId(2);
        targetColumn.setName("REVIEW");
        targetColumn.setMaxCapacity(0);

        Story story = new Story();
        story.setId(1);
        story.setTitle("Test Story");
        story.setStatus(StoryStatus.BACKLOG);

        when(columnRepo.find(toColumnId)).thenReturn(targetColumn);
        when(storyRepo.find(storyId)).thenReturn(story);
        when(storyRepo.findByColumn(toColumnId)).thenReturn(new ArrayList<>());

        String result = controller.moveStory(projectId, storyId, toColumnId, null, null, null, null);

        assertTrue(result.contains("success"));
        assertEquals(StoryStatus.REVIEW, story.getStatus());
        verify(storyRepo).persist(story);
    }

    @Test
    void testMoveStoryWithException() {
        Long projectId = 1L;
        Long storyId = 1L;
        Long toColumnId = 2L;

        Column targetColumn = new Column();
        targetColumn.setId(2);
        targetColumn.setName("IN PROGRESS");
        targetColumn.setMaxCapacity(0);

        when(columnRepo.find(toColumnId)).thenReturn(targetColumn);
        when(storyRepo.findByColumn(toColumnId)).thenReturn(new ArrayList<>());
        doThrow(new IllegalStateException("Test error"))
                .when(columnRepo).moveStoryBetweenColumns(storyId, null, toColumnId);

        String result = controller.moveStory(projectId, storyId, toColumnId, null, null, null, null);

        assertTrue(result.contains("success\":false"));
        assertTrue(result.contains("Test error"));
    }


    @Test
    void testUpdateColumnNameGetWithEmptyName() {
        Long projectId = 1L;
        Long columnId = 1L;
        String newName = "";

        Column column = new Column();
        column.setId(1);
        column.setName("Old Name");

        when(columnRepo.find(columnId)).thenReturn(column);

        String result = controller.updateColumnNameGet(projectId, columnId, newName);

        assertTrue(result.contains("error"));
        assertTrue(result.contains("cannot be empty"));
    }

    @Test
    void testUpdateColumnNameWithEmptyName() {
        Long projectId = 1L;
        Long columnId = 1L;
        String newName = "   ";

        Column column = new Column();
        column.setId(1);
        column.setName("Old Name");

        when(columnRepo.find(columnId)).thenReturn(column);

        String result = controller.updateColumnName(projectId, columnId, newName);

        assertTrue(result.contains("success\":false"));
        assertTrue(result.contains("cannot be empty"));
    }

    @Test
    void testUpdateColumnNameWithNullName() {
        Long projectId = 1L;
        Long columnId = 1L;
        String newName = null;

        Column column = new Column();
        column.setId(1);
        column.setName("Old Name");

        when(columnRepo.find(columnId)).thenReturn(column);

        String result = controller.updateColumnName(projectId, columnId, newName);

        assertTrue(result.contains("success\":false"));
        assertTrue(result.contains("cannot be empty"));
    }

    @Test
    void testUpdateColumnWithNullName() {
        Long projectId = 1L;
        Long columnId = 1L;
        String newName = null;
        int maxCapacity = 10;

        Column column = new Column();
        column.setId(1);
        column.setName("Old Name");
        column.setMaxCapacity(5);

        when(columnRepo.find(columnId)).thenReturn(column);

        String result = controller.updateColumn(projectId, columnId, newName, maxCapacity);

        assertTrue(result.contains("success"));
        assertEquals("Old Name", column.getName()); // Name unchanged
        assertEquals(maxCapacity, column.getMaxCapacity()); // Capacity updated
        verify(columnRepo).persist(column);
    }

    @Test
    void testUpdateColumnNameGetWithException() {
        Long projectId = 1L;
        Long columnId = 1L;
        String newName = "New Name";

        when(columnRepo.find(columnId)).thenThrow(new RuntimeException("Database error"));

        String result = controller.updateColumnNameGet(projectId, columnId, newName);

        assertTrue(result.contains("error"));
    }

    @Test
    void testAddColumnBeforeDoneColumn() {
        Long projectId = 1L;
        String name = "New Column";

        Project project = new Project();
        project.setId(1);

        Column doneColumn = new Column();
        doneColumn.setId(10);
        doneColumn.setName("DONE");
        doneColumn.setPosition(5);

        Column col1 = new Column();
        col1.setId(1);
        col1.setPosition(1);

        when(projectRepo.find(projectId)).thenReturn(project);
        when(columnRepo.findByProject(projectId)).thenReturn(Arrays.asList(col1, doneColumn));

        String result = controller.addColumn(projectId, name, 0, false);

        assertEquals("redirect:/board/" + projectId, result);
        verify(columnRepo, atLeast(2)).persist(any(Column.class));
    }

    @Test
    void testDeleteColumnWithStoriesAjaxWithNonExistentColumn() {
        Long projectId = 1L;
        Long columnId = 999L;

        when(columnRepo.find(columnId)).thenReturn(null);

        String result = controller.deleteColumnWithStories(projectId, columnId);

        assertEquals("error", result);
        verify(columnRepo, never()).remove(anyLong());
    }

    @Test
    void testReorderStoriesWithInvalidId() {
        Long projectId = 1L;
        Long columnId = 1L;
        String storyOrder = "1,invalid,3";

        Story story1 = new Story();
        story1.setId(1);

        when(storyRepo.find(1L)).thenReturn(story1);

        String result = controller.reorderStories(projectId, columnId, storyOrder);

        assertTrue(result.contains("success\":false"));
    }

    @Test
    @DisplayName("moveStory should handle null targetColumn")
    void testMoveStoryWithNullTargetColumn() {
        Long projectId = 1L;
        Long storyId = 1L;
        Long toColumnId = 999L;

        Story story = new Story();
        story.setId((int) storyId.intValue());
        story.setStatus(fr.uha.ensisa.gl.entities.StoryStatus.BACKLOG);

        when(columnRepo.find(toColumnId)).thenReturn(null);
        when(storyRepo.find(storyId)).thenReturn(story);

        String result = controller.moveStory(projectId, storyId, toColumnId, null, null, null, null);

        assertTrue(result.contains("success"));
        verify(columnRepo).moveStoryBetweenColumns(storyId, null, toColumnId);
    }

    @Test
    @DisplayName("moveStory should handle null story")
    void testMoveStoryWithNullStory() {
        Long projectId = 1L;
        Long storyId = 999L;
        Long toColumnId = 2L;

        Column targetColumn = new Column();
        targetColumn.setId(toColumnId.intValue());
        targetColumn.setName("IN PROGRESS");

        when(columnRepo.find(toColumnId)).thenReturn(targetColumn);
        when(storyRepo.find(storyId)).thenReturn(null);

        String result = controller.moveStory(projectId, storyId, toColumnId, null, null, null, null);

        assertTrue(result.contains("success"));
        verify(columnRepo).moveStoryBetweenColumns(storyId, null, toColumnId);
    }

    @Test
    @DisplayName("moveStory should handle column with maxCapacity = 0")
    void testMoveStoryWithZeroMaxCapacity() {
        Long projectId = 1L;
        Long storyId = 1L;
        Long toColumnId = 2L;

        Column targetColumn = new Column();
        targetColumn.setId(toColumnId.intValue());
        targetColumn.setMaxCapacity(0); // No limit

        Story story = new Story();
        story.setId((int) storyId.intValue());

        when(columnRepo.find(toColumnId)).thenReturn(targetColumn);
        when(storyRepo.find(storyId)).thenReturn(story);
        when(storyRepo.findByColumn(toColumnId)).thenReturn(List.of());

        String result = controller.moveStory(projectId, storyId, toColumnId, null, null, null, null);

        assertTrue(result.contains("success"));
    }

    @Test
    @DisplayName("moveStory should handle full column")
    void testMoveStoryWithFullColumn() {
        Long projectId = 1L;
        Long storyId = 1L;
        Long toColumnId = 2L;

        Column targetColumn = new Column();
        targetColumn.setId(toColumnId.intValue());
        targetColumn.setMaxCapacity(2);

        Story existingStory1 = new Story();
        existingStory1.setId(10);
        Story existingStory2 = new Story();
        existingStory2.setId(20);

        when(columnRepo.find(toColumnId)).thenReturn(targetColumn);
        when(storyRepo.findByColumn(toColumnId)).thenReturn(List.of(existingStory1, existingStory2));

        String result = controller.moveStory(projectId, storyId, toColumnId, null, null, null, null);

        assertTrue(result.contains("error"));
        assertTrue(result.contains("full"));
        verify(columnRepo, never()).moveStoryBetweenColumns(anyLong(), any(), anyLong());
    }

    @Test
    @DisplayName("updateSubColumn should handle exception")
    void testUpdateSubColumnWithException() {
        Long projectId = 1L;
        Long storyId = 1L;
        String subColumn = "DOING";

        when(storyRepo.find(storyId)).thenThrow(new RuntimeException("Database error"));

        String result = controller.updateSubColumn(projectId, storyId, subColumn);

        assertTrue(result.contains("error"));
    }

    @Test
    @DisplayName("showBoard should redirect when project is null")
    void testShowBoardWithNullProject() {
        Long projectId = 999L;

        when(projectRepo.find(projectId)).thenReturn(null);

        ModelAndView mav = controller.showBoard(projectId);

        assertEquals("redirect:/project/list", mav.getViewName());
    }

    @Test
    @DisplayName("showBoard should set hasSubColumns for default columns")
    void testShowBoardSetsHasSubColumns() {
        Long projectId = 1L;

        Project project = new Project();
        project.setId(projectId.intValue());

        Column inProgressColumn = new Column();
        inProgressColumn.setId(2);
        inProgressColumn.setName("IN PROGRESS");
        inProgressColumn.setHasSubColumns(false);

        when(projectRepo.find(projectId)).thenReturn(project);
        when(columnRepo.findByProject(projectId)).thenReturn(List.of(inProgressColumn));
        when(storyRepo.findByColumn(2L)).thenReturn(List.of());
        when(swimlaneRepo.findAll()).thenReturn(List.of());

        ModelAndView mav = controller.showBoard(projectId);

        assertNotNull(mav);
        assertEquals("board", mav.getViewName());
        verify(columnRepo).persist(inProgressColumn);
        assertTrue(inProgressColumn.isHasSubColumns());
    }

    @Test
    @DisplayName("moveStory should handle custom column name")
    void testMoveStoryWithCustomColumn() {
        Long projectId = 1L;
        Long storyId = 1L;
        Long toColumnId = 5L;

        Column customColumn = new Column();
        customColumn.setId(toColumnId.intValue());
        customColumn.setName("Custom Column");

        Story story = new Story();
        story.setId((int) storyId.intValue());
        story.setStatus(fr.uha.ensisa.gl.entities.StoryStatus.BACKLOG);

        when(columnRepo.find(toColumnId)).thenReturn(customColumn);
        when(storyRepo.find(storyId)).thenReturn(story);
        when(storyRepo.findByColumn(toColumnId)).thenReturn(List.of());

        String result = controller.moveStory(projectId, storyId, toColumnId, null, null, null, null);

        assertTrue(result.contains("success"));
        verify(storyRepo).persist(story);
    }

    @Test
    @DisplayName("moveStory should handle swimlaneId parameter")
    void testMoveStoryWithSwimlaneId() {
        Long projectId = 1L;
        Long storyId = 1L;
        Long toColumnId = 2L;
        Long swimlaneId = 10L;

        Column targetColumn = new Column();
        targetColumn.setId(toColumnId.intValue());
        targetColumn.setName("IN PROGRESS");

        Story story = new Story();
        story.setId((int) storyId.intValue());

        when(columnRepo.find(toColumnId)).thenReturn(targetColumn);
        when(storyRepo.find(storyId)).thenReturn(story);
        when(storyRepo.findByColumn(toColumnId)).thenReturn(List.of());

        String result = controller.moveStory(projectId, storyId, toColumnId, null, null, null, swimlaneId);

        assertTrue(result.contains("success"));
        assertEquals(swimlaneId, story.getSwimlaneId());
        verify(storyRepo).persist(story);
    }

    @Test
    @DisplayName("moveStory should handle IllegalStateException")
    void testMoveStoryWithIllegalStateException() {
        Long projectId = 1L;
        Long storyId = 1L;
        Long toColumnId = 2L;

        Column targetColumn = new Column();
        targetColumn.setId(toColumnId.intValue());

        when(columnRepo.find(toColumnId)).thenReturn(targetColumn);
        doThrow(new IllegalStateException("Cannot move story"))
            .when(columnRepo).moveStoryBetweenColumns(storyId, null, toColumnId);

        String result = controller.moveStory(projectId, storyId, toColumnId, null, null, null, null);

        assertTrue(result.contains("error"));
        assertTrue(result.contains("Cannot move story"));
    }

    @Test
    @DisplayName("moveStory should map BACKLOG column to BACKLOG status")
    void testMoveStoryMapsBacklogColumn() {
        Long projectId = 1L;
        Long storyId = 1L;
        Long toColumnId = 2L;

        Column backlogColumn = new Column();
        backlogColumn.setId(toColumnId.intValue());
        backlogColumn.setName("BACKLOG");

        Story story = new Story();
        story.setId((int) storyId.intValue());
        story.setStatus(fr.uha.ensisa.gl.entities.StoryStatus.IN_PROGRESS);

        when(columnRepo.find(toColumnId)).thenReturn(backlogColumn);
        when(storyRepo.find(storyId)).thenReturn(story);
        when(storyRepo.findByColumn(toColumnId)).thenReturn(List.of());

        String result = controller.moveStory(projectId, storyId, toColumnId, null, null, null, null);

        assertTrue(result.contains("success"));
        assertEquals(fr.uha.ensisa.gl.entities.StoryStatus.BACKLOG, story.getStatus());
        verify(storyRepo).persist(story);
    }

    @Test
    @DisplayName("moveStory should map REVIEW column to REVIEW status")
    void testMoveStoryMapsReviewColumn() {
        Long projectId = 1L;
        Long storyId = 1L;
        Long toColumnId = 3L;

        Column reviewColumn = new Column();
        reviewColumn.setId(toColumnId.intValue());
        reviewColumn.setName("REVIEW");

        Story story = new Story();
        story.setId((int) storyId.intValue());

        when(columnRepo.find(toColumnId)).thenReturn(reviewColumn);
        when(storyRepo.find(storyId)).thenReturn(story);
        when(storyRepo.findByColumn(toColumnId)).thenReturn(List.of());

        String result = controller.moveStory(projectId, storyId, toColumnId, null, null, null, null);

        assertTrue(result.contains("success"));
        assertTrue(result.contains("REVIEW"));
        assertEquals(fr.uha.ensisa.gl.entities.StoryStatus.REVIEW, story.getStatus());
    }

    @Test
    @DisplayName("moveStory should map DONE column to DONE status")
    void testMoveStoryMapsDoneColumn() {
        Long projectId = 1L;
        Long storyId = 1L;
        Long toColumnId = 4L;

        Column doneColumn = new Column();
        doneColumn.setId(toColumnId.intValue());
        doneColumn.setName("DONE");

        Story story = new Story();
        story.setId((int) storyId.intValue());

        when(columnRepo.find(toColumnId)).thenReturn(doneColumn);
        when(storyRepo.find(storyId)).thenReturn(story);
        when(storyRepo.findByColumn(toColumnId)).thenReturn(List.of());

        String result = controller.moveStory(projectId, storyId, toColumnId, null, null, null, null);

        assertTrue(result.contains("success"));
        assertTrue(result.contains("DONE"));
        assertEquals(fr.uha.ensisa.gl.entities.StoryStatus.DONE, story.getStatus());
    }

    @Test
    @DisplayName("moveStory should map BLOCKED column to BLOCKED status")
    void testMoveStoryMapsBlockedColumn() {
        Long projectId = 1L;
        Long storyId = 1L;
        Long toColumnId = 5L;

        Column blockedColumn = new Column();
        blockedColumn.setId(toColumnId.intValue());
        blockedColumn.setName("BLOCKED");

        Story story = new Story();
        story.setId((int) storyId.intValue());

        when(columnRepo.find(toColumnId)).thenReturn(blockedColumn);
        when(storyRepo.find(storyId)).thenReturn(story);
        when(storyRepo.findByColumn(toColumnId)).thenReturn(List.of());

        String result = controller.moveStory(projectId, storyId, toColumnId, null, null, null, null);

        assertTrue(result.contains("success"));
        assertTrue(result.contains("BLOCKED"));
        assertEquals(fr.uha.ensisa.gl.entities.StoryStatus.BLOCKED, story.getStatus());
    }

    @Test
    @DisplayName("moveStory should map IN PROGRESS column with space to IN_PROGRESS status")
    void testMoveStoryMapsInProgressWithSpace() {
        Long projectId = 1L;
        Long storyId = 1L;
        Long toColumnId = 2L;

        Column inProgressColumn = new Column();
        inProgressColumn.setId(toColumnId.intValue());
        inProgressColumn.setName("IN PROGRESS");

        Story story = new Story();
        story.setId((int) storyId.intValue());

        when(columnRepo.find(toColumnId)).thenReturn(inProgressColumn);
        when(storyRepo.find(storyId)).thenReturn(story);
        when(storyRepo.findByColumn(toColumnId)).thenReturn(List.of());

        String result = controller.moveStory(projectId, storyId, toColumnId, null, null, null, null);

        assertTrue(result.contains("success"));
        assertEquals(fr.uha.ensisa.gl.entities.StoryStatus.IN_PROGRESS, story.getStatus());
    }

    @Test
    @DisplayName("moveStory should set subColumn to null for BACKLOG column")
    void testMoveStorySetsSubColumnNullForBacklog() {
        Long projectId = 1L;
        Long storyId = 1L;
        Long toColumnId = 2L;

        Column backlogColumn = new Column();
        backlogColumn.setId(toColumnId.intValue());
        backlogColumn.setName("BACKLOG");

        Story story = new Story();
        story.setId((int) storyId.intValue());
        story.setSubColumn("DOING");

        when(columnRepo.find(toColumnId)).thenReturn(backlogColumn);
        when(storyRepo.find(storyId)).thenReturn(story);
        when(storyRepo.findByColumn(toColumnId)).thenReturn(List.of());

        String result = controller.moveStory(projectId, storyId, toColumnId, null, null, null, null);

        assertTrue(result.contains("success"));
        assertNull(story.getSubColumn());
    }

    @Test
    @DisplayName("moveStory should set subColumn to null for DONE column")
    void testMoveStorySetsSubColumnNullForDone() {
        Long projectId = 1L;
        Long storyId = 1L;
        Long toColumnId = 4L;

        Column doneColumn = new Column();
        doneColumn.setId(toColumnId.intValue());
        doneColumn.setName("DONE");

        Story story = new Story();
        story.setId((int) storyId.intValue());

        when(columnRepo.find(toColumnId)).thenReturn(doneColumn);
        when(storyRepo.find(storyId)).thenReturn(story);
        when(storyRepo.findByColumn(toColumnId)).thenReturn(List.of());

        String result = controller.moveStory(projectId, storyId, toColumnId, null, null, null, null);

        assertTrue(result.contains("success"));
        assertNull(story.getSubColumn());
    }

    @Test
    @DisplayName("addColumn should truncate name if longer than 25 characters")
    void testAddColumnTruncatesLongName() {
        Long projectId = 1L;
        String longName = "This is a very long column name that exceeds 25 characters";

        Project project = new Project();
        project.setId(projectId.intValue());

        when(projectRepo.find(projectId)).thenReturn(project);
        when(columnRepo.findByProject(projectId)).thenReturn(List.of());

        String result = controller.addColumn(projectId, longName, 0, false);

        assertEquals("redirect:/board/" + projectId, result);
        ArgumentCaptor<Column> columnCaptor = ArgumentCaptor.forClass(Column.class);
        verify(columnRepo).persist(columnCaptor.capture());
        assertEquals(25, columnCaptor.getValue().getName().length());
    }

    @Test
    @DisplayName("addColumn should place column at end when no DONE column exists")
    void testAddColumnWithoutDoneColumn() {
        Long projectId = 1L;
        String name = "New Column";

        Project project = new Project();
        project.setId(projectId.intValue());

        Column col1 = new Column();
        col1.setId(1);
        col1.setPosition(1);
        col1.setName("BACKLOG");

        when(projectRepo.find(projectId)).thenReturn(project);
        when(columnRepo.findByProject(projectId)).thenReturn(List.of(col1));

        String result = controller.addColumn(projectId, name, 0, false);

        assertEquals("redirect:/board/" + projectId, result);
        ArgumentCaptor<Column> columnCaptor = ArgumentCaptor.forClass(Column.class);
        verify(columnRepo).persist(columnCaptor.capture());
        assertEquals(2, columnCaptor.getValue().getPosition());
    }

    @Test
    @DisplayName("deleteColumn should prevent deletion of BACKLOG column")
    void testDeleteColumnPreventsBacklog() {
        Long projectId = 1L;
        Long columnId = 1L;

        Column backlogColumn = new Column();
        backlogColumn.setId(columnId.intValue());
        backlogColumn.setName("BACKLOG");

        when(columnRepo.find(columnId)).thenReturn(backlogColumn);

        String result = controller.deleteColumn(projectId, columnId);

        assertTrue(result.contains("error"));
        assertTrue(result.contains("Cannot delete"));
        verify(columnRepo, never()).remove(columnId);
    }

    @Test
    @DisplayName("deleteColumn should prevent deletion of DONE column")
    void testDeleteColumnPreventsDone() {
        Long projectId = 1L;
        Long columnId = 5L;

        Column doneColumn = new Column();
        doneColumn.setId(columnId.intValue());
        doneColumn.setName("DONE");

        when(columnRepo.find(columnId)).thenReturn(doneColumn);

        String result = controller.deleteColumn(projectId, columnId);

        assertTrue(result.contains("error"));
        assertTrue(result.contains("Cannot delete"));
        verify(columnRepo, never()).remove(columnId);
    }


    @Test
    @DisplayName("showBoard should set hasSubColumns for REVIEW column")
    void testShowBoardSetsHasSubColumnsForReview() {
        Long projectId = 1L;

        Project project = new Project();
        project.setId(projectId.intValue());

        Column reviewColumn = new Column();
        reviewColumn.setId(3);
        reviewColumn.setName("REVIEW");
        reviewColumn.setHasSubColumns(false);

        when(projectRepo.find(projectId)).thenReturn(project);
        when(columnRepo.findByProject(projectId)).thenReturn(List.of(reviewColumn));
        when(storyRepo.findByColumn(3L)).thenReturn(List.of());
        when(swimlaneRepo.findAll()).thenReturn(List.of());

        ModelAndView mav = controller.showBoard(projectId);

        assertNotNull(mav);
        verify(columnRepo).persist(reviewColumn);
        assertTrue(reviewColumn.isHasSubColumns());
    }

    @Test
    @DisplayName("showBoard should NOT set hasSubColumns for BLOCKED column")
    void testShowBoardSetsHasSubColumnsForBlocked() {
        Long projectId = 1L;

        Project project = new Project();
        project.setId(projectId.intValue());

        Column blockedColumn = new Column();
        blockedColumn.setId(5);
        blockedColumn.setName("BLOCKED");
        blockedColumn.setHasSubColumns(false);

        when(projectRepo.find(projectId)).thenReturn(project);
        when(columnRepo.findByProject(projectId)).thenReturn(List.of(blockedColumn));
        when(storyRepo.findByColumn(5L)).thenReturn(List.of());
        when(swimlaneRepo.findAll()).thenReturn(List.of());

        ModelAndView mav = controller.showBoard(projectId);

        assertNotNull(mav);
        //BLOCKED should NOT have sub-columns, so persist should NOT be called
        verify(columnRepo, never()).persist(blockedColumn);
        assertFalse(blockedColumn.isHasSubColumns());
    }

    @Test
    @DisplayName("reorderColumns should handle backlog column")
    void testReorderColumnsWithBacklog() {
        Long projectId = 1L;
        String columnOrder = "2,3,1";

        Column backlog = new Column();
        backlog.setId(1);
        backlog.setName("BACKLOG");

        Column col2 = new Column();
        col2.setId(2);
        col2.setName("Column 2");

        when(columnRepo.findByProject(projectId)).thenReturn(List.of(backlog, col2));

        String result = controller.reorderColumns(projectId, columnOrder);

        assertTrue(result.contains("success"));
        verify(columnRepo).reorder(1L, 1);
    }

    @Test
    @DisplayName("addColumn should not truncate name with exactly 25 characters")
    void testAddColumn_WithExact25Characters() {
        Long projectId = 1L;
        String nameExactly25 = "A".repeat(25); // exactly 25 chars

        Project project = new Project();
        project.setId(projectId.intValue());

        when(projectRepo.find(projectId)).thenReturn(project);
        when(columnRepo.findByProject(projectId)).thenReturn(List.of());

        String result = controller.addColumn(projectId, nameExactly25, 0, false);

        assertEquals("redirect:/board/" + projectId, result);
        ArgumentCaptor<Column> columnCaptor = ArgumentCaptor.forClass(Column.class);
        verify(columnRepo).persist(columnCaptor.capture());
        assertEquals(25, columnCaptor.getValue().getName().length(), "Name should not be truncated when exactly 25 characters");
        assertEquals(nameExactly25, columnCaptor.getValue().getName());
    }

    @Test
    @DisplayName("reorderColumns should handle column with name different from BACKLOG")
    void testReorderColumns_WithNonBacklogColumn() {
        Long projectId = 1L;
        String columnOrder = "1,2";

        Column col1 = new Column();
        col1.setId(1);
        col1.setName("Column 1"); // Not BACKLOG

        Column col2 = new Column();
        col2.setId(2);
        col2.setName("Column 2");

        when(columnRepo.findByProject(projectId)).thenReturn(List.of(col1, col2));

        String result = controller.reorderColumns(projectId, columnOrder);

        assertTrue(result.contains("success"));
        // Since backlog doesn't exist, columns should be reordered starting from position 1
        verify(columnRepo).reorder(1L, 1);
        verify(columnRepo).reorder(2L, 2);
    }

    @Test
    @DisplayName("moveStory should handle null column name in mapColumnNameToStatus")
    void testMoveStory_WithNullColumnName() {
        Long projectId = 1L;
        Long storyId = 1L;
        Long toColumnId = 2L;

        Column targetColumn = new Column();
        targetColumn.setId(2);
        targetColumn.setName(null); // Null name to test mapColumnNameToStatus with null

        Story story = new Story();
        story.setId(1);
        story.setProjectId(projectId);

        when(columnRepo.find(toColumnId)).thenReturn(targetColumn);
        when(storyRepo.find(storyId)).thenReturn(story);
        when(storyRepo.findByColumn(toColumnId)).thenReturn(new ArrayList<>());

        String result = controller.moveStory(projectId, storyId, toColumnId, null, null, null, null);

        assertTrue(result.contains("success"));
        verify(storyRepo).persist(story);
    }

    @Test
    @DisplayName("reorderStories should correctly handle boundary condition with single story")
    void testReorderStories_WithSingleStory() {
        Long projectId = 1L;
        Long columnId = 1L;
        String storyOrder = "1"; // Single story to test boundary condition i < storyIds.length

        Story story1 = new Story();
        story1.setId(1);

        when(storyRepo.find(1L)).thenReturn(story1);

        String result = controller.reorderStories(projectId, columnId, storyOrder);

        assertTrue(result.contains("success"));
        assertEquals(0, story1.getPosition(), "Single story should have position 0");
        verify(storyRepo, times(1)).persist(story1);
    }

    @Test
    @DisplayName("addColumn should call setProject when project exists")
    void testAddColumn_VerifiesSetProjectCalled() {
        Long projectId = 1L;
        String name = "New Column";

        Project project = new Project();
        project.setId(1);

        when(projectRepo.find(projectId)).thenReturn(project);
        when(columnRepo.findByProject(projectId)).thenReturn(new ArrayList<>());

        ArgumentCaptor<Column> columnCaptor = ArgumentCaptor.forClass(Column.class);

        String result = controller.addColumn(projectId, name, 0, false);

        assertEquals("redirect:/board/" + projectId, result);
        verify(columnRepo).persist(columnCaptor.capture());
        assertEquals(project, columnCaptor.getValue().getProject(), "setProject should be called with the project");
    }

    @Test
    @DisplayName("addColumn should set position to donePosition when doneColumn exists")
    void testAddColumn_SetsPositionToDonePosition() {
        Long projectId = 1L;
        String name = "New Column";
        int donePosition = 5;

        Project project = new Project();
        project.setId(1);

        Column doneColumn = new Column();
        doneColumn.setId(10);
        doneColumn.setName("DONE");
        doneColumn.setPosition(donePosition);

        when(projectRepo.find(projectId)).thenReturn(project);
        when(columnRepo.findByProject(projectId)).thenReturn(Arrays.asList(doneColumn));

        ArgumentCaptor<Column> columnCaptor = ArgumentCaptor.forClass(Column.class);

        String result = controller.addColumn(projectId, name, 0, false);

        assertEquals("redirect:/board/" + projectId, result);
        verify(columnRepo, times(2)).persist(columnCaptor.capture());
        // First call is for doneColumn (position shifted), second call is for new column
        Column newColumn = columnCaptor.getAllValues().get(1);
        assertEquals(donePosition, newColumn.getPosition(), "New column should have donePosition");
    }

    @Test
    @DisplayName("addColumn should increment position for columns at or after donePosition")
    void testAddColumn_IncrementsPositionForColumnsAtOrAfterDonePosition() {
        Long projectId = 1L;
        String name = "New Column";
        int donePosition = 5;

        Project project = new Project();
        project.setId(1);

        Column doneColumn = new Column();
        doneColumn.setId(10);
        doneColumn.setName("DONE");
        doneColumn.setPosition(donePosition);

        Column colAtDonePosition = new Column();
        colAtDonePosition.setId(20);
        colAtDonePosition.setPosition(donePosition); // Exactly at donePosition

        Column colAfterDonePosition = new Column();
        colAfterDonePosition.setId(30);
        colAfterDonePosition.setPosition(donePosition + 1);

        when(projectRepo.find(projectId)).thenReturn(project);
        when(columnRepo.findByProject(projectId)).thenReturn(Arrays.asList(doneColumn, colAtDonePosition, colAfterDonePosition));

        String result = controller.addColumn(projectId, name, 0, false);

        assertEquals("redirect:/board/" + projectId, result);
        // Verify that columns at or after donePosition have their position incremented
        assertEquals(donePosition + 1, colAtDonePosition.getPosition(), "Column at donePosition should be incremented");
        assertEquals(donePosition + 2, colAfterDonePosition.getPosition(), "Column after donePosition should be incremented");
        verify(columnRepo, atLeast(2)).persist(any(Column.class));
    }

    @Test
    @DisplayName("addColumn should handle name with exactly 26 characters (boundary for > 25)")
    void testAddColumn_WithExactly26Characters() {
        Long projectId = 1L;
        String name26Chars = "A".repeat(26); // exactly 26 chars, should be truncated

        Project project = new Project();
        project.setId(1);

        when(projectRepo.find(projectId)).thenReturn(project);
        when(columnRepo.findByProject(projectId)).thenReturn(new ArrayList<>());

        ArgumentCaptor<Column> columnCaptor = ArgumentCaptor.forClass(Column.class);

        String result = controller.addColumn(projectId, name26Chars, 0, false);

        assertEquals("redirect:/board/" + projectId, result);
        verify(columnRepo).persist(columnCaptor.capture());
        assertEquals(25, columnCaptor.getValue().getName().length(), "Name should be truncated to 25 characters");
    }

    @Test
    @DisplayName("reorderStories should set position for each story")
    void testReorderStories_VerifiesSetPositionCalled() {
        Long projectId = 1L;
        Long columnId = 1L;
        String storyOrder = "1,2,3";

        Story story1 = new Story();
        story1.setId(1);
        Story story2 = new Story();
        story2.setId(2);
        Story story3 = new Story();
        story3.setId(3);

        when(storyRepo.find(1L)).thenReturn(story1);
        when(storyRepo.find(2L)).thenReturn(story2);
        when(storyRepo.find(3L)).thenReturn(story3);

        String result = controller.reorderStories(projectId, columnId, storyOrder);

        assertTrue(result.contains("success"));
        assertEquals(0, story1.getPosition());
        assertEquals(1, story2.getPosition());
        assertEquals(2, story3.getPosition());
        verify(storyRepo, times(3)).persist(any(Story.class));
    }

    @Test
    @DisplayName("reorderColumns should handle null backlogId")
    void testReorderColumns_WithNullBacklogId() {
        Long projectId = 1L;
        String columnOrder = "1,2";

        // No BACKLOG column exists, so backlogId will be null
        Column col1 = new Column();
        col1.setId(1);
        col1.setName("Column 1");

        Column col2 = new Column();
        col2.setId(2);
        col2.setName("Column 2");

        when(columnRepo.findByProject(projectId)).thenReturn(Arrays.asList(col1, col2));

        String result = controller.reorderColumns(projectId, columnOrder);

        assertTrue(result.contains("success"));
        // Should still reorder columns even if backlog doesn't exist
        verify(columnRepo, atLeastOnce()).reorder(anyLong(), anyInt());
    }

    @Test
    @DisplayName("addColumn should handle boundary condition name.length() == 26")
    void testAddColumn_WithBoundaryCondition26Chars() {
        Long projectId = 1L;
        String name26Chars = "A".repeat(26); // exactly 26 chars, should trigger truncation

        Project project = new Project();
        project.setId(1);

        when(projectRepo.find(projectId)).thenReturn(project);
        when(columnRepo.findByProject(projectId)).thenReturn(new ArrayList<>());

        ArgumentCaptor<Column> columnCaptor = ArgumentCaptor.forClass(Column.class);

        String result = controller.addColumn(projectId, name26Chars, 0, false);

        assertEquals("redirect:/board/" + projectId, result);
        verify(columnRepo).persist(columnCaptor.capture());
        assertEquals(25, columnCaptor.getValue().getName().length(), "Name should be truncated from 26 to 25");
    }

    @Test
    @DisplayName("updateColumn should call setMaxCapacity")
    void testUpdateColumn_VerifiesSetMaxCapacityCalled() {
        Long projectId = 1L;
        Long columnId = 1L;
        int maxCapacity = 10;

        Column column = new Column();
        column.setId(1);
        column.setMaxCapacity(5); // Initial capacity

        when(columnRepo.find(columnId)).thenReturn(column);

        String result = controller.updateColumn(projectId, columnId, null, maxCapacity);

        assertTrue(result.contains("success"));
        assertEquals(maxCapacity, column.getMaxCapacity(), "setMaxCapacity should be called");
        verify(columnRepo).persist(column);
    }

    @Test
    @DisplayName("reorderStories should call setPosition for each story in order")
    void testReorderStories_CallsSetPositionForEachStory() {
        Long projectId = 1L;
        Long columnId = 1L;
        String storyOrder = "10,20,30";

        Story story1 = new Story();
        story1.setId(10);
        story1.setPosition(99); // Initial position

        Story story2 = new Story();
        story2.setId(20);
        story2.setPosition(99);

        Story story3 = new Story();
        story3.setId(30);
        story3.setPosition(99);

        when(storyRepo.find(10L)).thenReturn(story1);
        when(storyRepo.find(20L)).thenReturn(story2);
        when(storyRepo.find(30L)).thenReturn(story3);

        String result = controller.reorderStories(projectId, columnId, storyOrder);

        assertTrue(result.contains("success"));
        assertEquals(0, story1.getPosition(), "First story should have position 0");
        assertEquals(1, story2.getPosition(), "Second story should have position 1");
        assertEquals(2, story3.getPosition(), "Third story should have position 2");
        verify(storyRepo, times(3)).persist(any(Story.class));
    }

    @Test
    @DisplayName("showBoard should force BLOCKED column to have hasSubColumns=false even if it was true")
    void testShowBoard_ForcesBlockedColumnToNoSubColumns() {
        Long projectId = 1L;

        Project project = new Project();
        project.setId(1);

        Column blockedColumn = new Column();
        blockedColumn.setId(5);
        blockedColumn.setName("BLOCKED");
        blockedColumn.setHasSubColumns(true); // Initially true, should be forced to false

        when(projectRepo.find(projectId)).thenReturn(project);
        when(columnRepo.findByProject(projectId)).thenReturn(List.of(blockedColumn));
        when(storyRepo.findByColumn(5L)).thenReturn(List.of());
        when(swimlaneRepo.findAll()).thenReturn(List.of());

        ModelAndView mav = controller.showBoard(projectId);

        assertNotNull(mav);
        assertEquals("board", mav.getViewName());
        assertFalse(blockedColumn.isHasSubColumns(), "BLOCKED column should have hasSubColumns=false");
        verify(columnRepo).persist(blockedColumn); // Should persist to save the change
    }

    @Test
    @DisplayName("updateColumnName should handle exception in catch block")
    void testUpdateColumnName_WithExceptionInCatch() {
        Long projectId = 1L;
        Long columnId = 1L;
        String newName = "New Name";

        when(columnRepo.find(columnId)).thenThrow(new RuntimeException("Database error"));

        String result = controller.updateColumnName(projectId, columnId, newName);

        assertTrue(result.contains("success\":false"));
        assertTrue(result.contains("error"));
    }

    @Test
    @DisplayName("deleteColumnWithStories should handle empty stories collection")
    void testDeleteColumnWithStories_WithEmptyStories() {
        Long projectId = 1L;
        Long columnId = 1L;

        Column column = new Column();
        column.setId(1);
        column.setName("Custom Column");

        when(columnRepo.find(columnId)).thenReturn(column);
        when(storyRepo.findByColumn(columnId)).thenReturn(new ArrayList<>());

        String result = controller.deleteColumnWithStories(projectId, columnId);

        assertEquals("success", result);
        verify(storyRepo, never()).remove(anyLong()); // No stories to remove
        verify(columnRepo).remove(columnId); // Column should still be removed
    }

    @Test
    @DisplayName("addColumnGet should redirect when name is null")
    void testAddColumnGet_WithNullName() {
        Long projectId = 1L;

        String result = controller.addColumnGet(projectId, null, 0, false);

        assertTrue(result.contains("redirect:/board/" + projectId));
        assertTrue(result.contains("showAddColumn=true"));
    }

    @Test
    @DisplayName("addColumnGet should redirect when name is empty")
    void testAddColumnGet_WithEmptyName() {
        Long projectId = 1L;

        String result = controller.addColumnGet(projectId, "", 0, false);

        assertTrue(result.contains("redirect:/board/" + projectId));
        assertTrue(result.contains("showAddColumn=true"));
    }

    @Test
    @DisplayName("updateColumn should handle exception in catch block")
    void testUpdateColumn_WithException() {
        Long projectId = 1L;
        Long columnId = 1L;

        when(columnRepo.find(columnId)).thenThrow(new RuntimeException("Database error"));

        String result = controller.updateColumn(projectId, columnId, "New Name", 10);

        assertTrue(result.contains("success\":false"));
        assertTrue(result.contains("error"));
    }

    @Test
    @DisplayName("updateColumn should handle null newName (should not update name)")
    void testUpdateColumn_WithNullNewName() {
        Long projectId = 1L;
        Long columnId = 1L;
        int maxCapacity = 15;

        Column column = new Column();
        column.setId(1);
        column.setName("Original Name");
        column.setMaxCapacity(5);

        when(columnRepo.find(columnId)).thenReturn(column);

        String result = controller.updateColumn(projectId, columnId, null, maxCapacity);

        assertTrue(result.contains("success"));
        assertEquals("Original Name", column.getName(), "Name should remain unchanged");
        assertEquals(maxCapacity, column.getMaxCapacity(), "MaxCapacity should be updated");
        verify(columnRepo).persist(column);
    }

    @Test
    @DisplayName("updateColumn should handle empty newName (should not update name)")
    void testUpdateColumn_WithEmptyNewName() {
        Long projectId = 1L;
        Long columnId = 1L;
        int maxCapacity = 20;

        Column column = new Column();
        column.setId(1);
        column.setName("Original Name");

        when(columnRepo.find(columnId)).thenReturn(column);

        String result = controller.updateColumn(projectId, columnId, "   ", maxCapacity);

        assertTrue(result.contains("success"));
        assertEquals("Original Name", column.getName(), "Name should remain unchanged when newName is only whitespace");
        verify(columnRepo).persist(column);
    }

    @Test
    @DisplayName("moveStory should handle column name with hyphen normalization")
    void testMoveStory_MapsColumnNameWithHyphen() {
        Long projectId = 1L;
        Long storyId = 1L;
        Long toColumnId = 2L;

        Column targetColumn = new Column();
        targetColumn.setId(2);
        targetColumn.setName("IN-PROGRESS"); // With hyphen

        Story story = new Story();
        story.setId(1);

        when(columnRepo.find(toColumnId)).thenReturn(targetColumn);
        when(storyRepo.find(storyId)).thenReturn(story);
        when(storyRepo.findByColumn(toColumnId)).thenReturn(new ArrayList<>());

        String result = controller.moveStory(projectId, storyId, toColumnId, null, null, null, null);

        assertTrue(result.contains("success"));
        assertEquals(fr.uha.ensisa.gl.entities.StoryStatus.IN_PROGRESS, story.getStatus());
        verify(storyRepo).persist(story);
    }

    @Test
    @DisplayName("moveStory should handle column name containing BACKLOG")
    void testMoveStory_MapsColumnNameContainingBacklog() {
        Long projectId = 1L;
        Long storyId = 1L;
        Long toColumnId = 2L;

        Column targetColumn = new Column();
        targetColumn.setId(2);
        targetColumn.setName("MY-BACKLOG-COLUMN"); // Contains BACKLOG

        Story story = new Story();
        story.setId(1);

        when(columnRepo.find(toColumnId)).thenReturn(targetColumn);
        when(storyRepo.find(storyId)).thenReturn(story);
        when(storyRepo.findByColumn(toColumnId)).thenReturn(new ArrayList<>());

        String result = controller.moveStory(projectId, storyId, toColumnId, null, null, null, null);

        assertTrue(result.contains("success"));
        assertEquals(fr.uha.ensisa.gl.entities.StoryStatus.BACKLOG, story.getStatus());
        verify(storyRepo).persist(story);
    }

    @Test
    @DisplayName("moveStory should handle column name containing INPROGRESS")
    void testMoveStory_MapsColumnNameContainingInProgress() {
        Long projectId = 1L;
        Long storyId = 1L;
        Long toColumnId = 2L;

        Column targetColumn = new Column();
        targetColumn.setId(2);
        targetColumn.setName("INPROGRESS"); // No space

        Story story = new Story();
        story.setId(1);

        when(columnRepo.find(toColumnId)).thenReturn(targetColumn);
        when(storyRepo.find(storyId)).thenReturn(story);
        when(storyRepo.findByColumn(toColumnId)).thenReturn(new ArrayList<>());

        String result = controller.moveStory(projectId, storyId, toColumnId, null, null, null, null);

        assertTrue(result.contains("success"));
        assertEquals(fr.uha.ensisa.gl.entities.StoryStatus.IN_PROGRESS, story.getStatus());
        verify(storyRepo).persist(story);
    }

    @Test
    @DisplayName("moveStory should handle column name with whitespace that maps to null status")
    void testMoveStory_WithUnmappedColumnName() {
        Long projectId = 1L;
        Long storyId = 1L;
        Long toColumnId = 2L;
        StoryStatus originalStatus = fr.uha.ensisa.gl.entities.StoryStatus.BACKLOG;

        Column targetColumn = new Column();
        targetColumn.setId(2);
        targetColumn.setName("UNKNOWN COLUMN"); // Doesn't match any status

        Story story = new Story();
        story.setId(1);
        story.setStatus(originalStatus);

        when(columnRepo.find(toColumnId)).thenReturn(targetColumn);
        when(storyRepo.find(storyId)).thenReturn(story);
        when(storyRepo.findByColumn(toColumnId)).thenReturn(new ArrayList<>());

        String result = controller.moveStory(projectId, storyId, toColumnId, null, null, null, null);

        assertTrue(result.contains("success"));
        assertEquals(originalStatus, story.getStatus(), "Status should remain unchanged for unknown column");
        verify(storyRepo).persist(story);
    }

    @Test
    @DisplayName("moveStory should handle null targetColumn gracefully")
    void testMoveStory_WithNullTargetColumnDoesNotCrash() {
        Long projectId = 1L;
        Long storyId = 1L;
        Long toColumnId = 2L;

        Story story = new Story();
        story.setId(1);

        when(columnRepo.find(toColumnId)).thenReturn(null);
        when(storyRepo.find(storyId)).thenReturn(story);

        String result = controller.moveStory(projectId, storyId, toColumnId, null, null, null, null);

        assertTrue(result.contains("success"));
        verify(storyRepo).persist(story);
    }

    @Test
    @DisplayName("moveStory should return newStatus in response when status is updated")
    void testMoveStory_ReturnsNewStatusInResponse() {
        Long projectId = 1L;
        Long storyId = 1L;
        Long toColumnId = 2L;

        Column targetColumn = new Column();
        targetColumn.setId(2);
        targetColumn.setName("DONE");

        Story story = new Story();
        story.setId(1);

        when(columnRepo.find(toColumnId)).thenReturn(targetColumn);
        when(storyRepo.find(storyId)).thenReturn(story);
        when(storyRepo.findByColumn(toColumnId)).thenReturn(new ArrayList<>());

        String result = controller.moveStory(projectId, storyId, toColumnId, null, null, null, null);

        assertTrue(result.contains("success"));
        assertTrue(result.contains("newStatus"));
        assertTrue(result.contains("DONE"));
        verify(storyRepo).persist(story);
    }

    @Test
    @DisplayName("moveStory should handle subColumn parameter when provided")
    void testMoveStory_WithSubColumnParameter() {
        Long projectId = 1L;
        Long storyId = 1L;
        Long toColumnId = 2L;
        String subColumn = "DOING";

        Column targetColumn = new Column();
        targetColumn.setId(2);
        targetColumn.setName("Custom Column");

        Story story = new Story();
        story.setId(1);

        when(columnRepo.find(toColumnId)).thenReturn(targetColumn);
        when(storyRepo.find(storyId)).thenReturn(story);
        when(storyRepo.findByColumn(toColumnId)).thenReturn(new ArrayList<>());

        String result = controller.moveStory(projectId, storyId, toColumnId, null, null, subColumn, null);

        assertTrue(result.contains("success"));
        assertEquals(subColumn, story.getSubColumn());
        verify(storyRepo).persist(story);
    }

    @Test
    @DisplayName("moveStory should handle empty subColumn parameter")
    void testMoveStory_WithEmptySubColumnParameter() {
        Long projectId = 1L;
        Long storyId = 1L;
        Long toColumnId = 2L;

        Column targetColumn = new Column();
        targetColumn.setId(2);
        targetColumn.setName("Custom Column");

        Story story = new Story();
        story.setId(1);

        when(columnRepo.find(toColumnId)).thenReturn(targetColumn);
        when(storyRepo.find(storyId)).thenReturn(story);
        when(storyRepo.findByColumn(toColumnId)).thenReturn(new ArrayList<>());

        String result = controller.moveStory(projectId, storyId, toColumnId, null, null, "", null);

        assertTrue(result.contains("success"));
        assertEquals("BACKLOG", story.getSubColumn(), "Should default to BACKLOG for custom column");
        verify(storyRepo).persist(story);
    }

    @Test
    @DisplayName("addColumn should handle null name gracefully")
    void testAddColumn_WithNullName() {
        Long projectId = 1L;

        Project project = new Project();
        project.setId(1);

        when(projectRepo.find(projectId)).thenReturn(project);
        when(columnRepo.findByProject(projectId)).thenReturn(new ArrayList<>());

        // Null name will cause NPE when calling name.trim() - this tests the boundary condition
        // name != null && name.length() > 25 - when name is null, first condition fails, so no crash
        // But then name.trim() will crash, which is expected behavior
        assertThrows(NullPointerException.class, () -> {
            controller.addColumn(projectId, null, 0, false);
        });
    }

    @Test
    @DisplayName("addColumn should handle name.trim() correctly")
    void testAddColumn_TrimsNameCorrectly() {
        Long projectId = 1L;
        String nameWithSpaces = "  Test Column  ";

        Project project = new Project();
        project.setId(1);

        when(projectRepo.find(projectId)).thenReturn(project);
        when(columnRepo.findByProject(projectId)).thenReturn(new ArrayList<>());

        ArgumentCaptor<Column> columnCaptor = ArgumentCaptor.forClass(Column.class);
        String result = controller.addColumn(projectId, nameWithSpaces, 0, false);

        assertEquals("redirect:/board/" + projectId, result);
        verify(columnRepo).persist(columnCaptor.capture());
        assertEquals("Test Column", columnCaptor.getValue().getName(), "Name should be trimmed");
    }

    @Test
    @DisplayName("addColumn should handle doneColumn name with different case")
    void testAddColumn_FindsDoneColumnCaseInsensitive() {
        Long projectId = 1L;
        String name = "New Column";

        Project project = new Project();
        project.setId(1);

        Column doneColumn = new Column();
        doneColumn.setId(10);
        doneColumn.setName("done"); // lowercase
        doneColumn.setPosition(5);

        Column col1 = new Column();
        col1.setId(1);
        col1.setPosition(1);

        when(projectRepo.find(projectId)).thenReturn(project);
        when(columnRepo.findByProject(projectId)).thenReturn(Arrays.asList(col1, doneColumn));

        ArgumentCaptor<Column> columnCaptor = ArgumentCaptor.forClass(Column.class);
        String result = controller.addColumn(projectId, name, 0, false);

        assertEquals("redirect:/board/" + projectId, result);
        verify(columnRepo, times(2)).persist(columnCaptor.capture());
        // First call is for doneColumn (position shifted), second call is for new column
        Column newColumn = columnCaptor.getAllValues().get(1);
        assertEquals(5, newColumn.getPosition(), "Should be placed at doneColumn position");
    }

    @Test
    @DisplayName("addColumn should handle doneColumn name with leading/trailing spaces")
    void testAddColumn_FindsDoneColumnWithSpaces() {
        Long projectId = 1L;
        String name = "New Column";

        Project project = new Project();
        project.setId(1);

        Column doneColumn = new Column();
        doneColumn.setId(10);
        doneColumn.setName("  DONE  "); // with spaces
        doneColumn.setPosition(5);

        when(projectRepo.find(projectId)).thenReturn(project);
        when(columnRepo.findByProject(projectId)).thenReturn(Arrays.asList(doneColumn));

        ArgumentCaptor<Column> columnCaptor = ArgumentCaptor.forClass(Column.class);
        String result = controller.addColumn(projectId, name, 0, false);

        assertEquals("redirect:/board/" + projectId, result);
        verify(columnRepo, times(2)).persist(columnCaptor.capture());
        // First call is for doneColumn (position shifted), second call is for new column
        Column newColumn = columnCaptor.getAllValues().get(1);
        assertEquals(5, newColumn.getPosition());
    }

    @Test
    @DisplayName("addColumn should handle col.getName() == null in loop")
    void testAddColumn_HandlesColumnWithNullNameInLoop() {
        Long projectId = 1L;
        String name = "New Column";

        Project project = new Project();
        project.setId(1);

        Column colWithNullName = new Column();
        colWithNullName.setId(1);
        colWithNullName.setName(null);
        colWithNullName.setPosition(1);

        when(projectRepo.find(projectId)).thenReturn(project);
        when(columnRepo.findByProject(projectId)).thenReturn(Arrays.asList(colWithNullName));

        ArgumentCaptor<Column> columnCaptor = ArgumentCaptor.forClass(Column.class);
        String result = controller.addColumn(projectId, name, 0, false);

        assertEquals("redirect:/board/" + projectId, result);
        verify(columnRepo).persist(columnCaptor.capture());
        assertEquals(2, columnCaptor.getValue().getPosition(), "Should be placed at end when no DONE column");
    }

    @Test
    @DisplayName("moveStory should handle column name containing REVIEW")
    void testMoveStory_MapsColumnNameContainingReview() {
        Long projectId = 1L;
        Long storyId = 1L;
        Long toColumnId = 2L;

        Column targetColumn = new Column();
        targetColumn.setId(2);
        targetColumn.setName("MY-REVIEW-COLUMN");

        Story story = new Story();
        story.setId(1);

        when(columnRepo.find(toColumnId)).thenReturn(targetColumn);
        when(storyRepo.find(storyId)).thenReturn(story);
        when(storyRepo.findByColumn(toColumnId)).thenReturn(new ArrayList<>());

        String result = controller.moveStory(projectId, storyId, toColumnId, null, null, null, null);

        assertTrue(result.contains("success"));
        assertEquals(fr.uha.ensisa.gl.entities.StoryStatus.REVIEW, story.getStatus());
        verify(storyRepo).persist(story);
    }

    @Test
    @DisplayName("moveStory should handle column name containing DONE")
    void testMoveStory_MapsColumnNameContainingDone() {
        Long projectId = 1L;
        Long storyId = 1L;
        Long toColumnId = 2L;

        Column targetColumn = new Column();
        targetColumn.setId(2);
        targetColumn.setName("ALREADY-DONE");

        Story story = new Story();
        story.setId(1);

        when(columnRepo.find(toColumnId)).thenReturn(targetColumn);
        when(storyRepo.find(storyId)).thenReturn(story);
        when(storyRepo.findByColumn(toColumnId)).thenReturn(new ArrayList<>());

        String result = controller.moveStory(projectId, storyId, toColumnId, null, null, null, null);

        assertTrue(result.contains("success"));
        assertEquals(fr.uha.ensisa.gl.entities.StoryStatus.DONE, story.getStatus());
        verify(storyRepo).persist(story);
    }

    @Test
    @DisplayName("moveStory should handle column name containing BLOCKED")
    void testMoveStory_MapsColumnNameContainingBlocked() {
        Long projectId = 1L;
        Long storyId = 1L;
        Long toColumnId = 2L;

        Column targetColumn = new Column();
        targetColumn.setId(2);
        targetColumn.setName("BLOCKED-ISSUE");

        Story story = new Story();
        story.setId(1);

        when(columnRepo.find(toColumnId)).thenReturn(targetColumn);
        when(storyRepo.find(storyId)).thenReturn(story);
        when(storyRepo.findByColumn(toColumnId)).thenReturn(new ArrayList<>());

        String result = controller.moveStory(projectId, storyId, toColumnId, null, null, null, null);

        assertTrue(result.contains("success"));
        assertEquals(fr.uha.ensisa.gl.entities.StoryStatus.BLOCKED, story.getStatus());
        verify(storyRepo).persist(story);
    }

    @Test
    @DisplayName("moveStory should handle column name with space normalization")
    void testMoveStory_MapsColumnNameWithSpaceNormalization() {
        Long projectId = 1L;
        Long storyId = 1L;
        Long toColumnId = 2L;

        Column targetColumn = new Column();
        targetColumn.setId(2);
        targetColumn.setName("in progress"); // lowercase with space

        Story story = new Story();
        story.setId(1);

        when(columnRepo.find(toColumnId)).thenReturn(targetColumn);
        when(storyRepo.find(storyId)).thenReturn(story);
        when(storyRepo.findByColumn(toColumnId)).thenReturn(new ArrayList<>());

        String result = controller.moveStory(projectId, storyId, toColumnId, null, null, null, null);

        assertTrue(result.contains("success"));
        assertEquals(fr.uha.ensisa.gl.entities.StoryStatus.IN_PROGRESS, story.getStatus());
        verify(storyRepo).persist(story);
    }

    @Test
    @DisplayName("showBoard should initialize hasSubColumns for REVIEW column when false")
    void testShowBoard_InitializesHasSubColumnsForReview() {
        Long projectId = 1L;

        Project project = new Project();
        project.setId(1);

        Column reviewColumn = new Column();
        reviewColumn.setId(3);
        reviewColumn.setName("review"); // lowercase
        reviewColumn.setHasSubColumns(false);

        when(projectRepo.find(projectId)).thenReturn(project);
        when(columnRepo.findByProject(projectId)).thenReturn(List.of(reviewColumn));
        when(storyRepo.findByColumn(3L)).thenReturn(List.of());
        when(swimlaneRepo.findAll()).thenReturn(List.of());

        ModelAndView mav = controller.showBoard(projectId);

        assertNotNull(mav);
        assertEquals("board", mav.getViewName());
        assertTrue(reviewColumn.isHasSubColumns(), "REVIEW column should have hasSubColumns=true");
        verify(columnRepo).persist(reviewColumn);
    }

    @Test
    @DisplayName("showBoard should handle column with null name in loop")
    void testShowBoard_HandlesColumnWithNullName() {
        Long projectId = 1L;

        Project project = new Project();
        project.setId(1);

        Column colWithNullName = new Column();
        colWithNullName.setId(1);
        colWithNullName.setName(null);

        when(projectRepo.find(projectId)).thenReturn(project);
        when(columnRepo.findByProject(projectId)).thenReturn(List.of(colWithNullName));
        when(storyRepo.findByColumn(1L)).thenReturn(List.of());
        when(swimlaneRepo.findAll()).thenReturn(List.of());

        ModelAndView mav = controller.showBoard(projectId);

        assertNotNull(mav);
        assertEquals("board", mav.getViewName());
        // Should not crash, column with null name should be skipped in hasSubColumns logic
    }

    @Test
    @DisplayName("showBoard should force BLOCKED to have no sub-columns and persist the change")
    void testShowBoardForcesBlockedNoSubColumnsAndPersists() {
        Long projectId = 1L;

        Project project = new Project();
        project.setId(1);
        project.setName("Test Project");

        Column blockedColumn = new Column();
        blockedColumn.setId(5);
        blockedColumn.setName("BLOCKED");
        blockedColumn.setHasSubColumns(true);

        when(projectRepo.find(projectId)).thenReturn(project);
        when(columnRepo.findByProject(projectId)).thenReturn(List.of(blockedColumn));
        when(storyRepo.findByColumn(5L)).thenReturn(List.of());
        when(swimlaneRepo.findAll()).thenReturn(List.of());

        ModelAndView mav = controller.showBoard(projectId);

        assertNotNull(mav);

        // false + persist
        assertFalse(blockedColumn.isHasSubColumns());
        verify(columnRepo).persist(blockedColumn);
    }

    @Test
    @DisplayName("addColumnGet should redirect to addColumn page when name is null")
    void testAddColumnGetNameNull() {
        Long projectId = 1L;

        String result = controller.addColumnGet(projectId, null, 3, false);

        assertTrue(result.contains("redirect:"));
        assertTrue(result.contains("showAddColumn=true"));
        verify(columnRepo, never()).persist(any(Column.class));
    }

    @Test
    @DisplayName("addColumnGet should redirect to addColumn page when name is empty or blank")
    void testAddColumnGetNameEmpty() {
        Long projectId = 1L;

        String result = controller.addColumnGet(projectId, "   ", 2, true);

        assertTrue(result.contains("redirect:"));
        assertTrue(result.contains("showAddColumn=true"));
        verify(columnRepo, never()).persist(any(Column.class));
    }

    @Test
    @DisplayName("updateColumnName should return error when column is null")
    void testUpdateColumnNameColumnNull() {
        Long projectId = 1L;
        Long columnId = 99L;

        when(columnRepo.find(columnId)).thenReturn(null);

        String result = controller.updateColumnName(projectId, columnId, "NEWNAME");

        assertNotNull(result);
        assertTrue(result.toLowerCase().contains("error")); // garde ce check simple/robuste
        verify(columnRepo, never()).persist(any(Column.class));
    }

    @Test
    @DisplayName("updateColumnName should return error when persist throws exception")
    void testUpdateColumnNameErrorOnPersist() {
        Long projectId = 1L;
        Long columnId = 10L;

        Column column = new Column();
        column.setId(10);
        column.setName("OLD");

        when(columnRepo.find(columnId)).thenReturn(column);
        doThrow(new RuntimeException("DB error")).when(columnRepo).persist(column);

        String result = controller.updateColumnName(projectId, columnId, "NEW");

        assertNotNull(result);
        assertTrue(result.toLowerCase().contains("error"));
    }

    @Test
    @DisplayName("updateColumn should return error when column is null")
    void testUpdateColumnColumnNull() {
        Long projectId = 1L;
        Long columnId = 404L;

        when(columnRepo.find(columnId)).thenReturn(null);

        String result = controller.updateColumn(projectId, columnId, "NEWNAME", 7);

        assertNotNull(result);
        assertTrue(result.toLowerCase().contains("error"));
        verify(columnRepo, never()).persist(any(Column.class));
    }

    @Test
    @DisplayName("updateColumn should return error when persist throws exception")
    void testUpdateColumnErrorOnPersist() {
        Long projectId = 1L;
        Long columnId = 12L;

        Column column = new Column();
        column.setId(12);
        column.setName("TODO");
        column.setMaxCapacity(3);

        when(columnRepo.find(columnId)).thenReturn(column);
        doThrow(new RuntimeException("DB error")).when(columnRepo).persist(column);

        String result = controller.updateColumn(projectId, columnId, "IN_PROGRESS", 5);

        assertNotNull(result);
        assertTrue(result.toLowerCase().contains("error"));
    }

    @Test
    @DisplayName("mapColumnNameToStatus should map REVIEW, DONE, BLOCKED correctly")
    void testMapColumnNameToStatusReviewDoneBlocked() throws Exception {
        Method m = BoardController.class.getDeclaredMethod("mapColumnNameToStatus", String.class);
        m.setAccessible(true);

        Object review = m.invoke(controller, "REVIEW");
        Object done = m.invoke(controller, "DONE");
        Object blocked = m.invoke(controller, "BLOCKED");

        assertEquals(StoryStatus.REVIEW, review);
        assertEquals(StoryStatus.DONE, done);
        assertEquals(StoryStatus.BLOCKED, blocked);
    }

    @Test
    @DisplayName("showBoard should fetch stories for each column (REVIEW/DONE/BLOCKED)")
    void testShowBoardFetchStoriesForEachColumn() {
        Long projectId = 1L;

        Project project = new Project();
        project.setId(1);
        project.setName("P");

        Column reviewCol = new Column(); reviewCol.setId(1); reviewCol.setName("REVIEW"); reviewCol.setHasSubColumns(false);
        Column doneCol   = new Column(); doneCol.setId(2);   doneCol.setName("DONE");   doneCol.setHasSubColumns(false);
        Column blockedCol= new Column(); blockedCol.setId(3);blockedCol.setName("BLOCKED");blockedCol.setHasSubColumns(false);

        when(projectRepo.find(projectId)).thenReturn(project);
        when(columnRepo.findByProject(projectId)).thenReturn(List.of(reviewCol, doneCol, blockedCol));
        when(storyRepo.findByColumn(1L)).thenReturn(List.of(new Story()));
        when(storyRepo.findByColumn(2L)).thenReturn(List.of(new Story(), new Story()));
        when(storyRepo.findByColumn(3L)).thenReturn(List.of());
        when(swimlaneRepo.findAll()).thenReturn(List.of());

        ModelAndView mav = controller.showBoard(projectId);

        assertNotNull(mav);

        verify(storyRepo).findByColumn(1L);
        verify(storyRepo).findByColumn(2L);
        verify(storyRepo).findByColumn(3L);
    }

    @Test
    @DisplayName("addColumnGet should persist column with maxCapacity=0 and hasSubColumns=false")
    void testAddColumnGetDefaultValuesPersisted() {
        Long projectId = 1L;

        Project project = new Project();
        project.setId(1);
        project.setName("P");

        when(projectRepo.find(projectId)).thenReturn(project);

        String result = controller.addColumnGet(projectId, "TODO", 0, false);

        assertNotNull(result);
        assertTrue(result.startsWith("redirect:"));

        ArgumentCaptor<Column> captor = ArgumentCaptor.forClass(Column.class);
        verify(columnRepo).persist(captor.capture());

        Column saved = captor.getValue();
        assertEquals("TODO", saved.getName());
        assertEquals(0, saved.getMaxCapacity());
        assertFalse(saved.isHasSubColumns());
        assertEquals(project, saved.getProject());
    }

    @Test
    @DisplayName("updateColumn should update only maxCapacity when newName is null")
    void testUpdateColumnNullNewNameUpdatesOnlyCapacity() {
        Long projectId = 1L;
        Long columnId = 7L;

        Column column = new Column();
        column.setId(7);
        column.setName("IN_PROGRESS");
        column.setMaxCapacity(2);

        when(columnRepo.find(columnId)).thenReturn(column);

        String result = controller.updateColumn(projectId, columnId, null, 10);

        assertNotNull(result);
        assertTrue(result.toLowerCase().contains("success") || result.startsWith("redirect:") || !result.toLowerCase().contains("error"));

        assertEquals("IN_PROGRESS", column.getName()); // inchangé
        assertEquals(10, column.getMaxCapacity());     // modifié
        verify(columnRepo).persist(column);
    }

}

