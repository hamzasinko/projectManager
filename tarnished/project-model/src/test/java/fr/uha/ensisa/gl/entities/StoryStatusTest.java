package fr.uha.ensisa.gl.entities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StoryStatus Enum Tests")
class StoryStatusTest {

    @Test
    @DisplayName("Should have all expected enum values")
    void testEnumValues() {
        StoryStatus[] values = StoryStatus.values();
        assertEquals(5, values.length);
        
        assertTrue(containsValue(values, StoryStatus.BACKLOG));
        assertTrue(containsValue(values, StoryStatus.IN_PROGRESS));
        assertTrue(containsValue(values, StoryStatus.REVIEW));
        assertTrue(containsValue(values, StoryStatus.DONE));
        assertTrue(containsValue(values, StoryStatus.BLOCKED));
    }

    @Test
    @DisplayName("Should parse enum from string")
    void testValueOf() {
        assertEquals(StoryStatus.BACKLOG, StoryStatus.valueOf("BACKLOG"));
        assertEquals(StoryStatus.IN_PROGRESS, StoryStatus.valueOf("IN_PROGRESS"));
        assertEquals(StoryStatus.REVIEW, StoryStatus.valueOf("REVIEW"));
        assertEquals(StoryStatus.DONE, StoryStatus.valueOf("DONE"));
        assertEquals(StoryStatus.BLOCKED, StoryStatus.valueOf("BLOCKED"));
    }

    @Test
    @DisplayName("Should throw exception for invalid enum value")
    void testInvalidValueOf() {
        assertThrows(IllegalArgumentException.class, () -> {
            StoryStatus.valueOf("INVALID_STATUS");
        });
    }

    @Test
    @DisplayName("Should have correct enum names")
    void testEnumNames() {
        assertEquals("BACKLOG", StoryStatus.BACKLOG.name());
        assertEquals("IN_PROGRESS", StoryStatus.IN_PROGRESS.name());
        assertEquals("REVIEW", StoryStatus.REVIEW.name());
        assertEquals("DONE", StoryStatus.DONE.name());
        assertEquals("BLOCKED", StoryStatus.BLOCKED.name());
    }

    @Test
    @DisplayName("Should test enum ordinal")
    void testEnumOrdinal() {
        StoryStatus[] values = StoryStatus.values();
        for (int i = 0; i < values.length; i++) {
            assertEquals(i, values[i].ordinal());
        }
    }

    private boolean containsValue(StoryStatus[] values, StoryStatus status) {
        for (StoryStatus value : values) {
            if (value == status) {
                return true;
            }
        }
        return false;
    }
}

