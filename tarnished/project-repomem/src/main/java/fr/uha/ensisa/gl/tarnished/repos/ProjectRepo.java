package fr.uha.ensisa.gl.tarnished.repos;
import fr.uha.ensisa.gl.entities.Project;

import java.util.Collection;

public interface ProjectRepo {
    public void persist(Project column);
    public void remove(long id);
    public Project find(long id);
    public Collection<Project> findAll();
}
