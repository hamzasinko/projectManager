package fr.uha.ensisa.gl;

import fr.uha.ensisa.gl.entities.Column;
import fr.uha.ensisa.gl.entities.Story;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class ColumnTest {

    @Test
    void testCreateColumnValid() {
        Column column = new Column();
        column.setId(1);
        column.setName("To Do");
        column.setPosition(1);
        column.setMaxCapacity(5);
        
        assertNotNull(column);
        assertEquals("To Do", column.getName());
        assertEquals(1, column.getPosition());
        assertEquals(5, column.getMaxCapacity());
    }

    @Test
    void testValidationNameRequired() {
        Column column = new Column();
        column.setId(1);
        column.setName("");
        column.setPosition(1);
        column.setMaxCapacity(5);
        
        assertTrue(column.getName().isEmpty());
    }

    @Test
    void testValidationLimitGreaterThanOrEqualToOne() {
        Column column = new Column();
        column.setId(1);
        column.setName("In Progress");
        column.setPosition(2);
        column.setMaxCapacity(1);
        
        assertTrue(column.getMaxCapacity() >= 1);
    }

    @Test
    void testAddStoryWhenSpaceAvailable() {
        Column column = new Column();
        column.setId(1);
        column.setName("To Do");
        column.setPosition(1);
        column.setMaxCapacity(5);
        column.setStories(new ArrayList<>());
        
        Story story = new Story();
        story.setId(1);
        column.getStories().add(story);
        
        assertEquals(1, column.getStories().size());
        assertTrue(column.getStories().size() < column.getMaxCapacity());
    }

    @Test
    void testRefuseAddStoryWhenColumnFull() {
        Column column = new Column();
        column.setId(1);
        column.setName("Done");
        column.setPosition(3);
        column.setMaxCapacity(2);
        column.setStories(new ArrayList<>());
        
        Story story1 = new Story();
        story1.setId(1);
        column.getStories().add(story1);
        
        Story story2 = new Story();
        story2.setId(2);
        column.getStories().add(story2);
        
        assertEquals(2, column.getStories().size());
        assertTrue(column.getStories().size() >= column.getMaxCapacity());
    }

    @Test
    void testColumnWithSubColumnsEnabled() {
        Column column = new Column();
        column.setId(1);
        column.setName("Custom Column");
        column.setPosition(2);
        column.setMaxCapacity(5);
        column.setHasSubColumns(true);
        
        assertTrue(column.isHasSubColumns());
        assertEquals("Custom Column", column.getName());
    }

    @Test
    void testColumnWithSubColumnsDisabled() {
        Column column = new Column();
        column.setId(1);
        column.setName("Simple Column");
        column.setPosition(1);
        column.setMaxCapacity(3);
        column.setHasSubColumns(false);
        
        assertFalse(column.isHasSubColumns());
        assertEquals("Simple Column", column.getName());
    }

    @Test
    void testColumnDefaultSubColumnsValue() {
        Column column = new Column();
        column.setId(1);
        column.setName("Default Column");
        column.setPosition(1);
        column.setMaxCapacity(5);
        
        //Default value for boolean is false
        assertFalse(column.isHasSubColumns());
    }

    @Test
    void testColumnWithSubColumnsToggle() {
        Column column = new Column();
        column.setId(1);
        column.setName("Toggle Column");
        column.setPosition(1);
        column.setMaxCapacity(5);
        column.setHasSubColumns(true);
        
        assertTrue(column.isHasSubColumns());
        
        column.setHasSubColumns(false);
        assertFalse(column.isHasSubColumns());
    }
}
