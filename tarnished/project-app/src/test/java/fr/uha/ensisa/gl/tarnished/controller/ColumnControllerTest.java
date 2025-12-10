package fr.uha.ensisa.gl.tarnished.controller;

import fr.uha.ensisa.gl.entities.Column;
import fr.uha.ensisa.gl.entities.Project;
import fr.uha.ensisa.gl.tarnished.repos.ColumnRepo;
import fr.uha.ensisa.gl.tarnished.repos.ProjectRepo;
import fr.uha.ensisa.gl.tarnished.repos.RepoFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.Model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

class ColumnControllerTest {

    @Mock
    private RepoFactory repoFactory;

    @Mock
    private ColumnRepo columnRepo;

    @Mock
    private ProjectRepo projectRepo;

    @Mock
    private Model model;

    @InjectMocks
    private ColumnController controller;

    private Column column;
    private Project project;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        column = new Column();
        column.setId(1);
        column.setName("To Do");
        column.setPosition(1);
        column.setMaxCapacity(5);
        column.setStories(new ArrayList<>());

        project = new Project();
        project.setId(1);
        project.setName("Test Project");

        when(repoFactory.getColumnRepo()).thenReturn(columnRepo);
        when(repoFactory.getProjectRepo()).thenReturn(projectRepo);
    }

    @Test
    void testListColumns() {
        Collection<Column> columns = Arrays.asList(column);
        when(columnRepo.findAll()).thenReturn(columns);

        String viewName = controller.listColumns(model);

        assertEquals("column-list", viewName);
        verify(model).addAttribute("columns", columns);
        verify(columnRepo).findAll();
    }

    @Test
    void testShowCreateForm() {
        String viewName = controller.showCreateForm(1L, model);

        assertEquals("column-create", viewName);
        verify(model).addAttribute("projectId", 1L);
    }

    @Test
    void testCreateColumnSuccess() {
        when(projectRepo.find(1L)).thenReturn(project);

        String result = controller.createColumn("To Do", 1, 5, 1L);

        assertEquals("redirect:/columns", result);
        verify(columnRepo).persist(any(Column.class));
    }

    @Test
    void testCreateColumnWithoutProject() {
        String result = controller.createColumn("To Do", 1, 5, null);

        assertEquals("redirect:/columns", result);
        verify(columnRepo).persist(any(Column.class));
    }

    @Test
    void testShowEditForm() {
        when(columnRepo.find(1L)).thenReturn(column);

        String viewName = controller.showEditForm(1L, model);

        assertEquals("column-edit", viewName);
        verify(model).addAttribute("column", column);
        verify(columnRepo).find(1L);
    }

    @Test
    void testEditColumnSuccess() {
        when(columnRepo.find(1L)).thenReturn(column);

        String result = controller.editColumn(1L, "Updated Name", 2, 10);

        assertEquals("redirect:/columns", result);
        assertEquals("Updated Name", column.getName());
        assertEquals(2, column.getPosition());
        assertEquals(10, column.getMaxCapacity());
        verify(columnRepo).persist(column);
    }

    @Test
    void testEditColumnNotFound() {
        when(columnRepo.find(1L)).thenReturn(null);

        String result = controller.editColumn(1L, "Updated Name", 2, 10);

        assertEquals("redirect:/columns", result);
        verify(columnRepo, never()).persist(any());
    }

    @Test
    void testDeleteColumn() {
        String result = controller.deleteColumn(1L);

        assertEquals("redirect:/columns", result);
        verify(columnRepo).remove(1L);
    }

    @Test
    void testReorderColumn() {
        when(columnRepo.find(1L)).thenReturn(column);

        String result = controller.reorderColumn(1L, 3);

        assertEquals("redirect:/columns", result);
        verify(columnRepo).reorder(1L, 3);
    }

    @Test
    void testMoveStorySuccess() {
        String result = controller.moveStory(1L, 2L, 1L);

        assertEquals("redirect:/stories", result);
        verify(columnRepo).moveStoryBetweenColumns(1L, 1L, 2L);
    }

    @Test
    void testMoveStoryColumnFull() {
        doThrow(new IllegalStateException("Column is full"))
                .when(columnRepo).moveStoryBetweenColumns(1L, 1L, 2L);

        String result = controller.moveStory(1L, 2L, 1L);

        assertEquals("redirect:/stories?error=Column is full", result);
        verify(columnRepo).moveStoryBetweenColumns(1L, 1L, 2L);
    }

    @Test
    void testMoveStoryWithoutFromColumn() {
        String result = controller.moveStory(1L, 2L, null);

        assertEquals("redirect:/stories", result);
        verify(columnRepo).moveStoryBetweenColumns(1L, null, 2L);
    }

    @Test
    void testCreateColumnWithLongName() {
        String longName = "A".repeat(30);
        when(projectRepo.find(1L)).thenReturn(project);
        
        controller.createColumn(longName, 1, 5, 1L);
        
        verify(columnRepo).persist(argThat(col -> col.getName().length() <= 25));
    }

    @Test
    void testEditColumnWithLongName() {
        Column col = new Column();
        when(columnRepo.find(1L)).thenReturn(col);
        String longName = "B".repeat(30);
        
        controller.editColumn(1L, longName, 2, 10);
        
        assertTrue(col.getName().length() <= 25);
    }

    @Test
    void testShowCreateFormWithProjectId() {
        controller.showCreateForm(123L, model);
        
        verify(model).addAttribute("projectId", 123L);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("editColumn with null column should redirect")
    public void testEditColumnNullColumn() {
        when(columnRepo.find(999L)).thenReturn(null);
        
        String result = controller.editColumn(999L, "Name", 1, 10);
        
        assertEquals("redirect:/columns", result);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("editColumn should truncate name and persist")
    public void testEditColumnTruncatesName() {
        fr.uha.ensisa.gl.entities.Column column = new fr.uha.ensisa.gl.entities.Column();
        when(columnRepo.find(1L)).thenReturn(column);
        
        String result = controller.editColumn(1L, "A".repeat(30), 2, 5);
        
        assertEquals("A".repeat(25), column.getName());
        assertEquals(2, column.getPosition());
        assertEquals(5, column.getMaxCapacity());
        verify(columnRepo).persist(column);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("moveStory should handle IllegalStateException")
    public void testMoveStoryHandlesException() {
        doThrow(new IllegalStateException("Full")).when(columnRepo).moveStoryBetweenColumns(1L, 2L, 3L);
        
        String result = controller.moveStory(1L, 3L, 2L);
        
        assertTrue(result.contains("error=Column is full"));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("createColumn should truncate names longer than 25 chars")
    public void testCreateColumnTruncatesName() {
        String longName = "A".repeat(30);
        
        String result = controller.createColumn(longName, 1, 10, null);
        
        ArgumentCaptor<fr.uha.ensisa.gl.entities.Column> captor = 
            ArgumentCaptor.forClass(fr.uha.ensisa.gl.entities.Column.class);
        verify(columnRepo).persist(captor.capture());
        
        fr.uha.ensisa.gl.entities.Column saved = captor.getValue();
        assertEquals("A".repeat(25), saved.getName());
        assertEquals("redirect:/columns", result);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("createColumn with short name should not truncate")
    public void testCreateColumnShortName() {
        String shortName = "TestColumn";
        
        String result = controller.createColumn(shortName, 1, 10, null);
        
        ArgumentCaptor<fr.uha.ensisa.gl.entities.Column> captor = 
            ArgumentCaptor.forClass(fr.uha.ensisa.gl.entities.Column.class);
        verify(columnRepo).persist(captor.capture());
        
        fr.uha.ensisa.gl.entities.Column saved = captor.getValue();
        assertEquals(shortName, saved.getName());
    }
}
