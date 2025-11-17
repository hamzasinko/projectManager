package fr.uha.ensisa.gl;

import org.junit.jupiter.api.Test;
import java.util.List;

import fr.uha.ensisa.gl.entities.Tag;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TagTest {

    @Test
    public void testTag() {
        Tag t = new Tag();

        t.setId(1);
        t.setName("Important");
        t.setColor("Red");
        t.setCategories(List.of("Work"));

        assertEquals(1, t.getId());
        assertEquals("Important", t.getName());
        assertEquals("Red", t.getColor());
        assertEquals(List.of("Work"), t.getCategories());
    }
}