package fr.uha.ensisa.gl.tarnished.mems;

import fr.uha.ensisa.gl.entities.Swimlane;
import fr.uha.ensisa.gl.tarnished.repos.SwimlaneRepo;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component
public class SwimlaneRepoMem implements SwimlaneRepo {
    private final Map<Long, Swimlane> store = Collections.synchronizedMap(new TreeMap<>());
    private final AtomicLong sequence = new AtomicLong(1);

    @Override
    public void persist(Swimlane swimlane) {
        if (swimlane.getId() == 0) {
            long id = sequence.getAndIncrement();
            swimlane.setId(id);
        }
        store.put(swimlane.getId(), swimlane);
    }

    @Override
    public void remove(long id) {
        store.remove(id);
    }

    @Override
    public Swimlane find(long id) {
        return store.get(id);
    }

    @Override
    public Collection<Swimlane> findAll() {
        return Collections.unmodifiableCollection(store.values());
    }

    @Override
    public Collection<Swimlane> findByProject(int projectId) {
        return store.values().stream()
                .filter(s -> s.getProjectId() == projectId)
                .collect(Collectors.toList());
    }

    @Override
    public long getNextId() {
        return sequence.get();
    }
}
