package fr.uha.ensisa.gl.tarnished.services;

import fr.uha.ensisa.gl.entities.User;
import fr.uha.ensisa.gl.tarnished.repos.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepo userRepo;

    @Autowired
    public UserService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public String signup(User user) {

        User existing = userRepo.get(user.getEmail());
        if (existing != null) {
            return "Error: Email already used";
        }


        userRepo.add(user);

        return "Signup successful!";
    }
}