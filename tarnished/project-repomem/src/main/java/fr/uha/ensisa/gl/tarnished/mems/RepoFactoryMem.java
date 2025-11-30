package fr.uha.ensisa.gl.tarnished.mems;

import fr.uha.ensisa.gl.tarnished.repos.ColumnRepo;
import fr.uha.ensisa.gl.tarnished.repos.ProjectRepo;
import fr.uha.ensisa.gl.tarnished.repos.StoryRepo;
import fr.uha.ensisa.gl.tarnished.repos.RepoFactory;
import org.springframework.stereotype.Component;

@Component
public class RepoFactoryMem implements RepoFactory {
    public final ColumnRepo columnRepo = new ColumnRepoMem();
    public final ProjectRepo projectRepo = new ProjectRepoMem();
    public final StoryRepo storyRepo = new StoryRepoMem();

    @Override
    public ColumnRepo getColumnRepo() {
        return this.columnRepo;
    }

    @Override
    public ProjectRepo getProjectRepo() {
        return this.projectRepo;
    }

    @Override
    public StoryRepo getStoryRepo() {
        return this.storyRepo;
    }
}
