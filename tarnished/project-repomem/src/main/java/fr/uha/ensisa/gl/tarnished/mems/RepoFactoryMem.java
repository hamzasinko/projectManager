package fr.uha.ensisa.gl.tarnished.mems;

import fr.uha.ensisa.gl.tarnished.repos.*;
import org.springframework.stereotype.Component;

@Component
public class RepoFactoryMem implements RepoFactory {
    final StoryRepo storyRepo = new StoryRepoMem();
    final ColumnRepo columnRepo = new ColumnRepoMem();
    final ProjectRepo projectRepo = new ProjectRepoMem();
    final UserRepo userRepo = new UserRepoMem();
    final SwimlaneRepo swimRepo = new SwimlaneRepoMem();

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
    public SwimlaneRepo getSwimlaneRepo() {
        return this.swimRepo;
    }

    @Override
    public UserRepo getUserRepo() {
        return this.userRepo;
    }
}
