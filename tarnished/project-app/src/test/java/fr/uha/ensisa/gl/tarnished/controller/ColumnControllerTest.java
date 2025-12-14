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

        String result = controller.createColumn("To Do", 1, 5, false, 1L);

        assertEquals("redirect:/columns", result);
        verify(columnRepo).persist(any(Column.class));
    }

    @Test
    void testCreateColumnWithoutProject() {
        String result = controller.createColumn("To Do", 1, 5, false, null);

        assertEquals("redirect:/columns", result);
        verify(columnRepo).persist(any(Column.class));
    }

    @Test
    void testCreateColumnWithSubColumns() {
        when(projectRepo.find(1L)).thenReturn(project);

        String result = controller.createColumn("Custom Column", 2, 5, true, 1L);

        assertEquals("redirect:/columns", result);
        verify(columnRepo).persist(argThat(col -> 
            col.getName().equals("Custom Column") && 
            col.isHasSubColumns()
        ));
    }

    @Test
    void testCreateColumnWithoutSubColumns() {
        when(projectRepo.find(1L)).thenReturn(project);

        String result = controller.createColumn("Simple Column", 2, 5, false, 1L);

        assertEquals("redirect:/columns", result);
        verify(columnRepo).persist(argThat(col -> 
            col.getName().equals("Simple Column") && 
            !col.isHasSubColumns()
        ));
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
        when(projectRepo.find(1L)).thenReturn(project);
        String longName = "This is a very long column name that exceeds 25 characters";

        String result = controller.createColumn(longName, 1, 5, false, 1L);

        assertEquals("redirect:/columns", result);
        verify(columnRepo).persist(argThat(col -> 
            col.getName().length() <= 25
        ));
    }

    @Test
    void testCreateColumnWithDuplicateName() {
        when(projectRepo.find(1L)).thenReturn(project);
        Column existingColumn = new Column();
        existingColumn.setId(2);
        existingColumn.setName("To Do");
        existingColumn.setProject(project);
        when(columnRepo.findAll()).thenReturn(Arrays.asList(existingColumn));

        String result = controller.createColumn("To Do", 1, 5, false, 1L);

        assertEquals("redirect:/columns?error=Column already exists", result);
        verify(columnRepo, never()).persist(any(Column.class));
    }

    @Test
    void testCreateColumnWithDuplicateNameCaseInsensitive() {
        when(projectRepo.find(1L)).thenReturn(project);
        Column existingColumn = new Column();
        existingColumn.setId(2);
        existingColumn.setName("To Do");
        existingColumn.setProject(project);
        when(columnRepo.findAll()).thenReturn(Arrays.asList(existingColumn));

        String result = controller.createColumn("  to do  ", 1, 5, false, 1L);

        assertEquals("redirect:/columns?error=Column already exists", result);
        verify(columnRepo, never()).persist(any(Column.class));
    }

    @Test
    void testCreateColumnWithDuplicateNameDifferentProject() {
        when(projectRepo.find(1L)).thenReturn(project);
        Project otherProject = new Project();
        otherProject.setId(2);
        Column existingColumn = new Column();
        existingColumn.setId(2);
        existingColumn.setName("To Do");
        existingColumn.setProject(otherProject);
        when(columnRepo.findAll()).thenReturn(Arrays.asList(existingColumn));

        String result = controller.createColumn("To Do", 1, 5, false, 1L);

        assertEquals("redirect:/columns", result);
        verify(columnRepo).persist(any(Column.class));
    }

    @Test
    void testCreateColumnWithEmptyName() {
        when(projectRepo.find(1L)).thenReturn(project);

        String result = controller.createColumn("", 1, 5, false, 1L);

        assertEquals("redirect:/columns", result);
        verify(columnRepo).persist(any(Column.class));
    }

    @Test
    void testShowCreateFormWithoutProjectId() {
        String viewName = controller.showCreateForm(null, model);

        assertEquals("column-create", viewName);
        verify(model).addAttribute("projectId", null);
    }

    @Test
    void testEditColumnWithLongName() {
        when(columnRepo.find(1L)).thenReturn(column);
        String longName = "This is a very long column name that exceeds 25 characters";

        String result = controller.editColumn(1L, longName, 2, 10);

        assertEquals("redirect:/columns", result);
        assertTrue(column.getName().length() <= 25);
        verify(columnRepo).persist(column);
    }

    @Test
    void testEditColumnWithEmptyName() {
        when(columnRepo.find(1L)).thenReturn(column);

        String result = controller.editColumn(1L, "", 2, 10);

        assertEquals("redirect:/columns", result);
        verify(columnRepo).persist(column);
    }

    @Test
    void testCreateColumnTrimsName() {
        when(projectRepo.find(1L)).thenReturn(project);

        String result = controller.createColumn("  Trimmed Name  ", 1, 5, false, 1L);

        assertEquals("redirect:/columns", result);
        verify(columnRepo).persist(argThat(col -> 
            col.getName().equals("Trimmed Name")
        ));
    }
}
