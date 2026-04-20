package fr.uha.ensisa.gl.tarnished.repos;

import fr.uha.ensisa.gl.entities.Story;
import fr.uha.ensisa.gl.entities.WorkLog;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface StoryRepo {
    public void persist(Story story);
    public void remove(long id);
    public Story find(long id);
    public Collection<Story> findAll();
    public Collection<Story> findByProject(long projectId);
    Collection<Story> findByColumn(Long columnId);
    void moveToColumn(Long storyId, Long columnId);
    void updateStatus(Long storyId, fr.uha.ensisa.gl.entities.StoryStatus status);
    void addWorkLog(Long storyId, WorkLog workLog);
    void removeWorkLog(Long storyId, Long workLogId);
    Long calculateTotalTime(Long storyId);
    WorkLog startTimer(Long storyId, Long userId);
    WorkLog stopTimer(Long storyId, Long workLogId);
}
