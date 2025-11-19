package fr.uha.ensisa.gl.tarnished.mems;
import fr.uha.ensisa.gl.entities.User;
import fr.uha.ensisa.gl.tarnished.mems.UserMem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserMemTest {

    @Test
    public void testAddAndFindUser() {
        UserMem userMem = new UserMem();

        User user = new User();
        user.setEmail("test@ex.com");
        userMem.add(user);

        User retrieved = userMem.find(1); // Premier user, id devrait être 1
        assertNotNull(retrieved);
        assertEquals("test@ex.com", retrieved.getEmail());
    }

    @Test
    public void testGetUserByEmail() {
        UserMem userMem = new UserMem();

        User user = new User();
        user.setEmail("alice@example.com");
        userMem.add(user);

        User found = userMem.get("alice@example.com");
        assertNotNull(found);
        assertEquals("alice@example.com", found.getEmail());
    }

    @Test
    public void testGetAllReturnsAllUsers() {
        UserMem userMem = new UserMem();

        User user1 = new User();
        user1.setEmail("u1@ex.com");
        userMem.add(user1);

        User user2 = new User();
        user2.setEmail("u2@ex.com");
        userMem.add(user2);

        assertEquals(2, userMem.getAll().size());
    }
}
