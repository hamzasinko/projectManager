package fr.uha.ensisa.gl.tarnished.it;

import fr.uha.ensisa.gl.tarnished.controller.SignupController;
import fr.uha.ensisa.gl.tarnished.repos.UserRepo;
import fr.uha.ensisa.gl.tarnished.mems.UserRepoMem;
import fr.uha.ensisa.gl.tarnished.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SignupTest {

    private SignupController controller;

    @BeforeEach
    void setup() {
        UserRepo repo = new UserRepoMem();
        UserService service = new UserService(repo);
        controller = new SignupController(service);
    }

    @Test
    void testSignupSuccess() {
        String result = controller.signup("Sara", "sara@gmail.com", "1234", "1234");
        assertEquals("Signup successful!", result);
    }

    @Test
    void testEmailAlreadyUsed() {
        controller.signup("Sara", "sara@gmail.com", "1234", "1234");
        String result = controller.signup("Sara", "sara@gmail.com", "1234", "1234");
        assertEquals("Error: Email already used", result);
    }

    @Test
    void testPasswordsDoNotMatch() {
        String result = controller.signup("Bob", "bob@gmail.com", "1234", "0000");
        assertEquals("Error: passwords do not match", result);
    }
}