package fr.uha.ensisa.gl.tarnished.mems;

import fr.uha.ensisa.gl.entities.Project;
import fr.uha.ensisa.gl.tarnished.repos.ProjectRepo;

import java.util.*;

public class ProjectRepoMem implements ProjectRepo {
    private final Map<Long, Project> store =
            Collections.synchronizedMap(new TreeMap<Long, Project>());
    private int nextId = 1;

    @Override
    public void persist(Project project) {
        project.setId(nextId++);
        store.put((long)project.getId(), project);
    }

    @Override
    public void remove(long id) {
        store.remove(id);
    }

    @Override
    public Project find(long id) {
        return store.get(id);
    }

    @Override
    public Collection<Project> findAll() {
        return store.values();
    }

    public long count() {
        return store.size();
    }
}
