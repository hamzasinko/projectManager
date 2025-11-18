package fr.uha.ensisa.gl.tarnished.mems;

import fr.uha.ensisa.gl.entities.Column;
import fr.uha.ensisa.gl.tarnished.repos.ColumnRepo;

import java.util.*;

public class ColumnRepoMem implements ColumnRepo {
    private final Map<Long, Column> store =
            Collections.synchronizedMap(new TreeMap<Long, Column>());

    @Override
    public void persist(Column column) {
        store.put((long)column.getId(), column);
    }

    @Override
    public void remove(long id) {
        store.remove(id);
    }

    @Override
    public Column find(long id) {
        return store.get(id);
    }

    @Override
    public Collection<Column> findAll() {
        return store.values();
    }

    public long count() {
        return store.size();
    }
}
