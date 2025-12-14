package fr.uha.ensisa.gl.tarnished.controller;

import fr.uha.ensisa.gl.entities.Column;
import fr.uha.ensisa.gl.entities.Project;
import fr.uha.ensisa.gl.entities.Story;
import fr.uha.ensisa.gl.entities.StoryStatus;
import fr.uha.ensisa.gl.entities.Swimlane;
import fr.uha.ensisa.gl.tarnished.repos.ColumnRepo;
import fr.uha.ensisa.gl.tarnished.repos.ProjectRepo;
import fr.uha.ensisa.gl.tarnished.repos.RepoFactory;
import fr.uha.ensisa.gl.tarnished.repos.StoryRepo;
import fr.uha.ensisa.gl.tarnished.repos.SwimlaneRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map; // Added import for Map

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
    private SwimlaneRepo swimlaneRepo; // Added SwimlaneRepo mock

    @InjectMocks
    private BoardController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(repoFactory.getProjectRepo()).thenReturn(projectRepo);
        when(repoFactory.getColumnRepo()).thenReturn(columnRepo);
        when(repoFactory.getStoryRepo()).thenReturn(storyRepo);
        when(repoFactory.getSwimlaneRepo()).thenReturn(swimlaneRepo);
        when(swimlaneRepo.findByProject(anyInt())).thenReturn(new ArrayList<>()); // Configure RepoFactory to return SwimlaneRepo mock
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

        ModelAndView mav = controller.showBoard(projectId);

        assertNotNull(mav);
        assertEquals("board", mav.getViewName());
        assertEquals(project, mav.getModel().get("project"));
        assertNotNull(mav.getModel().get("columns"));

        verify(projectRepo).find(projectId);
        verify(columnRepo).findByProject(projectId);
    }

    @Test
    void testShowBoardWithNonExistentProject() {
        Long projectId = 999L;
        when(projectRepo.find(projectId)).thenReturn(null);

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

        controller.showBoard(projectId);

        // verify(columnRepo).persist(inProgressColumn); // This logic was removed from showBoard
        // assertTrue(inProgressColumn.isHasSubColumns()); // This logic was removed from showBoard

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

        String result = controller.moveStory(projectId, storyId, toColumnId, fromColumnId, null, null);

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

        String result = controller.moveStory(projectId, storyId, toColumnId, null, null, null);

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

        String result = controller.moveStory(projectId, storyId, toColumnId, null, null, subColumn);

        assertTrue(result.contains("success"));
        assertEquals(subColumn, story.getSubColumn());
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

        String result = controller.moveStory(projectId, storyId, toColumnId, null, null, null);

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

        String result = controller.moveStory(projectId, storyId, toColumnId, null, null, null);

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

        String result = controller.moveStory(projectId, storyId, blockedColumnId, null, null, null);

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
    void testAddSwimlaneSuccess() {
        int projectId = 1;
        String swimlaneName = "Team A";
        Project project = new Project();
        project.setId(projectId);

        when(projectRepo.find((long)projectId)).thenReturn(project);

        String result = controller.addSwimlane((long)projectId, swimlaneName);

        assertEquals("redirect:/board/" + projectId, result);
        ArgumentCaptor<fr.uha.ensisa.gl.entities.Swimlane> captor = ArgumentCaptor.forClass(fr.uha.ensisa.gl.entities.Swimlane.class);
        verify(swimlaneRepo).persist(captor.capture());
        fr.uha.ensisa.gl.entities.Swimlane capturedSwimlane = captor.getValue();
        assertEquals(swimlaneName, capturedSwimlane.getName());
        assertEquals(projectId, capturedSwimlane.getProjectId());
    }

    @Test
    void testAddSwimlaneNonExistentProject() {
        int projectId = 999;
        String swimlaneName = "Team B";

        when(projectRepo.find((long)projectId)).thenReturn(null);

        String result = controller.addSwimlane((long)projectId, swimlaneName);

        assertEquals("redirect:/project/list", result);
        verify(swimlaneRepo, never()).persist(any(fr.uha.ensisa.gl.entities.Swimlane.class));
    }

    @Test
    void testShowBoardIncludesSwimlanesAndStoriesBySwimlaneAndColumn() {
        int projectId = 1;
        Project project = new Project();
        project.setId(projectId);
        project.setName("Test Project");

        // Mock columns
        Column column1 = new Column();
        column1.setId(1);
        column1.setName("BACKLOG");
        Column column2 = new Column();
        column2.setId(2);
        column2.setName("DONE");
        List<Column> columns = Arrays.asList(column1, column2);

        // Mock swimlanes
        fr.uha.ensisa.gl.entities.Swimlane swimlane1 = new fr.uha.ensisa.gl.entities.Swimlane(1L, "Team A", projectId);
        fr.uha.ensisa.gl.entities.Swimlane swimlane2 = new fr.uha.ensisa.gl.entities.Swimlane(2L, "Team B", projectId);
        List<fr.uha.ensisa.gl.entities.Swimlane> swimlanes = Arrays.asList(swimlane1, swimlane2);

        // Mock stories
        Story story1 = new Story();
        story1.setId(1);
        story1.setTitle("Story 1");
        story1.setProjectId((long)projectId);
        story1.setColumnId(column1.getId());
        story1.setSwimlaneId((Long) swimlane1.getId());

        Story story2 = new Story();
        story2.setId(2);
        story2.setTitle("Story 2");
        story2.setProjectId((long)projectId);
        story2.setColumnId(column2.getId());
        story2.setSwimlaneId((Long) swimlane1.getId());
        List<Story> allStories = Arrays.asList(story1, story2);


        when(projectRepo.find((long)projectId)).thenReturn(project);
        when(columnRepo.findByProject((long)projectId)).thenReturn(columns);
        when(swimlaneRepo.findByProject(projectId)).thenReturn(swimlanes);
        when(storyRepo.findByProject((long)projectId)).thenReturn(allStories);


        ModelAndView mav = controller.showBoard((long)projectId);

        assertNotNull(mav);
        assertEquals("board", mav.getViewName());
        
        // Verify project, columns, and swimlanes are in the model
        assertEquals(project, mav.getModel().get("project"));
        assertEquals(columns, mav.getModel().get("columns"));
        assertEquals(swimlanes, mav.getModel().get("swimlanes"));

        // Verify storiesBySwimlaneAndColumn map
        @SuppressWarnings("unchecked")
        Map<Long, Map<Long, List<Story>>> storiesMap = 
                (Map<Long, Map<Long, List<Story>>>) mav.getModel().get("storiesBySwimlaneAndColumn");
        assertNotNull(storiesMap);
        assertFalse(storiesMap.isEmpty());

        assertTrue(storiesMap.containsKey(swimlane1.getId()));
        assertTrue(storiesMap.get(swimlane1.getId()).containsKey(column1.getId()));
        assertTrue(storiesMap.get(swimlane1.getId()).get(column1.getId()).contains(story1));

        assertTrue(storiesMap.containsKey(swimlane1.getId()));
        assertTrue(storiesMap.get(swimlane1.getId()).containsKey(column2.getId()));
        assertTrue(storiesMap.get(swimlane1.getId()).get(column2.getId()).contains(story2));
        
        // Ensure storyRepo.findByProject is called
        verify(storyRepo).findByProject((long)projectId);
    }
}

