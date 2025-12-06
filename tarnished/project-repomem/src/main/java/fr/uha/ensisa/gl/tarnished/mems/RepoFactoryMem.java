package fr.uha.ensisa.gl.tarnished.mems;

import fr.uha.ensisa.gl.tarnished.repos.ColumnRepo;
import fr.uha.ensisa.gl.tarnished.repos.ProjectRepo;
import fr.uha.ensisa.gl.tarnished.repos.StoryRepo;
import fr.uha.ensisa.gl.tarnished.repos.RepoFactory;
import fr.uha.ensisa.gl.tarnished.repos.UserRepo;
import org.springframework.stereotype.Component;

@Component
public class RepoFactoryMem implements RepoFactory {
    public final StoryRepo storyRepo = new StoryRepoMem();
    public final ColumnRepoMem columnRepoMem = new ColumnRepoMem();
    public final ColumnRepo columnRepo;
    public final ProjectRepo projectRepo = new ProjectRepoMem();
    public final UserRepo userRepo = new UserRepoMem();

    public RepoFactoryMem() {
        columnRepoMem.setStoryRepo(storyRepo);
        this.columnRepo = columnRepoMem;
    }

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

    @Override
    public UserRepo getUserRepo() {
        return this.userRepo;
    }
}
