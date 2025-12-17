package fr.uha.ensisa.gl.tarnished.mems;

import fr.uha.ensisa.gl.entities.Column;
import fr.uha.ensisa.gl.entities.Story;
import fr.uha.ensisa.gl.tarnished.repos.ColumnRepo;
import fr.uha.ensisa.gl.tarnished.repos.StoryRepo;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ColumnRepoMem implements ColumnRepo {
    private final Map<Long, Column> columns = new HashMap<>();
    private int nextId = 1;
    private StoryRepo storyRepo;

    @Override
    public void setStoryRepo(StoryRepo storyRepo) {
        this.storyRepo = storyRepo;
    }

    @Override
    public void persist(Column column) {
        if (column.getId() == 0) {
            column.setId(nextId++);
        }
        columns.put((long) column.getId(), column);
    }

    @Override
    public Column find(Long id) {
        return columns.get(id);
    }

    @Override
    public Collection<Column> findAll() {
        return new ArrayList<>(columns.values());
    }

    @Override
    public void remove(Long id) {
        columns.remove(id);
    }

    @Override
    public Collection<Column> findByProject(Long projectId) {
        return columns.values().stream()
                .filter(c -> c.getProject() != null && c.getProject().getId() == projectId)
                .sorted(Comparator.comparing(Column::getPosition))
                .collect(Collectors.toList());
    }

    @Override
    public void reorder(Long columnId, Integer newOrder) {
        Column column = find(columnId);
        if (column != null) {
            column.setPosition(newOrder);
        }
    }

    @Override
    public boolean isColumnFull(Long columnId) {
        Column column = find(columnId);
        if (column == null || column.getMaxCapacity() == 0) {
            return false;
        }
        int storyCount = column.getStories() != null ? column.getStories().size() : 0;
        return storyCount >= column.getMaxCapacity();
    }

    @Override
    public boolean canAcceptStory(Long columnId) {
        return !isColumnFull(columnId);
    }

    @Override
    public void addStoryToColumn(Long storyId, Long columnId) {
        Column column = find(columnId);
        if (column != null) {
            if (isColumnFull(columnId)) {
                throw new IllegalStateException("Column is full");
            }
            if (storyRepo != null) {
                storyRepo.moveToColumn(storyId, columnId);
            }
            //ne modifie pas column.stories - laisse BoardController le charger à nouveau
        }
    }

    @Override
    public void removeStoryFromColumn(Long storyId, Long columnId) {
        if (storyRepo != null) {
            storyRepo.moveToColumn(storyId, null);
        }
        //ne modifie pas column.stories - laisse BoardController le charger à nouveau
    }

    @Override
    public void moveStoryBetweenColumns(Long storyId, Long fromColumnId, Long toColumnId) {
        //vérifie la capacité de la colonne cible AVANT de déplacer
        if (toColumnId != null && storyRepo != null) {
            Column targetColumn = find(toColumnId);
            if (targetColumn != null && targetColumn.getMaxCapacity() > 0) {
                //compte les stories actuellement dans la colonne cible
                long currentCount = storyRepo.findByColumn(toColumnId).size();
                if (currentCount >= targetColumn.getMaxCapacity()) {
                    throw new IllegalStateException("Target column is full");
                }
            }
        }
        
        if (storyRepo != null) {
            storyRepo.moveToColumn(storyId, toColumnId);
        }
    }
}
