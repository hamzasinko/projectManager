package fr.uha.ensisa.gl.tarnished.controller;

import fr.uha.ensisa.gl.entities.Story;
import fr.uha.ensisa.gl.entities.StoryStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import fr.uha.ensisa.gl.tarnished.repos.RepoFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;

@Controller
@RequestMapping("/story")
public class StoryController {
    
    @Autowired
    public RepoFactory repoFactory;
    
    public StoryController() {
        System.out.println("*** StoryController CREATED ***");
    }
    
    /**
     * Affiche le formulaire de création de story
     */
    @GetMapping("/new")
    public ModelAndView showCreateForm(
        @RequestParam(required=false) Long projectId,
        @RequestParam(required=false) Long columnId
    ) {
        ModelAndView mav = new ModelAndView("story-create");
        // Add all projects to select from
        mav.addObject("projects", repoFactory.getProjectRepo().findAll());
        // Pass projectId and columnId if provided
        if (projectId != null) {
            mav.addObject("projectId", projectId);
        }
        if (columnId != null) {
            mav.addObject("columnId", columnId);
        }
        return mav;
    }
    
    /**
     * Traite la création d'une nouvelle story
     */
    @PostMapping("/create")
    public String createStory(
        @RequestParam(required=true) String title,
        @RequestParam(required=false) String description,
        @RequestParam(required=true) Long projectId,
        @RequestParam(required=false) Long columnId
    ) throws IOException {
        
        System.out.println("[DEBUG] createStory called - projectId: " + projectId + ", columnId: " + columnId);
        
        // Validate title
        if (title == null || title.trim().isEmpty()) {
            return "redirect:/story/new?error=Title is required&projectId=" + projectId;
        }
        
        // Limiter la longueur du titre à 59 caractères
        if (title.length() > 59) {
            return "redirect:/story/new?error=Title must be less than 59 characters&projectId=" + projectId;
        }
        
        // Validation : projectId est obligatoire
        if (projectId == null) {
            return "redirect:/story/new?error=Project is required";
        }
        
        Story story = new Story();
        story.setTitle(title.trim());
        story.setDescription(description);
        story.setDateCreated(new Date());
        story.setProjectId(projectId);
        
        // Définir le status et columnId en fonction de la colonne
        StoryStatus initialStatus = StoryStatus.BACKLOG;
        if (columnId != null) {
            // Assigner la story à la colonne spécifiée
            story.setColumnId(columnId);
            fr.uha.ensisa.gl.entities.Column column = repoFactory.getColumnRepo().find(columnId);
            if (column != null) {
                StoryStatus columnStatus = mapColumnNameToStatus(column.getName());
                if (columnStatus != null) {
                    initialStatus = columnStatus;
                }
            }
        } else {
            // Si pas de colonne spécifiée, trouver la colonne BACKLOG
            Collection<fr.uha.ensisa.gl.entities.Column> columns = repoFactory.getColumnRepo().findByProject(projectId);
            for (fr.uha.ensisa.gl.entities.Column col : columns) {
                if ("BACKLOG".equalsIgnoreCase(col.getName())) {
                    story.setColumnId((long) col.getId());
                    break;
                }
            }
        }
        story.setStatus(initialStatus);
        
        // Décaler toutes les stories existantes de cette colonne (position + 1)
        if (story.getColumnId() != null) {
            Collection<Story> storiesInColumn = repoFactory.getStoryRepo().findByColumn(story.getColumnId());
            for (Story existingStory : storiesInColumn) {
                existingStory.setPosition(existingStory.getPosition() + 1);
            }
        }
        
        // Assigner position 0 pour que la nouvelle story apparaisse EN HAUT
        story.setPosition(0);
        
        repoFactory.getStoryRepo().persist(story);
        
        System.out.println("[DEBUG] Story created - ID: " + story.getId() + ", ProjectID: " + story.getProjectId() + ", ColumnID: " + story.getColumnId() + ", Position: " + story.getPosition());
        System.out.println("[DEBUG] Redirecting to: /board/" + story.getProjectId());
        
        // Rediriger vers le board du projet où la story a été ajoutée
        return "redirect:/board/" + story.getProjectId();
    }
    
    /**
     * Liste toutes les stories existantes
     */
    @GetMapping("/list")
    public ModelAndView listStories() throws IOException {
        ModelAndView mav = new ModelAndView("story-list");
        mav.addObject("stories", repoFactory.getStoryRepo().findAll());
        mav.addObject("columns", repoFactory.getColumnRepo().findAll());
        return mav;
    }
    
    /**
     * Affiche les détails d'une story
     */
    @GetMapping("/{id}")
    public ModelAndView showStory(@PathVariable("id") Long id) throws IOException {
        ModelAndView mav = new ModelAndView("story-detail");
        Story story = repoFactory.getStoryRepo().find(id);
        
        if (story == null) {
            return new ModelAndView("redirect:/story/list");
        }
        
        mav.addObject("story", story);
        return mav;
    }
    
    /**
     * Affiche le formulaire d'édition d'une story
     */
    @GetMapping("/{id}/edit")
    public ModelAndView editStory(@PathVariable("id") Long id) {
        ModelAndView mav = new ModelAndView("story-edit");
        Story story = repoFactory.getStoryRepo().find(id);
        
        if (story == null) {
            return new ModelAndView("redirect:/story/list");
        }
        
        mav.addObject("story", story);
        mav.addObject("users", repoFactory.getUserRepo().getAll());
        
        // Check if story is in a default column
        if (story.getColumnId() != null) {
            fr.uha.ensisa.gl.entities.Column column = repoFactory.getColumnRepo().find(story.getColumnId());
            if (column != null) {
                mav.addObject("column", column);
                mav.addObject("isDefaultColumn", isDefaultColumn(column.getName()));
            }
        }
        
        return mav;
    }
    
    private boolean isDefaultColumn(String columnName) {
        if (columnName == null) return false;
        String normalized = columnName.toUpperCase().replace(" ", "_");
        return normalized.equals("BACKLOG") || normalized.equals("IN_PROGRESS") || 
               normalized.equals("REVIEW") || normalized.equals("DONE") || normalized.equals("BLOCKED");
    }
    
    private StoryStatus mapColumnNameToStatus(String columnName) {
        if (columnName == null) return null;
        String normalized = columnName.toUpperCase().replace(" ", "_");
        switch (normalized) {
            case "BACKLOG": return StoryStatus.BACKLOG;
            case "IN_PROGRESS": return StoryStatus.IN_PROGRESS;
            case "REVIEW": return StoryStatus.REVIEW;
            case "DONE": return StoryStatus.DONE;
            case "BLOCKED": return StoryStatus.BLOCKED;
            default: return null;
        }
    }
    
    /**
     * Traite la mise à jour d'une story
     */
    @PostMapping("/{id}/edit")
    public String updateStory(
        @PathVariable("id") Long id,
        @RequestParam(required=true) String title,
        @RequestParam(required=false) String description,
        @RequestParam(required=false) String status
    ) {
        Story story = repoFactory.getStoryRepo().find(id);
        
        if (story == null) {
            return "redirect:/story/list";
        }
        
        // Validate title
        if (title == null || title.trim().isEmpty()) {
            return "redirect:/story/" + id + "/edit?error=Title is required";
        }
        
        // Limiter la longueur du titre à 59 caractères
        if (title.length() > 59) {
            return "redirect:/story/" + id + "/edit?error=Title must be less than 59 characters";
        }
        
        story.setTitle(title.trim());
        story.setDescription(description);
        
        // Check if story is in a default column
        boolean inDefaultColumn = false;
        if (story.getColumnId() != null) {
            fr.uha.ensisa.gl.entities.Column column = repoFactory.getColumnRepo().find(story.getColumnId());
            if (column != null) {
                inDefaultColumn = isDefaultColumn(column.getName());
            }
        }
        
        // Only allow status change if NOT in default column
        if (!inDefaultColumn && status != null && !status.isEmpty()) {
            try {
                story.setStatus(StoryStatus.valueOf(status));
            } catch (IllegalArgumentException e) {
                // Invalid status, keep current status
            }
        }
        
        // DON'T call persist() - in-memory objects are references
        // Modifying the story object is enough, no need to persist
        // Calling persist() can duplicate the story if getId() == 0
        
        // Rediriger vers le board si la story a un projectId
        if (story.getProjectId() != null) {
            return "redirect:/board/" + story.getProjectId();
        }
        return "redirect:/story/" + id;
    }
    
    /**
     * Supprime une story
     */
    @PostMapping("/{id}/delete")
    public String deleteStory(@PathVariable("id") Long id) {
        repoFactory.getStoryRepo().remove(id);
        return "redirect:/story/list";
    }
    
    /**
     * Assigne une story à un utilisateur
     */
    @PostMapping("/{id}/assign")
    public String assignStory(
        @PathVariable("id") Long id,
        @RequestParam(required=true) int userId
    ) {
        Story story = repoFactory.getStoryRepo().find(id);
        
        if (story != null) {
            // Find user by ID
            fr.uha.ensisa.gl.entities.User user = repoFactory.getUserRepo().find(userId);
            if (user != null) {
                story.setUserAssigned(user);
            }
        }
        
        return "redirect:/story/list";
    }
    
    /**
     * Désassigne une story d'un utilisateur
     */
    @PostMapping("/{id}/unassign")
    public String unassignStory(@PathVariable("id") Long id) {
        Story story = repoFactory.getStoryRepo().find(id);
        
        if (story != null) {
            story.setUserAssigned(null);
        }
        
        return "redirect:/story/list";
    }

    @PostMapping("/{id}/timer/start")
    public String startTimer(@PathVariable("id") Long id, @RequestParam(required = false, defaultValue = "1") Long userId) {
        repoFactory.getStoryRepo().startTimer(id, userId);
        return "redirect:/story/list";
    }

    @PostMapping("/{id}/timer/stop")
    public String stopTimer(@PathVariable("id") Long id, @RequestParam Long workLogId) {
        repoFactory.getStoryRepo().stopTimer(id, workLogId);
        return "redirect:/story/list";
    }
}
