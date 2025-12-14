package fr.uha.ensisa.gl.tarnished.mems;

import fr.uha.ensisa.gl.tarnished.repos.ColumnRepo;
import fr.uha.ensisa.gl.tarnished.repos.ProjectRepo;
import fr.uha.ensisa.gl.tarnished.repos.StoryRepo;
import fr.uha.ensisa.gl.tarnished.repos.RepoFactory;
import fr.uha.ensisa.gl.tarnished.repos.UserRepo;
import fr.uha.ensisa.gl.tarnished.repos.SwimlaneRepo;
import org.springframework.stereotype.Component;

@Component
public class RepoFactoryMem implements RepoFactory {
    public final StoryRepo storyRepo = new StoryRepoMem();
    public final ColumnRepo columnRepo = new ColumnRepoMem();
    public final ProjectRepo projectRepo = new ProjectRepoMem();
    public final UserRepo userRepo = new UserRepoMem();
    public final SwimlaneRepo swimlaneRepo = new SwimlaneRepoMem();

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

    @Override
    public SwimlaneRepo getSwimlaneRepo() {
        return this.swimlaneRepo;
    }
}
