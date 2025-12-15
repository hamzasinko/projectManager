package fr.uha.ensisa.gl.tarnished.controller;

import fr.uha.ensisa.gl.entities.Story;
import fr.uha.ensisa.gl.entities.StoryStatus;
import fr.uha.ensisa.gl.entities.WorkLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import fr.uha.ensisa.gl.tarnished.repos.RepoFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

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
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long columnId,
            @RequestParam(required = false) Long swimlaneId) {

        ModelAndView mav = new ModelAndView("story-create");

        // Add all projects to select from
        mav.addObject("projects", repoFactory.getProjectRepo().findAll());

        // Pass projectId, columnId, and swimlaneId if provided
        if (projectId != null) {
            mav.addObject("projectId", projectId);
        }
        if (columnId != null) {
            mav.addObject("columnId", columnId);
        }
        if (swimlaneId != null) {
            mav.addObject("swimlaneId", swimlaneId);
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
        @RequestParam(required=false) Long projectId,
        @RequestParam(required=false) Long columnId,
        @RequestParam(required = false) Long swimlaneId
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
        
        // projectId is optional for stories; if absent we create a global story
        
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
            // Si pas de colonne spécifiée et si on a un projectId, trouver la colonne BACKLOG
            if (projectId != null) {
                Collection<fr.uha.ensisa.gl.entities.Column> columns = repoFactory.getColumnRepo().findByProject(projectId);
                for (fr.uha.ensisa.gl.entities.Column col : columns) {
                    if ("BACKLOG".equalsIgnoreCase(col.getName())) {
                        story.setColumnId((long) col.getId());
                        break;
                    }
                }
            } else {
                // pas de projectId fourni -> pas de colonne par défaut
                story.setColumnId(null);
            }
        }
        story.setStatus(initialStatus);
        
        // Décaler toutes les stories existantes de cette colonne (position + 1)
        if (story.getColumnId() != null) {
            Collection<Story> storiesInColumn = repoFactory.getStoryRepo().findByColumn(story.getColumnId());
            for (Story existingStory : storiesInColumn) {
                existingStory.setPosition(existingStory.getPosition() + 1);
                repoFactory.getStoryRepo().persist(existingStory);
            }
        }
        
        // Assigner position 0 pour que la nouvelle story apparaisse EN HAUT
        story.setPosition(0);
        
        // Initialiser subColumn pour les colonnes personnalisées (ni BACKLOG ni DONE)
        if (story.getColumnId() != null) {
            fr.uha.ensisa.gl.entities.Column column = repoFactory.getColumnRepo().find(story.getColumnId());
            if (column != null && !isDefaultColumn(column.getName())) {
                story.setSubColumn("BACKLOG");
                System.out.println("[DEBUG] Story subColumn initialized to BACKLOG for custom column: " + column.getName());
            } else {
                story.setSubColumn(null); // colonnes système => pas de sous-colonne
            }
        }

        if (swimlaneId != null) {
            story.setSwimlaneId(swimlaneId);
            System.out.println("[DEBUG] Story assigned to swimlane: " + swimlaneId);
        }

        repoFactory.getStoryRepo().persist(story);
        
        System.out.println("[DEBUG] Story created - ID: " + story.getId() + ", ProjectID: " + story.getProjectId() + ", ColumnID: " + story.getColumnId() + ", Position: " + story.getPosition() + ", SubColumn: " + story.getSubColumn());
        System.out.println("[DEBUG] Redirecting to: /board/" + story.getProjectId());
        
        // Rediriger vers le board du projet si projectId présent, sinon vers la liste des stories
        if (story.getProjectId() != null) {
            return "redirect:/board/" + story.getProjectId();
        }
        return "redirect:/story/list";
    }
    
    /**
     * Redirige vers la page d'accueil car les stories doivent être vues dans le contexte d'un projet
     * Les stories sont maintenant uniquement accessibles via le board du projet
     */
    @GetMapping("/list")
    public ModelAndView listStories() throws IOException {
        // Return the stories list view with all stories.
        ModelAndView mav = new ModelAndView("story-list");
        mav.addObject("stories", repoFactory.getStoryRepo().findAll());
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
            return new ModelAndView("redirect:/");
        }
        
        // Vérifier que la story a un projectId
        if (story.getProjectId() == null) {
            return new ModelAndView("redirect:/");
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
            return new ModelAndView("redirect:/");
        }
        
        // Vérifier que la story a un projectId
        if (story.getProjectId() == null) {
            return new ModelAndView("redirect:/");
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
        String normalized = columnName.toUpperCase(Locale.ROOT).replace(" ", "_");
        return normalized.equals("BACKLOG") || normalized.equals("IN_PROGRESS") || 
               normalized.equals("REVIEW") || normalized.equals("DONE") || normalized.equals("BLOCKED");
    }
    
    private StoryStatus mapColumnNameToStatus(String columnName) {
        if (columnName == null) return null;
        String normalized = columnName.toUpperCase(Locale.ROOT).replace(" ", "_");
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
        System.out.println("[DEBUG] updateStory called - storyId: " + id + ", title: " + title + ", status: " + status);
        Story story = repoFactory.getStoryRepo().find(id);
        
        if (story == null) {
            System.out.println("[DEBUG] Story not found! Redirecting to /story/list");
            return "redirect:/story/list";
        }
        
        System.out.println("[DEBUG] Found story - ID: " + story.getId() + ", CurrentTitle: " + story.getTitle() + ", UserAssigned: " + (story.getUserAssigned() != null ? story.getUserAssigned().getName() : "NULL"));
        
        // Validate title
        if (title == null || title.trim().isEmpty()) {
            System.out.println("[DEBUG] Title validation failed - empty title");
            return "redirect:/story/" + id + "/edit?error=Title is required";
        }
        
        // Limiter la longueur du titre à 59 caractères
        if (title.length() > 59) {
            System.out.println("[DEBUG] Title validation failed - too long");
            return "redirect:/story/" + id + "/edit?error=Title must be less than 59 characters";
        }
        
        System.out.println("[DEBUG] Setting new title: " + title.trim());
        story.setTitle(title.trim());
        System.out.println("[DEBUG] Setting new description: " + description);
        story.setDescription(description);
        
        // Check if story is in a default column
        boolean inDefaultColumn = false;
        if (story.getColumnId() != null) {
            fr.uha.ensisa.gl.entities.Column column = repoFactory.getColumnRepo().find(story.getColumnId());
            if (column != null) {
                inDefaultColumn = isDefaultColumn(column.getName());
            }
        }
        
        System.out.println("[DEBUG] Checking if in default column: " + inDefaultColumn);
        
        // Only allow status change if NOT in default column
        if (!inDefaultColumn && status != null && !status.isEmpty()) {
            System.out.println("[DEBUG] Updating status to: " + status);
            try {
                story.setStatus(StoryStatus.valueOf(status));
            } catch (IllegalArgumentException e) {
                System.out.println("[DEBUG] Invalid status value: " + status);
                // Invalid status, keep current status
            }
        }
        
        System.out.println("[DEBUG] About to persist story - ID: " + story.getId() + ", UserAssigned: " + (story.getUserAssigned() != null ? story.getUserAssigned().getName() : "NULL"));
        // MUST call persist() to ensure changes are saved
        // Even though in-memory objects are references, persist() ensures consistency
        repoFactory.getStoryRepo().persist(story);
        System.out.println("[DEBUG] Story persisted successfully");
        
        // Rediriger vers le board si la story a un projectId
        if (story.getProjectId() != null) {
            System.out.println("[DEBUG] Redirecting to /board/" + story.getProjectId());
            return "redirect:/board/" + story.getProjectId();
        }
        System.out.println("[DEBUG] Redirecting to /story/" + id);
        return "redirect:/story/" + id;
    }
    
    /**
     * Supprime une story
     */
    @PostMapping("/{id}/delete")
    public String deleteStory(@PathVariable("id") Long id) {
        Story story = repoFactory.getStoryRepo().find(id);
        Long projectId = story != null ? story.getProjectId() : null;
        
        repoFactory.getStoryRepo().remove(id);
        
        // Redirect to project board if story had a project, otherwise to home
        if (projectId != null) {
            return "redirect:/board/" + projectId;
        }
        // When no project is associated, show the stories list (integration tests expect this)
        return "redirect:/story/list";
    }
    
    /**
     * Assigne une story à un utilisateur
     */
    @GetMapping("/{id}/assign")
    public String assignStory(
        @PathVariable("id") Long id,
        @RequestParam(required=true) int userId
    ) {
        System.out.println("[DEBUG] assignStory called - storyId: " + id + ", userId: " + userId);
        Story story = repoFactory.getStoryRepo().find(id);
        System.out.println("[DEBUG] Found story: " + (story != null ? "ID=" + story.getId() : "NULL"));
        
        if (story != null) {
            // Find user by ID
            fr.uha.ensisa.gl.entities.User user = repoFactory.getUserRepo().find(userId);
            System.out.println("[DEBUG] Found user: " + (user != null ? user.getName() + " (ID=" + user.getId() + ")" : "NULL"));
            if (user != null) {
                story.setUserAssigned(user);
                System.out.println("[DEBUG] Before persist - Story ID: " + story.getId() + ", UserAssigned: " + (story.getUserAssigned() != null ? story.getUserAssigned().getName() : "NULL"));
                repoFactory.getStoryRepo().persist(story); // SAVE THE CHANGES!
                System.out.println("[DEBUG] After persist - Story saved successfully");
            }
            
            // Redirect to project board if story has a project
            if (story.getProjectId() != null) {
                System.out.println("[DEBUG] Redirecting to /board/" + story.getProjectId());
                return "redirect:/board/" + story.getProjectId();
            }
        }
        
        System.out.println("[DEBUG] Redirecting to /");
        return "redirect:/";
    }
    
    /**
     * Désassigne une story d'un utilisateur
     */
    @GetMapping("/{id}/unassign")
    public String unassignStory(@PathVariable("id") Long id) {
        Story story = repoFactory.getStoryRepo().find(id);
        
        if (story != null) {
            story.setUserAssigned(null);
            repoFactory.getStoryRepo().persist(story); // SAVE THE CHANGES!
            
            // Redirect to project board if story has a project
            if (story.getProjectId() != null) {
                return "redirect:/board/" + story.getProjectId();
            }
        }
        
        return "redirect:/";
    }

    @PostMapping("/{id}/timer/start")
    public String startTimer(@PathVariable("id") Long id, @RequestParam(required = false, defaultValue = "1") Long userId) {
        repoFactory.getStoryRepo().startTimer(id, userId);
        return "redirect:/story/" + id;
    }

    @PostMapping("/{id}/timer/stop")
    public String stopTimer(@PathVariable("id") Long id, @RequestParam Long workLogId) {
        repoFactory.getStoryRepo().stopTimer(id, workLogId);
        return "redirect:/story/" + id;
    }
    
    @PostMapping("/{id}/worklog/add")
    public String addWorkLog(@PathVariable("id") Long id, 
                            @RequestParam(required = false, defaultValue = "0") int days,
                            @RequestParam(required = false, defaultValue = "0") int hours,
                            @RequestParam(required = false, defaultValue = "0") int minutes,
                            @RequestParam(required = false) String comment,
                            @RequestParam(required = false, defaultValue = "1") Long userId) {
        // Calculate total duration in minutes
        long totalMinutes = (days * 24 * 60) + (hours * 60) + minutes;
        
        if (totalMinutes <= 0) {
            return "redirect:/story/" + id + "?error=Duration must be greater than 0";
        }
        
        // Limit comment to 120 characters
        if (comment != null && comment.length() > 45) {
            comment = comment.substring(0, 45);
        }
        
        WorkLog workLog = new WorkLog();
        workLog.setId(System.currentTimeMillis()); // Simple ID generation
        workLog.setStart(LocalDateTime.now().minus(totalMinutes, ChronoUnit.MINUTES));
        workLog.setEnd(LocalDateTime.now());
        workLog.setDuration(totalMinutes);
        workLog.setUserId(userId);
        workLog.setStoryId(id);
        workLog.setComment(comment);
        
        repoFactory.getStoryRepo().addWorkLog(id, workLog);
        return "redirect:/story/" + id;
    }
    
    @PostMapping("/{storyId}/worklog/{workLogId}/delete")
    public String deleteWorkLog(@PathVariable Long storyId, @PathVariable Long workLogId) {
        repoFactory.getStoryRepo().removeWorkLog(storyId, workLogId);
        return "redirect:/story/" + storyId;
    }
}
