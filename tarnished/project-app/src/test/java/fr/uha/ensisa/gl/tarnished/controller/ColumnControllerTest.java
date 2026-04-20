package fr.uha.ensisa.gl.tarnished.controller;

import fr.uha.ensisa.gl.entities.Column;
import fr.uha.ensisa.gl.entities.Project;
import fr.uha.ensisa.gl.tarnished.config.PathHelper;
import fr.uha.ensisa.gl.tarnished.repos.ColumnRepo;
import fr.uha.ensisa.gl.tarnished.repos.ProjectRepo;
import fr.uha.ensisa.gl.tarnished.repos.RepoFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.Model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

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

    @Mock
    private PathHelper pathHelper;

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
        controller.setPathHelper(pathHelper);
        
        // Configure PathHelper mock
        when(pathHelper.redirect(anyString())).thenAnswer(invocation -> {
            String path = invocation.getArgument(0);
            return "redirect:" + path;
        });
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

    @Test
    void testCreateColumn_VerifiesSettersCalled() {
        when(projectRepo.find(1L)).thenReturn(project);
        when(columnRepo.findAll()).thenReturn(new ArrayList<>());

        ArgumentCaptor<Column> columnCaptor = ArgumentCaptor.forClass(Column.class);
        
        String result = controller.createColumn("Test Column", 3, 10, true, 1L);

        assertEquals("redirect:/columns", result);
        verify(columnRepo).persist(columnCaptor.capture());
        
        Column captured = columnCaptor.getValue();
        assertEquals("Test Column", captured.getName());
        assertEquals(3, captured.getPosition());
        assertEquals(10, captured.getMaxCapacity());
        assertTrue(captured.isHasSubColumns());
        assertNotNull(captured.getStories());
        assertEquals(project, captured.getProject());
    }

    @Test
    void testCreateColumn_WithExact25Characters() {
        when(projectRepo.find(1L)).thenReturn(project);
        when(columnRepo.findAll()).thenReturn(new ArrayList<>());
        
        String name25Chars = "1234567890123456789012345"; // exactly 25 chars
        
        ArgumentCaptor<Column> columnCaptor = ArgumentCaptor.forClass(Column.class);
        String result = controller.createColumn(name25Chars, 1, 5, false, 1L);

        assertEquals("redirect:/columns", result);
        verify(columnRepo).persist(columnCaptor.capture());
        assertEquals(name25Chars, columnCaptor.getValue().getName());
    }

    @Test
    void testCreateColumn_WithBoundaryConditionNameLengthEquals25() {
        when(projectRepo.find(1L)).thenReturn(project);
        when(columnRepo.findAll()).thenReturn(new ArrayList<>());
        
        // Test exactement 25 caractères - devrait passer sans troncature (tuer le mutant "changed conditional boundary" qui change > en >=)
        // Si le mutant change > en >=, alors name.length() >= 25 serait vrai avec 25 chars et tronquerait
        // Donc on vérifie explicitement que le nom reste de 25 caractères (non tronqué)
        String nameExactly25 = "A".repeat(25);
        
        ArgumentCaptor<Column> captor = ArgumentCaptor.forClass(Column.class);
        String result = controller.createColumn(nameExactly25, 1, 5, false, 1L);
        
        assertEquals("redirect:/columns", result);
        verify(columnRepo).persist(captor.capture());
        Column captured = captor.getValue();
        assertEquals(25, captured.getName().length(), "Name should not be truncated when exactly 25 characters");
        assertEquals(nameExactly25, captured.getName(), "Name should remain unchanged when exactly 25 characters");
        // Vérifie que substring n'a pas été appelé (en vérifiant que le nom original est préservé)
        assertFalse(captured.getName().equals(nameExactly25.substring(0, 24)), "Name should not be truncated to 24 chars");
    }

    @Test
    void testCreateColumn_With24Characters_ShouldNotTruncate() {
        when(projectRepo.find(1L)).thenReturn(project);
        when(columnRepo.findAll()).thenReturn(new ArrayList<>());
        
        // Test avec 24 caractères - devrait passer sans modification
        String name24Chars = "A".repeat(24);
        
        ArgumentCaptor<Column> captor = ArgumentCaptor.forClass(Column.class);
        String result = controller.createColumn(name24Chars, 1, 5, false, 1L);
        
        assertEquals("redirect:/columns", result);
        verify(columnRepo).persist(captor.capture());
        assertEquals(24, captor.getValue().getName().length());
        assertEquals(name24Chars, captor.getValue().getName());
    }

    @Test
    void testCreateColumn_WithDuplicateNameHavingNullName() {
        when(projectRepo.find(1L)).thenReturn(project);
        
        Column existingColumn = new Column();
        existingColumn.setId(2);
        existingColumn.setName(null); // Column with null name (pour tuer le mutant "negated conditional" dans le lambda)
        existingColumn.setProject(project);
        
        when(columnRepo.findAll()).thenReturn(Arrays.asList(existingColumn));
        
        String result = controller.createColumn("New Column", 1, 5, false, 1L);
        
        assertEquals("redirect:/columns", result);
        verify(columnRepo).persist(any(Column.class));
    }

    @Test
    void testCreateColumn_With26Characters_Truncated() {
        when(projectRepo.find(1L)).thenReturn(project);
        when(columnRepo.findAll()).thenReturn(new ArrayList<>());
        
        String name26Chars = "12345678901234567890123456"; // 26 chars, should be truncated to 25
        
        ArgumentCaptor<Column> columnCaptor = ArgumentCaptor.forClass(Column.class);
        String result = controller.createColumn(name26Chars, 1, 5, false, 1L);

        assertEquals("redirect:/columns", result);
        verify(columnRepo).persist(columnCaptor.capture());
        assertEquals(25, columnCaptor.getValue().getName().length());
        assertEquals(name26Chars.substring(0, 25), columnCaptor.getValue().getName());
    }

    @Test
    void testCreateColumn_WithNullProject_VerifiesNoSetProject() {
        when(columnRepo.findAll()).thenReturn(new ArrayList<>());

        ArgumentCaptor<Column> columnCaptor = ArgumentCaptor.forClass(Column.class);
        
        String result = controller.createColumn("Test Column", 2, 5, false, null);

        assertEquals("redirect:/columns", result);
        verify(columnRepo).persist(columnCaptor.capture());
        verify(projectRepo, never()).find(anyLong());
        
        Column captured = columnCaptor.getValue();
        assertNull(captured.getProject());
        assertNotNull(captured.getStories());
        assertEquals(2, captured.getPosition());
        assertEquals(5, captured.getMaxCapacity());
    }

    @Test
    void testEditColumn_WithExact25Characters() {
        when(columnRepo.find(1L)).thenReturn(column);
        
        String name25Chars = "1234567890123456789012345";
        
        String result = controller.editColumn(1L, name25Chars, 2, 10);
        
        assertEquals("redirect:/columns", result);
        assertEquals(name25Chars, column.getName());
        assertEquals(2, column.getPosition());
        assertEquals(10, column.getMaxCapacity());
    }

    @Test
    void testEditColumn_WithBoundary26Characters_Truncated() {
        when(columnRepo.find(1L)).thenReturn(column);
        
        String name26Chars = "12345678901234567890123456";
        String result = controller.editColumn(1L, name26Chars, 3, 15);
        
        assertEquals("redirect:/columns", result);
        assertEquals(25, column.getName().length());
        assertEquals(name26Chars.substring(0, 25), column.getName());
        assertEquals(3, column.getPosition());
        assertEquals(15, column.getMaxCapacity());
    }

    @Test
    void testEditColumn_WithBoundaryConditionNameLengthEquals25() {
        when(columnRepo.find(1L)).thenReturn(column);
        
        // Test exactement 25 caractères - devrait passer sans troncature
        String nameExactly25 = "B".repeat(25);
        String result = controller.editColumn(1L, nameExactly25, 2, 10);
        
        assertEquals("redirect:/columns", result);
        assertEquals(25, column.getName().length());
        assertEquals(nameExactly25, column.getName());
    }

    @Test
    void testCreateColumn_WithDuplicateNameHavingNullProjectIdAndNullProject() {
        // Test pour tuer le mutant "negated conditional" dans (projectId == null && c.getProject() == null)
        // Si projectId est null ET la colonne existante a aussi un projet null, alors c'est un doublon
        Column existingColumn = new Column();
        existingColumn.setId(2);
        existingColumn.setName("To Do");
        existingColumn.setProject(null); // Column with null project
        
        when(columnRepo.findAll()).thenReturn(Arrays.asList(existingColumn));
        
        String result = controller.createColumn("To Do", 1, 5, false, null);
        
        assertEquals("redirect:/columns?error=Column already exists", result);
        verify(columnRepo, never()).persist(any(Column.class));
    }

    @Test
    void testCreateColumn_WithDifferentNameButSameProjectIdNull() {
        // Test que deux colonnes avec des noms différents mais toutes deux sans projet ne sont pas considérées comme doublons
        Column existingColumn = new Column();
        existingColumn.setId(2);
        existingColumn.setName("Different Name");
        existingColumn.setProject(null);
        
        when(columnRepo.findAll()).thenReturn(Arrays.asList(existingColumn));
        
        String result = controller.createColumn("To Do", 1, 5, false, null);
        
        assertEquals("redirect:/columns", result);
        verify(columnRepo).persist(any(Column.class));
    }

    @Test
    void testCreateColumn_WithBoundaryCondition26Chars_VerifiesTruncation() {
        // Test boundary condition: exactly 26 chars should be truncated to 25
        // This kills the mutation that changes > 25 to >= 25
        when(projectRepo.find(1L)).thenReturn(project);
        when(columnRepo.findAll()).thenReturn(new ArrayList<>());
        
        String name26Chars = "A".repeat(26); // exactly 26 chars
        
        ArgumentCaptor<Column> columnCaptor = ArgumentCaptor.forClass(Column.class);
        String result = controller.createColumn(name26Chars, 1, 5, false, 1L);
        
        assertEquals("redirect:/columns", result);
        verify(columnRepo).persist(columnCaptor.capture());
        Column captured = columnCaptor.getValue();
        assertEquals(25, captured.getName().length(), "Name should be truncated from 26 to 25 characters");
        assertEquals(name26Chars.substring(0, 25), captured.getName());
    }

    @Test
    void testEditColumn_WithBoundaryCondition26Chars_VerifiesTruncation() {
        // Test boundary condition for editColumn: exactly 26 chars should be truncated to 25
        when(columnRepo.find(1L)).thenReturn(column);
        
        String name26Chars = "B".repeat(26); // exactly 26 chars
        String result = controller.editColumn(1L, name26Chars, 2, 10);
        
        assertEquals("redirect:/columns", result);
        assertEquals(25, column.getName().length(), "Name should be truncated from 26 to 25 characters");
        assertEquals(name26Chars.substring(0, 25), column.getName());
        verify(columnRepo).persist(column);
    }
}
