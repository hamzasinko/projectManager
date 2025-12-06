package fr.uha.ensisa.gl.tarnished.controller;

import fr.uha.ensisa.gl.entities.Column;
import fr.uha.ensisa.gl.entities.Project;
import fr.uha.ensisa.gl.tarnished.repos.ColumnRepo;
import fr.uha.ensisa.gl.tarnished.repos.ProjectRepo;
import fr.uha.ensisa.gl.tarnished.repos.RepoFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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
}
