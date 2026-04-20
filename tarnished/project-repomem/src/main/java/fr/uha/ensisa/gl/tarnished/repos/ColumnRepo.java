package fr.uha.ensisa.gl.tarnished.repos;

import fr.uha.ensisa.gl.entities.Column;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface ColumnRepo {
    void persist(Column column);
    Column find(Long id);
    Collection<Column> findAll();
    void remove(Long id);
    Collection<Column> findByProject(Long projectId);
    void reorder(Long columnId, Integer newOrder);
    boolean isColumnFull(Long columnId);
    boolean canAcceptStory(Long columnId);
    void addStoryToColumn(Long storyId, Long columnId);
    void removeStoryFromColumn(Long storyId, Long columnId);
    void moveStoryBetweenColumns(Long storyId, Long fromColumnId, Long toColumnId);
    void setStoryRepo(StoryRepo storyRepo);
}
