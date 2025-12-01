package fr.uha.ensisa.gl.tarnished.mems;

import fr.uha.ensisa.gl.entities.Story;
import fr.uha.ensisa.gl.tarnished.repos.StoryRepo;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class StoryRepoMem implements StoryRepo {
    private final Map<Long, Story> store =
            Collections.synchronizedMap(new TreeMap<Long, Story>());
    private int nextId = 1;

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

    public long count() {
        return store.size();
    }
}
