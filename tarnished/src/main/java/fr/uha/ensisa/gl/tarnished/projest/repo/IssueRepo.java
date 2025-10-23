package fr.uha.ensisa.gl.tarnished.projest.repo;

import java.util.Collection;
import fr.uha.ensisa.gl.tarnished.projest.Issue;

public interface IssueRepo {
    void persist(Issue issue);
    void remove(long id);
    Issue find(long id);
    Collection<Issue> findAll();
}
