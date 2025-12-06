package fr.uha.ensisa.gl.tarnished.mems;

import fr.uha.ensisa.gl.entities.Story;
import fr.uha.ensisa.gl.entities.WorkLog;
import fr.uha.ensisa.gl.tarnished.repos.StoryRepo;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

@Component
public class StoryRepoMem implements StoryRepo {
    private final Map<Long, Story> store =
            Collections.synchronizedMap(new TreeMap<Long, Story>());
    private int nextId = 1;
    private long nextWorkLogId = 1L;

    @Override
    public void persist(Story story) {
        story.setId(nextId++);
        store.put((long)story.getId(), story);
    }

    @Override
    public void remove(long id) {
        store.remove(id);
    }

    @Override
    public Story find(long id) {
        return store.get(id);
    }

    @Override
    public Collection<Story> findAll() {
        return store.values();
    }

    @Override
    public Collection<Story> findByProject(long projectId) {
        // Note: Story doesn't have projectId in current model
        // This method will return empty collection for now
        // Should be implemented when Story model is updated with project reference
        return new ArrayList<>();
    }

    @Override
    public void addWorkLog(Long storyId, WorkLog workLog) {
        Story story = find(storyId);
        if (story != null) {
            if (story.getWorkLogs() == null) {
                story.setWorkLogs(new ArrayList<>());
            }
            story.getWorkLogs().add(workLog);
        }
    }

    @Override
    public void removeWorkLog(Long storyId, Long workLogId) {
        Story story = find(storyId);
        if (story != null && story.getWorkLogs() != null) {
            story.getWorkLogs().removeIf(wl -> wl.getId() == workLogId);
        }
    }

    @Override
    public Long calculateTotalTime(Long storyId) {
        Story story = find(storyId);
        if (story == null || story.getWorkLogs() == null) {
            return 0L;
        }
        return story.getWorkLogs().stream()
                .mapToLong(WorkLog::getDuration)
                .sum();
    }

    @Override
    public WorkLog startTimer(Long storyId, Long userId) {
        Story story = find(storyId);
        if (story == null) {
            return null;
        }
        
        WorkLog workLog = new WorkLog(nextWorkLogId++, LocalDateTime.now(), userId, storyId);
        addWorkLog(storyId, workLog);
        return workLog;
    }

    @Override
    public WorkLog stopTimer(Long storyId, Long workLogId) {
        Story story = find(storyId);
        if (story == null || story.getWorkLogs() == null) {
            return null;
        }
        
        WorkLog workLog = story.getWorkLogs().stream()
                .filter(wl -> wl.getId() == workLogId)
                .findFirst()
                .orElse(null);
        
        if (workLog != null) {
            workLog.stopTimer();
        }
        
        return workLog;
    }

    public long count() {
        return store.size();
    }
}
