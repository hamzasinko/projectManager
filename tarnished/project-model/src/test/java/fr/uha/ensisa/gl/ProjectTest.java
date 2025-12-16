package fr.uha.ensisa.gl;

import fr.uha.ensisa.gl.entities.Column;
import fr.uha.ensisa.gl.entities.Project;
import fr.uha.ensisa.gl.entities.User;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProjectTest {
    @Test
    void testNoArgsConstructorAndSetters() {
        Project project = new Project(); // NoArgsConstructor

        int id = 5;
        String name = "MyProject";
        String desc = "Test project";
        Date dateStarted = new Date();
        Date dateEnded = new Date();

        User owner = new User();
        List<User> members = new ArrayList<>();
        List<Column> workflow = new ArrayList<>();

        project.setId(id);
        project.setName(name);
        project.setDescription(desc);
        project.setDateStarted(dateStarted);
        project.setDateEnded(dateEnded);
        project.setOwner(owner);
        project.setMembers(members);
        project.setWorkFlow(workflow);

        assertEquals(id, project.getId());
        assertEquals(name, project.getName());
        assertEquals(desc, project.getDescription());
        assertEquals(dateStarted, project.getDateStarted());
        assertEquals(dateEnded, project.getDateEnded());
        assertEquals(owner, project.getOwner());
        assertEquals(members, project.getMembers());
        assertEquals(workflow, project.getWorkFlow());
    }

    @Test
    void testAllArgsConstructor() {
        int id = 10;
        String name = "Project X";
        String desc = "Secret project";

        Date dateStarted = new Date();
        Date dateEnded = new Date();
        User owner = new User();
        List<User> members = new ArrayList<>();
        List<Column> workflow = new ArrayList<>();

        Project project = new Project(
                id, name, desc, dateStarted, dateEnded, owner, members, workflow,new ArrayList<>()
        );

        assertEquals(id, project.getId());
        assertEquals(name, project.getName());
        assertEquals(desc, project.getDescription());
        assertEquals(dateStarted, project.getDateStarted());
        assertEquals(dateEnded, project.getDateEnded());
        assertEquals(owner, project.getOwner());
        assertEquals(members, project.getMembers());
        assertEquals(workflow, project.getWorkFlow());
    }
}
