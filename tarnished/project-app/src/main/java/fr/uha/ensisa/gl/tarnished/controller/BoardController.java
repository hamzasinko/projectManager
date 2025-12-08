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
            column.setStories(new java.util.ArrayList<>(stories));
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
                    // Assigner position 0 pour mettre en haut
                    story.setPosition(0);
                    
                    fr.uha.ensisa.gl.entities.StoryStatus newStatus = mapColumnNameToStatus(targetColumn.getName());
                    if (newStatus != null) {
                        story.setStatus(newStatus);
                        newStatusStr = newStatus.name();
                    }
                    // MUST persist to save position and status changes
                    repoFactory.getStoryRepo().persist(story);
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
            case "BACKLOG": return fr.uha.ensisa.gl.entities.StoryStatus.BACKLOG;
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
     * Réordonne les stories dans une colonne (AJAX)
     */
    @PostMapping("/{projectId}/reorder-stories")
    @ResponseBody
    public String reorderStories(
            @PathVariable Long projectId,
            @RequestParam Long columnId,
            @RequestParam String storyOrder) {
        
        try {
            String[] storyIds = storyOrder.split(",");
            for (int i = 0; i < storyIds.length; i++) {
                long storyId = Long.parseLong(storyIds[i]);
                Story story = repoFactory.getStoryRepo().find(storyId);
                if (story != null) {
                    story.setPosition(i);
                    repoFactory.getStoryRepo().persist(story);
                }
            }
            return "{\"success\":true}";
        } catch (Exception e) {
            return "{\"success\":false,\"error\":\"" + e.getMessage() + "\"}";
        }
    }
    
    /**
     * Supprime une colonne
     */
    @PostMapping("/{projectId}/delete-column/{columnId}")
    public String deleteColumn(
            @PathVariable Long projectId,
            @PathVariable Long columnId) {
        
        Column column = repoFactory.getColumnRepo().find(columnId);
        if (column == null) {
            return "redirect:/board/" + projectId + "?error=Column not found";
        }
        
        // Interdire la suppression de BACKLOG et DONE
        String columnName = column.getName().toUpperCase().replace(" ", "_");
        if ("BACKLOG".equals(columnName) || "DONE".equals(columnName)) {
            return "redirect:/board/" + projectId + "?error=Cannot delete " + column.getName() + " column";
        }
        
        // Vérifier si la colonne contient des stories
        Collection<Story> storiesInColumn = repoFactory.getStoryRepo().findByColumn(columnId);
        if (!storiesInColumn.isEmpty()) {
            return "redirect:/board/" + projectId + "?error=Cannot delete column with stories. Please move stories first";
        }
        
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

        // Ensure BACKLOG column is always position 1
        Long backlogId = null;
        for (fr.uha.ensisa.gl.entities.Column c : repoFactory.getColumnRepo().findByProject(projectId)) {
            if (c != null && c.getName() != null && "BACKLOG".equalsIgnoreCase(c.getName().trim())) {
                backlogId = (long) c.getId();
                break;
            }
        }

        int pos = 1;
        // If backlog exists, put it first
        if (backlogId != null) {
            repoFactory.getColumnRepo().reorder(backlogId, pos++);
        }

        // Then apply order for remaining columns in the payload, skipping backlog if present
        for (int i = 0; i < columnIds.length; i++) {
            Long columnId = Long.parseLong(columnIds[i]);
            if (backlogId != null && columnId.equals(backlogId)) continue;
            repoFactory.getColumnRepo().reorder(columnId, pos++);
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
            
            // Validation du nom
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
    
    /**
     * Met à jour une colonne (nom + capacité) (AJAX)
     */
    @PostMapping("/{projectId}/update-column-full")
    @ResponseBody
    public String updateColumn(
            @PathVariable Long projectId,
            @RequestParam Long columnId,
            @RequestParam(required = false) String newName,
            @RequestParam(required = false, defaultValue = "0") int maxCapacity) {
        
        try {
            Column column = repoFactory.getColumnRepo().find(columnId);
            if (column == null) {
                return "{\"success\":false,\"error\":\"Column not found\"}";
            }
            
            // Mettre à jour le nom si fourni
            if (newName != null && !newName.trim().isEmpty()) {
                column.setName(newName.trim());
            }
            
            // Mettre à jour la capacité
            column.setMaxCapacity(maxCapacity);
            repoFactory.getColumnRepo().persist(column);
            
            return "{\"success\":true}";
        } catch (Exception e) {
            return "{\"success\":false,\"error\":\"" + e.getMessage() + "\"}";
        }
    }
}
