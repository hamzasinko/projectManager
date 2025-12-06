package fr.uha.ensisa.gl.tarnished.controller;

import fr.uha.ensisa.gl.entities.Column;
import fr.uha.ensisa.gl.entities.Project;
import fr.uha.ensisa.gl.entities.Story;
import fr.uha.ensisa.gl.tarnished.repos.RepoFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.Collection;
import java.util.List;

@Controller
@RequestMapping("/board")
public class BoardController {

    @Autowired
    private RepoFactory repoFactory;

    /**
     * Affiche le Kanban board d'un projet
     */
    @GetMapping("/{projectId}")
    public ModelAndView showBoard(@PathVariable Long projectId) {
        ModelAndView mav = new ModelAndView("board");

        Project project = repoFactory.getProjectRepo().find(projectId);
        if (project == null) {
            return new ModelAndView("redirect:/project/list");
        }

        // Get all columns for this project
        Collection<Column> columns = repoFactory.getColumnRepo().findByProject(projectId);
        
        // For each column, get its stories
        for (Column column : columns) {
            Collection<Story> stories = repoFactory.getStoryRepo().findByColumn((long) column.getId());
            column.setStories((List<Story>) stories);
        }

        mav.addObject("project", project);
        mav.addObject("columns", columns);

        return mav;
    }

    /**
     * Déplace une story vers une autre colonne (AJAX)
     */
    @PostMapping("/{projectId}/move-story")
    @ResponseBody
    public String moveStory(
            @PathVariable Long projectId,
            @RequestParam Long storyId,
            @RequestParam Long toColumnId,
            @RequestParam(required = false) Long fromColumnId) {
        
        try {
            // Check if target column is full before moving
            Column targetColumn = repoFactory.getColumnRepo().find(toColumnId);
            if (targetColumn != null && targetColumn.getMaxCapacity() > 0) {
                Collection<Story> storiesInColumn = repoFactory.getStoryRepo().findByColumn(toColumnId);
                if (storiesInColumn.size() >= targetColumn.getMaxCapacity()) {
                    return "{\"success\":false,\"error\":\"Column is full (max " + targetColumn.getMaxCapacity() + ")\"}";
                }
            }
            
            repoFactory.getColumnRepo().moveStoryBetweenColumns(storyId, fromColumnId, toColumnId);
            
            // Auto-update status if column is a default column
            String newStatusStr = null;
            if (targetColumn != null) {
                Story story = repoFactory.getStoryRepo().find(storyId);
                if (story != null) {
                    fr.uha.ensisa.gl.entities.StoryStatus newStatus = mapColumnNameToStatus(targetColumn.getName());
                    if (newStatus != null) {
                        story.setStatus(newStatus);
                        // No need to persist - in-memory objects are references
                        newStatusStr = newStatus.name();
                    }
                }
            }
            
            if (newStatusStr != null) {
                return "{\"success\":true,\"newStatus\":\"" + newStatusStr + "\"}";
            }
            return "{\"success\":true}";
        } catch (IllegalStateException e) {
            return "{\"success\":false,\"error\":\"" + e.getMessage() + "\"}";
        }
    }
    
    /**
     * Maps default column names to story statuses
     */
    private fr.uha.ensisa.gl.entities.StoryStatus mapColumnNameToStatus(String columnName) {
        if (columnName == null) return null;
        String normalized = columnName.toUpperCase().replace(" ", "_");
        switch (normalized) {
            case "TODO": return fr.uha.ensisa.gl.entities.StoryStatus.TODO;
            case "IN_PROGRESS": return fr.uha.ensisa.gl.entities.StoryStatus.IN_PROGRESS;
            case "REVIEW": return fr.uha.ensisa.gl.entities.StoryStatus.REVIEW;
            case "DONE": return fr.uha.ensisa.gl.entities.StoryStatus.DONE;
            case "BLOCKED": return fr.uha.ensisa.gl.entities.StoryStatus.BLOCKED;
            default: return null;
        }
    }

    /**
     * Crée une nouvelle colonne pour un projet
     */
    @PostMapping("/{projectId}/add-column")
    public String addColumn(
            @PathVariable Long projectId,
            @RequestParam String name,
            @RequestParam(required = false, defaultValue = "0") int maxCapacity) {

        Project project = repoFactory.getProjectRepo().find(projectId);
        if (project == null) {
            return "redirect:/project/list";
        }

        Column column = new Column();
        column.setName(name);
        column.setProject(project);
        column.setMaxCapacity(maxCapacity);
        
        // Set position as last
        Collection<Column> existingColumns = repoFactory.getColumnRepo().findByProject(projectId);
        column.setPosition(existingColumns.size() + 1);

        repoFactory.getColumnRepo().persist(column);

        return "redirect:/board/" + projectId;
    }
    
    /**
     * Supprime une colonne
     */
    @PostMapping("/{projectId}/delete-column/{columnId}")
    public String deleteColumn(
            @PathVariable Long projectId,
            @PathVariable Long columnId) {
        
        repoFactory.getColumnRepo().remove(columnId);
        return "redirect:/board/" + projectId;
    }
    
    /**
     * Réordonne les colonnes (AJAX)
     */
    @PostMapping("/{projectId}/reorder-columns")
    @ResponseBody
    public String reorderColumns(
            @PathVariable Long projectId,
            @RequestParam String columnOrder) {
        
        String[] columnIds = columnOrder.split(",");
        for (int i = 0; i < columnIds.length; i++) {
            Long columnId = Long.parseLong(columnIds[i]);
            repoFactory.getColumnRepo().reorder(columnId, i + 1);
        }
        
        return "{\"success\":true}";
    }
    
    /**
     * Met à jour le nom d'une colonne (AJAX)
     */
    @PostMapping("/{projectId}/update-column")
    @ResponseBody
    public String updateColumnName(
            @PathVariable Long projectId,
            @RequestParam Long columnId,
            @RequestParam String newName) {
        
        try {
            Column column = repoFactory.getColumnRepo().find(columnId);
            if (column == null) {
                return "{\"success\":false,\"error\":\"Column not found\"}";
            }
            
            if (newName == null || newName.trim().isEmpty()) {
                return "{\"success\":false,\"error\":\"Column name cannot be empty\"}";
            }
            
            column.setName(newName.trim());
            repoFactory.getColumnRepo().persist(column);
            
            return "{\"success\":true}";
        } catch (Exception e) {
            return "{\"success\":false,\"error\":\"" + e.getMessage() + "\"}";
        }
    }
}
