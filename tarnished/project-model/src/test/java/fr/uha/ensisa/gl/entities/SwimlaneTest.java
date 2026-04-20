package fr.uha.ensisa.gl.entities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Swimlane Entity Tests")
class SwimlaneTest {

    @Test
    @DisplayName("Should create Swimlane with no-args constructor")
    void testNoArgsConstructor() {
        Swimlane swimlane = new Swimlane();
        assertNotNull(swimlane);
        assertEquals(0, swimlane.getId());
        assertNull(swimlane.getName());
        assertEquals(0, swimlane.getProjectId());
    }

    @Test
    @DisplayName("Should create Swimlane with all-args constructor")
    void testAllArgsConstructor() {
        long id = 1L;
        String name = "Sprint 1";
        int projectId = 10;

        Swimlane swimlane = new Swimlane(id, name, projectId);

        assertEquals(id, swimlane.getId());
        assertEquals(name, swimlane.getName());
        assertEquals(projectId, swimlane.getProjectId());
    }

    @Test
    @DisplayName("Should set and get id")
    void testSetGetId() {
        Swimlane swimlane = new Swimlane();
        long id = 5L;
        swimlane.setId(id);
        assertEquals(id, swimlane.getId());
    }

    @Test
    @DisplayName("Should set and get name")
    void testSetGetName() {
        Swimlane swimlane = new Swimlane();
        String name = "Backend Team";
        swimlane.setName(name);
        assertEquals(name, swimlane.getName());
    }

    @Test
    @DisplayName("Should set and get projectId")
    void testSetGetProjectId() {
        Swimlane swimlane = new Swimlane();
        int projectId = 42;
        swimlane.setProjectId(projectId);
        assertEquals(projectId, swimlane.getProjectId());
    }

    @Test
    @DisplayName("Should handle null name")
    void testNullName() {
        Swimlane swimlane = new Swimlane(1L, null, 10);
        assertNull(swimlane.getName());
    }

    @Test
    @DisplayName("Should handle empty name")
    void testEmptyName() {
        Swimlane swimlane = new Swimlane(1L, "", 10);
        assertEquals("", swimlane.getName());
    }

    @Test
    @DisplayName("Should test equals and hashCode")
    void testEqualsAndHashCode() {
        Swimlane swimlane1 = new Swimlane(1L, "Sprint 1", 10);
        Swimlane swimlane2 = new Swimlane(1L, "Sprint 1", 10);
        Swimlane swimlane3 = new Swimlane(2L, "Sprint 2", 10);

        assertEquals(swimlane1, swimlane2);
        assertNotEquals(swimlane1, swimlane3);
        assertEquals(swimlane1.hashCode(), swimlane2.hashCode());
    }

    @Test
    @DisplayName("Should test toString")
    void testToString() {
        Swimlane swimlane = new Swimlane(1L, "Sprint 1", 10);
        String toString = swimlane.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("Sprint 1"));
    }
}

