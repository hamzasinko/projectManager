package fr.uha.ensisa.gl.tarnished.repos;

public interface RepoFactory {
    public ColumnRepo getColumnRepo();
    public ProjectRepo getProjectRepo();
}
