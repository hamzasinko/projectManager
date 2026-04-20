package fr.uha.ensisa.gl.tarnished.controller;

import fr.uha.ensisa.gl.entities.User;
import fr.uha.ensisa.gl.tarnished.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("SignupController Tests")
class SignupControllerTest {

    @Mock
    private UserService userService;

    private SignupController signupController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        signupController = new SignupController(userService);
    }

    @Test
    @DisplayName("Should signup user successfully when passwords match")
    void testSignupSuccess() {
        //Arrange
        String name = "John Doe";
        String email = "john@example.com";
        String password = "password123";
        String confirmPassword = "password123";

        when(userService.signup(any(User.class))).thenReturn("Signup successful");

        //Act
        String result = signupController.signup(name, email, password, confirmPassword);

        //Assert
        assertEquals("Signup successful", result);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).signup(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertEquals(name, capturedUser.getName());
        assertEquals(email, capturedUser.getEmail());
        assertEquals(password, capturedUser.getPassword());
    }

    @Test
    @DisplayName("Should return error when passwords do not match")
    void testSignupPasswordMismatch() {
        //Arrange
        String name = "John Doe";
        String email = "john@example.com";
        String password = "password123";
        String confirmPassword = "password456";

        //Act
        String result = signupController.signup(name, email, password, confirmPassword);

        //Assert
        assertEquals("Error: passwords do not match", result);
        verify(userService, never()).signup(any(User.class));
    }

    @Test
    @DisplayName("Should handle empty password")
    void testSignupEmptyPassword() {
        //Arrange
        String name = "John Doe";
        String email = "john@example.com";
        String password = "";
        String confirmPassword = "";

        when(userService.signup(any(User.class))).thenReturn("Signup successful");

        //Act
        String result = signupController.signup(name, email, password, confirmPassword);

        //Assert
        assertEquals("Signup successful", result);
        verify(userService).signup(any(User.class));
    }

    @Test
    @DisplayName("Should handle null password")
    void testSignupNullPassword() {
        //Arrange
        String name = "John Doe";
        String email = "john@example.com";
        String password = null;
        String confirmPassword = null;

        when(userService.signup(any(User.class))).thenReturn("Signup successful");

        //Act
        String result = signupController.signup(name, email, password, confirmPassword);

        //Assert
        assertEquals("Signup successful", result);
        verify(userService).signup(any(User.class));
    }

    @Test
    @DisplayName("Should handle one null one non-null password")
    void testSignupOneNullPassword() {
        //Arrange
        String name = "John Doe";
        String email = "john@example.com";
        String password = "password123";
        String confirmPassword = null;

        //Act
        String result = signupController.signup(name, email, password, confirmPassword);

        //Assert
        assertEquals("Error: passwords do not match", result);
        verify(userService, never()).signup(any(User.class));
    }

    @Test
    @DisplayName("Should handle empty name")
    void testSignupEmptyName() {
        //Arrange
        String name = "";
        String email = "john@example.com";
        String password = "password123";
        String confirmPassword = "password123";

        when(userService.signup(any(User.class))).thenReturn("Signup successful");

        //Act
        String result = signupController.signup(name, email, password, confirmPassword);

        //Assert
        assertEquals("Signup successful", result);
        
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).signup(userCaptor.capture());
        assertEquals("", userCaptor.getValue().getName());
    }

    @Test
    @DisplayName("Should handle empty email")
    void testSignupEmptyEmail() {
        //Arrange
        String name = "John Doe";
        String email = "";
        String password = "password123";
        String confirmPassword = "password123";

        when(userService.signup(any(User.class))).thenReturn("Signup successful");

        //Act
        String result = signupController.signup(name, email, password, confirmPassword);

        //Assert
        assertEquals("Signup successful", result);
        
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).signup(userCaptor.capture());
        assertEquals("", userCaptor.getValue().getEmail());
    }

    @Test
    @DisplayName("Should handle null name")
    void testSignupNullName() {
        //Arrange
        String name = null;
        String email = "john@example.com";
        String password = "password123";
        String confirmPassword = "password123";

        when(userService.signup(any(User.class))).thenReturn("Signup successful");

        //Act
        String result = signupController.signup(name, email, password, confirmPassword);

        //Assert
        assertEquals("Signup successful", result);
        
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).signup(userCaptor.capture());
        assertNull(userCaptor.getValue().getName());
    }

    @Test
    @DisplayName("Should handle null email")
    void testSignupNullEmail() {
        //Arrange
        String name = "John Doe";
        String email = null;
        String password = "password123";
        String confirmPassword = "password123";

        when(userService.signup(any(User.class))).thenReturn("Signup successful");

        //Act
        String result = signupController.signup(name, email, password, confirmPassword);

        //Assert
        assertEquals("Signup successful", result);
        
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).signup(userCaptor.capture());
        assertNull(userCaptor.getValue().getEmail());
    }

    @Test
    @DisplayName("Should handle UserService returning error")
    void testSignupServiceError() {
        //Arrange
        String name = "John Doe";
        String email = "existing@example.com";
        String password = "password123";
        String confirmPassword = "password123";

        when(userService.signup(any(User.class))).thenReturn("Error: Email already exists");

        //Act
        String result = signupController.signup(name, email, password, confirmPassword);

        //Assert
        assertEquals("Error: Email already exists", result);
        verify(userService).signup(any(User.class));
    }

    @Test
    @DisplayName("Should handle whitespace in passwords")
    void testSignupWhitespacePasswords() {
        //Arrange
        String name = "John Doe";
        String email = "john@example.com";
        String password = "  pass word  ";
        String confirmPassword = "  pass word  ";

        when(userService.signup(any(User.class))).thenReturn("Signup successful");

        //Act
        String result = signupController.signup(name, email, password, confirmPassword);

        //Assert
        assertEquals("Signup successful", result);
        verify(userService).signup(any(User.class));
    }

    @Test
    @DisplayName("Should handle special characters in all fields")
    void testSignupSpecialCharacters() {
        //Arrange
        String name = "Jean-François O'Brien";
        String email = "user+test@example.co.uk";
        String password = "P@ssw0rd!#$%";
        String confirmPassword = "P@ssw0rd!#$%";

        when(userService.signup(any(User.class))).thenReturn("Signup successful");

        //Act
        String result = signupController.signup(name, email, password, confirmPassword);

        //Assert
        assertEquals("Signup successful", result);
        
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).signup(userCaptor.capture());
        
        User capturedUser = userCaptor.getValue();
        assertEquals(name, capturedUser.getName());
        assertEquals(email, capturedUser.getEmail());
        assertEquals(password, capturedUser.getPassword());
    }

    @Test
    @DisplayName("Should create new User object for each signup")
    void testSignupCreatesNewUser() {
        //Arrange
        when(userService.signup(any(User.class))).thenReturn("Signup successful");

        //Act
        signupController.signup("User1", "user1@example.com", "pass1", "pass1");
        signupController.signup("User2", "user2@example.com", "pass2", "pass2");

        //Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService, times(2)).signup(userCaptor.capture());

        //Verify two different User objects were created
        assertEquals(2, userCaptor.getAllValues().size());
        assertNotSame(userCaptor.getAllValues().get(0), userCaptor.getAllValues().get(1));
    }
}
