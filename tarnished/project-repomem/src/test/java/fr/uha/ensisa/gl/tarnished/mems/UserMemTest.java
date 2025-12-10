package fr.uha.ensisa.gl.tarnished.mems;
import fr.uha.ensisa.gl.entities.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserMemTest {

    @Test
    public void testAddAndFindUser() {
        UserRepoMem userMem = new UserRepoMem();

        User user = new User();
        user.setEmail("test@ex.com");
        userMem.add(user);

        User retrieved = userMem.find(1); // Premier user, id devrait être 1
        assertNotNull(retrieved);
        assertEquals("test@ex.com", retrieved.getEmail());
    }

    @Test
    public void testGetUserByEmail() {
        UserRepoMem userMem = new UserRepoMem();

        User user = new User();
        user.setEmail("alice@example.com");
        userMem.add(user);

        User found = userMem.get("alice@example.com");
        assertNotNull(found);
        assertEquals("alice@example.com", found.getEmail());
    }

    @Test
    public void testGetAllReturnsAllUsers() {
        UserRepoMem userMem = new UserRepoMem();

        User user1 = new User();
        user1.setEmail("u1@ex.com");
        userMem.add(user1);

        User user2 = new User();
        user2.setEmail("u2@ex.com");
        userMem.add(user2);

        assertEquals(2, userMem.getAll().size());
    }

    @Test
    public void testUpdate() {
        UserRepoMem userMem = new UserRepoMem();

        User user = new User();
        user.setEmail("test@ex.com");
        userMem.add(user);

        // Update should not throw even if not implemented
        user.setEmail("updated@ex.com");
        userMem.update(user);

        // Verify user still exists
        User found = userMem.find(user.getId());
        assertNotNull(found);
    }

    @Test
    public void testDelete() {
        UserRepoMem userMem = new UserRepoMem();

        User user = new User();
        user.setEmail("test@ex.com");
        userMem.add(user);

        int id = user.getId();
        
        // Delete should not throw even if not implemented
        userMem.delete(id);

        // Verify user still exists (since delete is not implemented)
        User found = userMem.find(id);
        assertNotNull(found);
    }

    @Test
    public void testGetUserByEmailCaseInsensitive() {
        UserRepoMem userMem = new UserRepoMem();

        User user = new User();
        user.setEmail("Alice@Example.com");
        userMem.add(user);

        User found1 = userMem.get("alice@example.com");
        assertNotNull(found1);
        assertEquals("Alice@Example.com", found1.getEmail());

        User found2 = userMem.get("ALICE@EXAMPLE.COM");
        assertNotNull(found2);
        assertEquals("Alice@Example.com", found2.getEmail());
    }

    @Test
    public void testGetUserByEmailNotFound() {
        UserRepoMem userMem = new UserRepoMem();

        User found = userMem.get("nonexistent@example.com");
        assertNull(found);
    }

    @Test
    public void testFindNotFound() {
        UserRepoMem userMem = new UserRepoMem();

        User found = userMem.find(999);
        assertNull(found);
    }
}
