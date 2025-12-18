package fr.uha.ensisa.gl.tarnished.services;

import fr.uha.ensisa.gl.entities.User;
import fr.uha.ensisa.gl.tarnished.repos.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepo userRepo;

    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userService = new UserService(userRepo);
    }

    @Test
    @DisplayName("Should successfully signup a new user")
    void testSignupNewUser() {
        //Arrange
        User newUser = new User(0, "John Doe", "john@example.com", "password123", new ArrayList<>());
        when(userRepo.get("john@example.com")).thenReturn(null);

        //Act
        String result = userService.signup(newUser);

        //Assert
        assertEquals("Signup successful!", result);
        verify(userRepo).get("john@example.com");
        verify(userRepo).add(newUser);
    }

    @Test
    @DisplayName("Should fail signup when email already exists")
    void testSignupExistingEmail() {
        //Arrange
        User existingUser = new User(1, "Jane Doe", "jane@example.com", "password123", new ArrayList<>());
        User newUser = new User(0, "John Doe", "jane@example.com", "differentpassword", new ArrayList<>());
        when(userRepo.get("jane@example.com")).thenReturn(existingUser);

        //Act
        String result = userService.signup(newUser);

        //Assert
        assertEquals("Error: Email already used", result);
        verify(userRepo).get("jane@example.com");
        verify(userRepo, never()).add(any());
    }

    @Test
    @DisplayName("Should handle null email")
    void testSignupNullEmail() {
        //Arrange
        User userWithNullEmail = new User(0, "Test User", null, "password", new ArrayList<>());
        when(userRepo.get(null)).thenReturn(null);

        //Act
        String result = userService.signup(userWithNullEmail);

        //Assert
        assertEquals("Signup successful!", result);
        verify(userRepo).get(null);
        verify(userRepo).add(userWithNullEmail);
    }

    @Test
    @DisplayName("Should handle empty email")
    void testSignupEmptyEmail() {
        //Arrange
        User userWithEmptyEmail = new User(0, "Test User", "", "password", new ArrayList<>());
        when(userRepo.get("")).thenReturn(null);

        //Act
        String result = userService.signup(userWithEmptyEmail);

        //Assert
        assertEquals("Signup successful!", result);
        verify(userRepo).get("");
        verify(userRepo).add(userWithEmptyEmail);
    }

    @Test
    @DisplayName("Should handle user with null name")
    void testSignupNullName() {
        //Arrange
        User user = new User(0, null, "test@example.com", "password", new ArrayList<>());
        when(userRepo.get("test@example.com")).thenReturn(null);

        //Act
        String result = userService.signup(user);

        //Assert
        assertEquals("Signup successful!", result);
        verify(userRepo).add(user);
    }

    @Test
    @DisplayName("Should handle user with null password")
    void testSignupNullPassword() {
        //Arrange
        User user = new User(0, "Test", "test@example.com", null, new ArrayList<>());
        when(userRepo.get("test@example.com")).thenReturn(null);

        //Act
        String result = userService.signup(user);

        //Assert
        assertEquals("Signup successful!", result);
        verify(userRepo).add(user);
    }

    @Test
    @DisplayName("Should handle user with null stories list")
    void testSignupNullStoriesList() {
        //Arrange
        User user = new User(0, "Test", "test@example.com", "password", null);
        when(userRepo.get("test@example.com")).thenReturn(null);

        //Act
        String result = userService.signup(user);

        //Assert
        assertEquals("Signup successful!", result);
        verify(userRepo).add(user);
    }
}
