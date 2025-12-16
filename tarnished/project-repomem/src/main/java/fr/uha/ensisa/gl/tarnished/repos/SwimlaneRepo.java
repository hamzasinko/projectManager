package fr.uha.ensisa.gl.tarnished.repos;

import fr.uha.ensisa.gl.entities.Swimlane;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface SwimlaneRepo {
    void persist(Swimlane swimlane);
    void remove(long id);
    Swimlane find(long id);
    Collection<Swimlane> findAll();
    Collection<Swimlane> findByProject(int projectId);
    long getNextId();
}
