package fr.uha.ensisa.gl;

import fr.uha.ensisa.gl.entities.Story;
import fr.uha.ensisa.gl.entities.User;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UserTest {
    @Test
    void testNoArgsConstructorAndSetters() {
        User user = new User(); // NoArgsConstructor

        user.setId(1);
        user.setName("Hamza");
        user.setEmail("hamza@test.com");
        user.setPassword("1234");

        List<Story> stories = new ArrayList<>();
        user.setStories(stories);

        assertEquals(1, user.getId());
        assertEquals("Hamza", user.getName());
        assertEquals("hamza@test.com", user.getEmail());
        assertEquals("1234", user.getPassword());
        assertEquals(stories, user.getStories());
    }

    @Test
    void testAllArgsConstructor() {
        List<Story> stories = new ArrayList<>();
        User user = new User(1, "Hamza", "hamza@test.com", "1234", stories);

        assertEquals(1, user.getId());
        assertEquals("Hamza", user.getName());
        assertEquals("hamza@test.com", user.getEmail());
        assertEquals("1234", user.getPassword());
        assertEquals(stories, user.getStories());
    }
}