package fr.uha.ensisa.gl.tarnished.repos;

import fr.uha.ensisa.gl.entities.User;
import java.util.List;

public interface UserRepo {

    User find(int id);
    User get(String email);
    void add(User user);
    void update(User user);
    void delete(int id);
    List<User> getAll();
}