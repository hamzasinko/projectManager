package fr.uha.ensisa.gl.tarnished.mems;

import fr.uha.ensisa.gl.entities.Column;
import fr.uha.ensisa.gl.entities.Project;
import fr.uha.ensisa.gl.tarnished.repos.ColumnRepo;
import fr.uha.ensisa.gl.tarnished.repos.ProjectRepo;
import fr.uha.ensisa.gl.tarnished.repos.RepoFactory;
import fr.uha.ensisa.gl.tarnished.repos.StoryRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ProjectRepoMem implements ProjectRepo {
    private final Map<Long, Project> store =
            Collections.synchronizedMap(new TreeMap<Long, Project>());
    private int nextId = 1;

    private ColumnRepo columnRepo;

    @Override
    public void setColumnRepo(ColumnRepo columnRepo) {
        this.columnRepo = columnRepo;
    }

    @Override
    public void persist(Project project) {
        project.setId(nextId++);
        project.setDateStarted(Calendar.getInstance().getTime());
        store.put((long)project.getId(), project);
        
        //on crée les colonnes par défaut
        createDefaultColumns(project);
    }
    
    private void createDefaultColumns(Project project) {
        String[] defaultColumns = {"BACKLOG", "IN PROGRESS", "REVIEW", "DONE", "BLOCKED"};
        
        for (int i = 0; i < defaultColumns.length; i++) {
            Column column = new Column();
            column.setName(defaultColumns[i]);
            column.setProject(project);
            column.setPosition(i + 1);
            column.setMaxCapacity(0); //pas de limite par défaut
            
            //les colonnes par défaut sauf BACKLOG, DONE et BLOCKED ont des sous-colonnes
            String name = defaultColumns[i];
            column.setHasSubColumns(!name.equals("BACKLOG") && !name.equals("DONE") && !name.equals("BLOCKED"));
            
            columnRepo.persist(column);
        }
    }

    @Override
    public void update(Project project) {
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
