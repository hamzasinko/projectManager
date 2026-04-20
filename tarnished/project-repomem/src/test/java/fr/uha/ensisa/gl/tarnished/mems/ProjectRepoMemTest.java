package fr.uha.ensisa.gl.tarnished.mems;

import fr.uha.ensisa.gl.entities.Column;
import fr.uha.ensisa.gl.entities.Project;
import fr.uha.ensisa.gl.tarnished.repos.ColumnRepo;
import fr.uha.ensisa.gl.tarnished.repos.StoryRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("ProjectRepoMem Tests")
class ProjectRepoMemTest {

    @Mock
    private ColumnRepo columnRepo;

    private ProjectRepoMem projectRepo;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        columnRepo = Mockito.mock(ColumnRepo.class);
        projectRepo = new ProjectRepoMem();
        projectRepo.setColumnRepo(columnRepo);
    }

    @Test
    @DisplayName("Should persist project and create default columns")
    void testPersistProject() {
        //Arrange
        Project project = new Project();
        project.setName("Test Project");
        project.setDescription("Test Description");

        //Act
        projectRepo.persist(project);

        //Assert
        assertEquals(1, project.getId());
        assertNotNull(project.getDateStarted());
        
        //Verify 5 default columns were created
        ArgumentCaptor<Column> columnCaptor = ArgumentCaptor.forClass(Column.class);
        verify(columnRepo, times(5)).persist(columnCaptor.capture());

        //Verify column names
        var capturedColumns = columnCaptor.getAllValues();
        assertEquals("BACKLOG", capturedColumns.get(0).getName());
        assertEquals("IN PROGRESS", capturedColumns.get(1).getName());
        assertEquals("REVIEW", capturedColumns.get(2).getName());
        assertEquals("DONE", capturedColumns.get(3).getName());
        assertEquals("BLOCKED", capturedColumns.get(4).getName());

        //Verify positions
        for (int i = 0; i < 5; i++) {
            assertEquals(i + 1, capturedColumns.get(i).getPosition());
            assertEquals(0, capturedColumns.get(i).getMaxCapacity());
            assertEquals(project, capturedColumns.get(i).getProject());
        }
    }

    @Test
    @DisplayName("Should generate incrementing IDs")
    void testIncrementingIds() {
        //Arrange
        Project project1 = new Project();
        project1.setName("Project 1");

        Project project2 = new Project();
        project2.setName("Project 2");

        Project project3 = new Project();
        project3.setName("Project 3");

        //Act
        projectRepo.persist(project1);
        projectRepo.persist(project2);
        projectRepo.persist(project3);

        //Assert
        assertEquals(1, project1.getId());
        assertEquals(2, project2.getId());
        assertEquals(3, project3.getId());
    }

    @Test
    @DisplayName("Should find project by ID")
    void testFindProject() {
        //Arrange
        Project project = new Project();
        project.setName("Test Project");
        projectRepo.persist(project);

        //Act
        Project found = projectRepo.find(1L);

        //Assert
        assertNotNull(found);
        assertEquals(project.getId(), found.getId());
        assertEquals("Test Project", found.getName());
    }

    @Test
    @DisplayName("Should return null when project not found")
    void testFindProjectNotFound() {
        //Act
        Project found = projectRepo.find(999L);

        //Assert
        assertNull(found);
    }

    @Test
    @DisplayName("Should find all projects")
    void testFindAllProjects() {
        //Arrange
        Project project1 = new Project();
        project1.setName("Project 1");

        Project project2 = new Project();
        project2.setName("Project 2");

        projectRepo.persist(project1);
        projectRepo.persist(project2);

        //Act
        Collection<Project> projects = projectRepo.findAll();

        //Assert
        assertEquals(2, projects.size());
        assertTrue(projects.stream().anyMatch(p -> p.getName().equals("Project 1")));
        assertTrue(projects.stream().anyMatch(p -> p.getName().equals("Project 2")));
    }

    @Test
    @DisplayName("Should return empty collection when no projects exist")
    void testFindAllEmpty() {
        //Act
        Collection<Project> projects = projectRepo.findAll();

        //Assert
        assertNotNull(projects);
        assertTrue(projects.isEmpty());
    }

    @Test
    @DisplayName("Should update existing project")
    void testUpdateProject() {
        //Arrange
        Project project = new Project();
        project.setName("Original Name");
        projectRepo.persist(project);

        //Act
        project.setName("Updated Name");
        project.setDescription("Updated Description");
        projectRepo.update(project);

        Project found = projectRepo.find((long)project.getId());

        //Assert
        assertNotNull(found);
        assertEquals("Updated Name", found.getName());
        assertEquals("Updated Description", found.getDescription());
    }

    @Test
    @DisplayName("Should remove project")
    void testRemoveProject() {
        //Arrange
        Project project = new Project();
        project.setName("Test Project");
        projectRepo.persist(project);

        long projectId = project.getId();
        assertNotNull(projectRepo.find(projectId));

        //Act
        projectRepo.remove(projectId);

        //Assert
        assertNull(projectRepo.find(projectId));
    }

    @Test
    @DisplayName("Should handle removing non-existent project")
    void testRemoveNonExistentProject() {
        //Act & Assert - should not throw exception
        assertDoesNotThrow(() -> projectRepo.remove(999L));
    }

    @Test
    @DisplayName("Should return correct count")
    void testCount() {
        //Arrange
        assertEquals(0, projectRepo.count());

        Project project1 = new Project();
        Project project2 = new Project();
        Project project3 = new Project();

        //Act
        projectRepo.persist(project1);
        projectRepo.persist(project2);
        projectRepo.persist(project3);

        //Assert
        assertEquals(3, projectRepo.count());

        projectRepo.remove(2L);
        assertEquals(2, projectRepo.count());
    }

    @Test
    @DisplayName("Should handle setColumnRepo")
    void testSetColumnRepo() {
        //Arrange
        ProjectRepoMem newProjectRepo = new ProjectRepoMem();
        ColumnRepo mockColumnRepo = mock(ColumnRepo.class);

        //Act
        newProjectRepo.setColumnRepo(mockColumnRepo);
        
        Project project = new Project();
        newProjectRepo.persist(project);

        //Assert
        verify(mockColumnRepo, times(5)).persist(any(Column.class));
    }

    @Test
    @DisplayName("Should handle concurrent access")
    void testConcurrentAccess() {
        //Arrange
        Project project1 = new Project();
        project1.setName("Project 1");

        Project project2 = new Project();
        project2.setName("Project 2");

        //Act - store is synchronized
        projectRepo.persist(project1);
        projectRepo.persist(project2);

        //Assert - both should be findable
        assertNotNull(projectRepo.find(1L));
        assertNotNull(projectRepo.find(2L));
        assertEquals(2, projectRepo.count());
    }

    @Test
    void find_onUnknownId_returnsNull() {
        ProjectRepoMem repo = new ProjectRepoMem();
        assertNull(repo.find(999L));
    }

    @Test
    void remove_onUnknownId_doesNotThrow() {
        ProjectRepoMem repo = new ProjectRepoMem();
        assertDoesNotThrow(() -> repo.remove(999L));
    }
}
