package fr.uha.ensisa.gl.tarnished.projest.repo.mem;

import fr.uha.ensisa.gl.tarnished.projest.repo.RepoFactory;
import fr.uha.ensisa.gl.tarnished.projest.repo.IssueRepo;

public class RepoFactoryMem implements RepoFactory {
    public final IssueRepo issueRepo = new IssueRepoMem();

    public IssueRepo getIssueRepo() {
        return this.issueRepo;
    }
}
