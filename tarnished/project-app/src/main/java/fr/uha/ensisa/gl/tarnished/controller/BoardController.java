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
import java.util.Locale;

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
            // Initialiser hasSubColumns=true pour les colonnes par défaut (sauf BACKLOG et DONE)
            String columnName = column.getName().toUpperCase();
            if (!column.isHasSubColumns() && 
                (columnName.equals("IN PROGRESS") || 
                 columnName.equals("REVIEW") || 
                 columnName.equals("BLOCKED"))) {
                column.setHasSubColumns(true);
                repoFactory.getColumnRepo().persist(column);
            }
            
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
            @RequestParam(required = false) Long fromColumnId,
            @RequestParam(required = false) String newStatus,
            @RequestParam(required = false) String subColumn) {
        
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
            
            // Update position, subcolumn, and status based on target column
            String newStatusStr = null;
            Story story = repoFactory.getStoryRepo().find(storyId);
            if (story != null) {
                System.out.println("[DEBUG] moveStory - storyId: " + storyId + ", toColumnId: " + toColumnId + ", subColumn param: " + subColumn);
                System.out.println("[DEBUG] Before move - Story ID: " + story.getId() + ", SubColumn: " + story.getSubColumn() + ", Status: " + story.getStatus());
                
                // Assigner position 0 pour mettre en haut
                story.setPosition(0);
                
                // Save subcolumn if provided
                if (subColumn != null && !subColumn.isEmpty()) {
                    story.setSubColumn(subColumn);
                    System.out.println("[DEBUG] SubColumn set from parameter: " + subColumn);
                } else {
                    // Default to BACKLOG for custom columns
                    if (targetColumn != null && !"BACKLOG".equals(targetColumn.getName()) && !"DONE".equals(targetColumn.getName())) {
                        story.setSubColumn("BACKLOG");
                        System.out.println("[DEBUG] SubColumn defaulted to BACKLOG for custom column");
                    } else {
                        story.setSubColumn(null);
                        System.out.println("[DEBUG] SubColumn set to null for base column");
                    }
                }
                
                // ALWAYS update status based on target column name FOR DEFAULT COLUMNS
                // Default columns: BACKLOG, IN PROGRESS, REVIEW, DONE, BLOCKED
                // Custom columns and subcolumns don't change the status
                if (targetColumn != null) {
                    String columnName = targetColumn.getName();
                    fr.uha.ensisa.gl.entities.StoryStatus mappedStatus = mapColumnNameToStatus(columnName);
                    if (mappedStatus != null) {
                        // This is a default column - MUST update the status
                        story.setStatus(mappedStatus);
                        newStatusStr = mappedStatus.name();
                        System.out.println("[DEBUG] Status updated to: " + newStatusStr + " based on default column: " + columnName);
                    } else {
                        // This is a custom column - keep current status (subcolumns don't change status)
                        System.out.println("[DEBUG] Custom column detected, status unchanged: " + columnName);
                    }
                }
                
                // MUST persist to save position and status changes
                repoFactory.getStoryRepo().persist(story);
            }
            
            if (newStatusStr != null) {
                return "{\"success\":true,\"newStatus\":\"" + newStatusStr + "\"}";
            }
            return "{\"success\":true}";
        } catch (IllegalStateException e) {
            return "{\"success\":false,\"error\":\"" + e.getMessage() + "\"}";
        }
    }
    
    @PostMapping("/{projectId}/update-subcolumn")
    @ResponseBody
    public String updateSubColumn(
            @PathVariable Long projectId,
            @RequestParam Long storyId,
            @RequestParam String subColumn) {
        try {
            System.out.println("[DEBUG] updateSubColumn called - storyId: " + storyId + ", subColumn: " + subColumn);
            Story story = repoFactory.getStoryRepo().find(storyId);
            if (story == null) {
                System.out.println("[DEBUG] Story not found: " + storyId);
                return "{\"success\":false,\"error\":\"Story not found\"}";
            }
            
            System.out.println("[DEBUG] Before update - Story ID: " + story.getId() + ", SubColumn: " + story.getSubColumn());
            story.setSubColumn(subColumn);
            repoFactory.getStoryRepo().persist(story);
            System.out.println("[DEBUG] After update - Story ID: " + story.getId() + ", SubColumn: " + story.getSubColumn());
            
            return "{\"success\":true}";
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"success\":false,\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * Crée une nouvelle colonne pour un projet (GET - pour compatibilité avec les tests)
     */
    @GetMapping("/{projectId}/add-column")
    public String addColumnGet(
            @PathVariable Long projectId,
            @RequestParam String name,
            @RequestParam(required = false, defaultValue = "0") int maxCapacity,
            @RequestParam(required = false, defaultValue = "false") boolean hasSubColumns) {
        return addColumn(projectId, name, maxCapacity, hasSubColumns);
    }
    
    /**
     * Crée une nouvelle colonne pour un projet
     */
    @PostMapping("/{projectId}/add-column")
    public String addColumn(
            @PathVariable Long projectId,
            @RequestParam String name,
            @RequestParam(required = false, defaultValue = "0") int maxCapacity,
            @RequestParam(required = false, defaultValue = "false") boolean hasSubColumns) {

        Project project = repoFactory.getProjectRepo().find(projectId);
        if (project == null) {
            return "redirect:/project/list";
        }

        // Validation: limit name to 25 characters
        if (name != null && name.length() > 25) {
            name = name.substring(0, 25);
        }

        Column column = new Column();
        column.setName(name);
        column.setProject(project);
        column.setMaxCapacity(maxCapacity);
        column.setHasSubColumns(hasSubColumns);
        
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
        String columnName = column.getName().toUpperCase(Locale.ROOT).replace(" ", "_");
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
     * Supprimer une colonne avec toutes ses stories
     */
    @PostMapping("/{projectId}/delete-column-with-stories/{columnId}")
    @ResponseBody
    public String deleteColumnWithStories(
            @PathVariable Long projectId,
            @PathVariable Long columnId) {
        
        Column column = repoFactory.getColumnRepo().find(columnId);
        if (column == null) {
            return "error";
        }
        
        // Delete all stories in the column first
        Collection<Story> storiesInColumn = repoFactory.getStoryRepo().findByColumn(columnId);
        for (Story story : storiesInColumn) {
            repoFactory.getStoryRepo().remove(story.getId());
        }
        
        // Then delete the column
        repoFactory.getColumnRepo().remove(columnId);
        return "success";
    }
    
    /**
     * Déplacer toutes les stories d'une colonne vers une autre
     */
    @PostMapping("/{projectId}/move-all-stories")
    @ResponseBody
    public String moveAllStories(
            @PathVariable Long projectId,
            @RequestParam Long fromColumnId,
            @RequestParam Long toColumnId) {
        
        Column fromColumn = repoFactory.getColumnRepo().find(fromColumnId);
        Column toColumn = repoFactory.getColumnRepo().find(toColumnId);
        
        if (fromColumn == null || toColumn == null) {
            return "error";
        }
        
        Collection<Story> stories = repoFactory.getStoryRepo().findByColumn(fromColumnId);
        for (Story story : stories) {
            story.setColumnId(toColumnId);
            repoFactory.getStoryRepo().persist(story);
        }
        
        return "success";
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
     * Met à jour le nom d'une colonne (GET - pour compatibilité avec les tests)
     */
    @GetMapping("/{projectId}/update-column")
    public String updateColumnNameGet(
            @PathVariable Long projectId,
            @RequestParam Long columnId,
            @RequestParam String newName) {
        try {
            Column column = repoFactory.getColumnRepo().find(columnId);
            if (column == null) {
                return "redirect:/board/" + projectId + "?error=Column not found";
            }
            
            // Validation du nom
            if (newName == null || newName.trim().isEmpty()) {
                return "redirect:/board/" + projectId + "?error=Column name cannot be empty";
            }
            
            column.setName(newName.trim());
            repoFactory.getColumnRepo().persist(column);
        } catch (Exception e) {
            return "redirect:/board/" + projectId + "?error=" + e.getMessage();
        }
        return "redirect:/board/" + projectId;
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
    
    /**
     * Map column name to StoryStatus enum
     * Handles default columns: BACKLOG, IN PROGRESS (or IN_PROGRESS), REVIEW, DONE, BLOCKED
     */
    private fr.uha.ensisa.gl.entities.StoryStatus mapColumnNameToStatus(String columnName) {
        if (columnName == null) return null;
        // Normalize: uppercase and replace spaces with underscores, trim whitespace
        String normalized = columnName.toUpperCase(Locale.ROOT).trim().replace(" ", "_").replace("-", "_");
        
        // Direct matches
        switch (normalized) {
            case "BACKLOG": return fr.uha.ensisa.gl.entities.StoryStatus.BACKLOG;
            case "IN_PROGRESS": return fr.uha.ensisa.gl.entities.StoryStatus.IN_PROGRESS;
            case "REVIEW": return fr.uha.ensisa.gl.entities.StoryStatus.REVIEW;
            case "DONE": return fr.uha.ensisa.gl.entities.StoryStatus.DONE;
            case "BLOCKED": return fr.uha.ensisa.gl.entities.StoryStatus.BLOCKED;
            default: 
                // Additional checks for variations
                if (normalized.contains("BACKLOG")) return fr.uha.ensisa.gl.entities.StoryStatus.BACKLOG;
                if (normalized.contains("IN_PROGRESS") || normalized.contains("INPROGRESS")) return fr.uha.ensisa.gl.entities.StoryStatus.IN_PROGRESS;
                if (normalized.contains("REVIEW")) return fr.uha.ensisa.gl.entities.StoryStatus.REVIEW;
                if (normalized.contains("DONE")) return fr.uha.ensisa.gl.entities.StoryStatus.DONE;
                if (normalized.contains("BLOCKED")) return fr.uha.ensisa.gl.entities.StoryStatus.BLOCKED;
                return null;
        }
    }
}
