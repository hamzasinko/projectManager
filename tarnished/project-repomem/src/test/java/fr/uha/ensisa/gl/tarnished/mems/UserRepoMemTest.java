package fr.uha.ensisa.gl.tarnished.mems;

import fr.uha.ensisa.gl.entities.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserRepoMem Tests")
class UserRepoMemTest {

    private UserRepoMem userRepo;

    @BeforeEach
    void setUp() {
        userRepo = new UserRepoMem();
    }

    @Test
    @DisplayName("Should add user with incrementing ID")
    void testAddUser() {
        //Arrange
        User user = new User();
        user.setName("John Doe");
        user.setEmail("john@example.com");
        user.setPassword("password123");

        //Act
        userRepo.add(user);

        //Assert
        assertEquals(1, user.getId());
    }

    @Test
    @DisplayName("Should generate incrementing IDs for multiple users")
    void testIncrementingIds() {
        //Arrange
        User user1 = new User();
        user1.setEmail("user1@example.com");

        User user2 = new User();
        user2.setEmail("user2@example.com");

        User user3 = new User();
        user3.setEmail("user3@example.com");

        //Act
        userRepo.add(user1);
        userRepo.add(user2);
        userRepo.add(user3);

        //Assert
        assertEquals(1, user1.getId());
        assertEquals(2, user2.getId());
        assertEquals(3, user3.getId());
    }

    @Test
    @DisplayName("Should find user by ID")
    void testFindById() {
        //Arrange
        User user = new User();
        user.setName("Jane Smith");
        user.setEmail("jane@example.com");
        userRepo.add(user);

        //Act
        User found = userRepo.find(1);

        //Assert
        assertNotNull(found);
        assertEquals("Jane Smith", found.getName());
        assertEquals("jane@example.com", found.getEmail());
    }

    @Test
    @DisplayName("Should return null when user ID not found")
    void testFindByIdNotFound() {
        //Act
        User found = userRepo.find(999);

        //Assert
        assertNull(found);
    }

    @Test
    @DisplayName("Should get user by email (case-insensitive)")
    void testGetByEmail() {
        //Arrange
        User user = new User();
        user.setName("Bob Johnson");
        user.setEmail("bob@example.com");
        userRepo.add(user);

        //Act
        User foundLowercase = userRepo.get("bob@example.com");
        User foundUppercase = userRepo.get("BOB@EXAMPLE.COM");
        User foundMixedCase = userRepo.get("Bob@Example.Com");

        //Assert
        assertNotNull(foundLowercase);
        assertNotNull(foundUppercase);
        assertNotNull(foundMixedCase);
        assertEquals("Bob Johnson", foundLowercase.getName());
        assertEquals("Bob Johnson", foundUppercase.getName());
        assertEquals("Bob Johnson", foundMixedCase.getName());
    }

    @Test
    @DisplayName("Should return null when email not found")
    void testGetByEmailNotFound() {
        User user = new User();
        user.setEmail("existing@example.com");
        userRepo.add(user);

        User found = userRepo.get("nonexistent@example.com");
        assertNull(found);
    }

    @Test
    @DisplayName("Should update user")
    void testUpdateUser() {
        User user = new User();
        user.setName("Original Name");
        user.setEmail("original@example.com");
        userRepo.add(user);

        user.setName("Updated Name");
        user.setEmail("updated@example.com");
        userRepo.update(user);

        User found = userRepo.find(user.getId());
        assertNotNull(found);
        assertEquals("Updated Name", found.getName());
        assertEquals("updated@example.com", found.getEmail());
    }

    @Test
    @DisplayName("Should delete user by ID")
    void testDeleteUser() {
        User user = new User();
        user.setName("To Delete");
        user.setEmail("delete@example.com");
        userRepo.add(user);
        int userId = user.getId();

        userRepo.delete(userId);

        User found = userRepo.find(userId);
        assertNull(found);
    }

    @Test
    @DisplayName("Should delete non-existent user without error")
    void testDeleteNonExistentUser() {
        assertDoesNotThrow(() -> userRepo.delete(999));
    }

    @Test
    @DisplayName("Should get all users")
    void testGetAllUsers() {
        User user1 = new User();
        user1.setEmail("user1@example.com");
        User user2 = new User();
        user2.setEmail("user2@example.com");
        User user3 = new User();
        user3.setEmail("user3@example.com");

        userRepo.add(user1);
        userRepo.add(user2);
        userRepo.add(user3);

        List<User> all = userRepo.getAll();
        assertEquals(3, all.size());
    }

    @Test
    @DisplayName("Should return empty list when no users")
    void testGetAllEmpty() {
        List<User> all = userRepo.getAll();
        assertNotNull(all);
        assertTrue(all.isEmpty());
    }

    @Test
    @DisplayName("Should return new list instance in getAll")
    void testGetAllReturnsNewList() {
        User user = new User();
        user.setEmail("test@example.com");
        userRepo.add(user);

        List<User> list1 = userRepo.getAll();
        List<User> list2 = userRepo.getAll();

        assertNotSame(list1, list2);
        assertEquals(list1.size(), list2.size());
    }

    @Test
    @DisplayName("Should find user after multiple adds")
    void testFindAfterMultipleAdds() {
        User user1 = new User();
        user1.setEmail("first@example.com");
        User user2 = new User();
        user2.setEmail("second@example.com");
        User user3 = new User();
        user3.setEmail("third@example.com");

        userRepo.add(user1);
        userRepo.add(user2);
        userRepo.add(user3);

        User found = userRepo.find(2);
        assertNotNull(found);
        assertEquals("second@example.com", found.getEmail());
    }

    @Test
    @DisplayName("Should handle multiple operations")
    void testMultipleOperations() {
        User user1 = new User();
        user1.setName("User 1");
        user1.setEmail("user1@example.com");

        User user2 = new User();
        user2.setName("User 2");
        user2.setEmail("user2@example.com");

        User user3 = new User();
        user3.setName("User 3");
        user3.setEmail("user3@example.com");

        userRepo.add(user1);
        userRepo.add(user2);
        userRepo.add(user3);

        List<User> users = userRepo.getAll();
        assertEquals(3, users.size());
        assertTrue(users.stream().anyMatch(u -> u.getName().equals("User 1")));
        assertTrue(users.stream().anyMatch(u -> u.getName().equals("User 2")));
        assertTrue(users.stream().anyMatch(u -> u.getName().equals("User 3")));
    }

    @Test
    @DisplayName("Should handle adding user with null fields")
    void testAddUserWithNullFields() {
        //Arrange
        User user = new User();
        //All fields are null

        //Act & Assert
        assertDoesNotThrow(() -> userRepo.add(user));
        assertEquals(1, user.getId());
    }

    @Test
    @DisplayName("Should find first user when multiple users have same email")
    void testGetByEmailFindsFirstMatch() {
        //Arrange
        User user1 = new User();
        user1.setEmail("duplicate@example.com");
        user1.setName("First User");

        User user2 = new User();
        user2.setEmail("duplicate@example.com");
        user2.setName("Second User");

        userRepo.add(user1);
        userRepo.add(user2);

        //Act
        User found = userRepo.get("duplicate@example.com");

        //Assert
        assertNotNull(found);
        assertEquals("First User", found.getName());
    }
}
