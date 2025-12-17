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
        System.out.println("[REPO DEBUG] persist() called - Story ID: " + story.getId() + ", Title: " + story.getTitle() + ", UserAssigned: " + (story.getUserAssigned() != null ? story.getUserAssigned().getName() : "NULL"));
        if (story.getId() == 0) {
            //nouvelle story
            story.setId(nextId++);
            System.out.println("[REPO DEBUG] New story created with ID: " + story.getId());
        } else {
            System.out.println("[REPO DEBUG] Updating existing story with ID: " + story.getId());
        }
        //met à jour ou insère
        store.put((long)story.getId(), story);
        System.out.println("[REPO DEBUG] Story saved in store. Total stories in store: " + store.size());
        
        //vérifie que la story a été sauvegardée correctement
        Story savedStory = store.get((long)story.getId());
        System.out.println("[REPO DEBUG] Verification - Saved story UserAssigned: " + (savedStory != null && savedStory.getUserAssigned() != null ? savedStory.getUserAssigned().getName() : "NULL"));
    }

    @Override
    public void remove(long id) {
        store.remove(id);
    }

    @Override
    public Story find(long id) {
        Story story = store.get(id);
        System.out.println("[REPO DEBUG] find(" + id + ") called - Found: " + (story != null ? "ID=" + story.getId() + ", Title=" + story.getTitle() + ", UserAssigned=" + (story.getUserAssigned() != null ? story.getUserAssigned().getName() : "NULL") : "NULL"));
        return story;
    }

    @Override
    public Collection<Story> findAll() {
        return store.values();
    }

    @Override
    public Collection<Story> findByProject(long projectId) {
        return store.values().stream()
                .filter(story -> story.getProjectId() != null && story.getProjectId() == projectId)
                .toList();
    }

    @Override
    public Collection<Story> findByColumn(Long columnId) {
        if (columnId == null) {
            return store.values().stream()
                    .filter(story -> story.getColumnId() == null)
                    .sorted((s1, s2) -> Integer.compare(s1.getPosition(), s2.getPosition())) //tri par position
                    .toList();
        }
        return store.values().stream()
                .filter(story -> columnId.equals(story.getColumnId()))
                .sorted((s1, s2) -> Integer.compare(s1.getPosition(), s2.getPosition())) //tri par position
                .toList();
    }

    @Override
    public void moveToColumn(Long storyId, Long columnId) {
        Story story = find(storyId);
        if (story != null) {
            story.setColumnId(columnId);
        }
    }
    
    @Override
    public void updateStatus(Long storyId, fr.uha.ensisa.gl.entities.StoryStatus status) {
        Story story = find(storyId);
        if (story != null) {
            story.setStatus(status);
        }
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
