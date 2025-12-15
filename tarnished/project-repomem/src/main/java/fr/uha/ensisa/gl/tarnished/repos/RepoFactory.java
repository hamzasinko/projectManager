package fr.uha.ensisa.gl.tarnished.repos;

import org.springframework.stereotype.Repository;

@Repository
public interface RepoFactory {
    public ColumnRepo getColumnRepo();
    public ProjectRepo getProjectRepo();
    public UserRepo getUserRepo();
    public StoryRepo getStoryRepo();
    public SwimlaneRepo getSwimlaneRepo();
}
