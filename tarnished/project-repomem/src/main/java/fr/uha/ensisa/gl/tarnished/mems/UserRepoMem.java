package fr.uha.ensisa.gl.tarnished.mems;
import fr.uha.ensisa.gl.entities.User;
import fr.uha.ensisa.gl.tarnished.repos.UserRepo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.ArrayList;

@Component
public class UserRepoMem implements UserRepo {

    private List<User> users = new ArrayList<>();
    private int nextId = 1;

    @Override
    public User find(int id) {
        return users.stream()
                .filter(u -> u.getId() == id)
                .findFirst()
                .orElse(null);
    }

    @Override
    public User get(String email) {
        return users.stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void add(User user) {
        user.setId(nextId++);
        users.add(user);
    }

    @Override
    public void update(User user) {
        users.removeIf(u -> u.getId() == user.getId());
        users.add(user);
    }

    @Override
    public void delete(int id) {
        users.removeIf(u -> u.getId() == id);
    }

    @Override
    public List<User> getAll() {
        return new ArrayList<>(users);
    }
}