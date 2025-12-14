package fr.uha.ensisa.gl.tarnished.mems;

import fr.uha.ensisa.gl.tarnished.repos.ColumnRepo;
import fr.uha.ensisa.gl.tarnished.repos.ProjectRepo;
import fr.uha.ensisa.gl.tarnished.repos.StoryRepo;
import fr.uha.ensisa.gl.tarnished.repos.RepoFactory;
import fr.uha.ensisa.gl.tarnished.repos.UserRepo;
import org.springframework.stereotype.Component;

@Component
public class RepoFactoryMem implements RepoFactory {
    final StoryRepo storyRepo = new StoryRepoMem();
    final ColumnRepo columnRepo = new ColumnRepoMem();
    final ProjectRepo projectRepo = new ProjectRepoMem();
    final UserRepo userRepo = new UserRepoMem();

    @Override
    public ColumnRepo getColumnRepo() {
        this.columnRepo.setStoryRepo(storyRepo);
        return this.columnRepo;
    }

    @Override
    public ProjectRepo getProjectRepo() {
        this.projectRepo.setColumnRepo(columnRepo);
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
