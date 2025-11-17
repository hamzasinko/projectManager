package fr.uha.ensisa.gl;

import fr.uha.ensisa.gl.entities.Column;
import fr.uha.ensisa.gl.entities.Project;
import fr.uha.ensisa.gl.entities.Story;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ColumnTest {

    @Mock
    Project project;

    @Mock
    Story story1;

    @Mock
    Story story2;

    @Test
    void columnGettersAndSettersWork() {
        Column c = new Column();
        c.setId(123);
        c.setName("col");
        c.setPosition(1);
        c.setMaxCapacity(50);
        c.setProject(project);
        List<Story> stories = List.of(story1, story2);
        c.setStories(stories);

        assertEquals(123, c.getId());
        assertEquals("col", c.getName());
        assertEquals(1, c.getPosition());
        assertEquals(50, c.getMaxCapacity());
        assertSame(project, c.getProject());
        assertEquals(stories, c.getStories());
    }
}
