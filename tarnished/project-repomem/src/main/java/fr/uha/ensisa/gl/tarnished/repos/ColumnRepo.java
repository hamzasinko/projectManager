package fr.uha.ensisa.gl.tarnished.repos;

import fr.uha.ensisa.gl.entities.Column;

import java.util.Collection;

public interface ColumnRepo {
    public void persist(Column column);
    public void remove(long id);
    public Column find(long id);
    public Collection<Column> findAll();
}
