package fr.uha.ensisa.gl.tarnished.repos;

import fr.uha.ensisa.gl.entities.User;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepo {

    User find(int id);
    User get(String email);
    void add(User user);
    void update(User user);
    void delete(int id);
    List<User> getAll();
}