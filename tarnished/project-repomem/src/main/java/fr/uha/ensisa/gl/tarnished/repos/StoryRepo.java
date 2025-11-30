package fr.uha.ensisa.gl.tarnished.repos;

import fr.uha.ensisa.gl.entities.Story;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface StoryRepo {
    public void persist(Story story);
    public void remove(long id);
    public Story find(long id);
    public Collection<Story> findAll();
    public Collection<Story> findByProject(long projectId);
}
