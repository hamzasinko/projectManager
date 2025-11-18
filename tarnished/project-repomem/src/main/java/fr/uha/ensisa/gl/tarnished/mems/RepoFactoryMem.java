package fr.uha.ensisa.gl.tarnished.mems;

import fr.uha.ensisa.gl.tarnished.repos.ColumnRepo;
import fr.uha.ensisa.gl.tarnished.repos.RepoFactory;

public class RepoFactoryMem implements RepoFactory {
    public final ColumnRepo columnRepo = new ColumnRepoMem();

    @Override
    public ColumnRepo getColumnRepo() {
        return this.columnRepo;
    }
}
