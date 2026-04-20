package fr.uha.ensisa.gl.tarnished.controller;

import fr.uha.ensisa.gl.entities.User;
import fr.uha.ensisa.gl.tarnished.services.UserService;

import java.util.Objects;

public class SignupController {

    private UserService userService;

    public SignupController(UserService userService) {
        this.userService = userService;
    }

    public String signup(String name, String email, String password, String confirmPassword) {


        if (!Objects.equals(password, confirmPassword)) {
            return "Error: passwords do not match";
        }


        User newUser = new User();
        newUser.setName(name);
        newUser.setEmail(email);
        newUser.setPassword(password);


        return userService.signup(newUser);
    }
}