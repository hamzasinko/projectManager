package fr.uha.ensisa.gl.tarnished.controller;

import fr.uha.ensisa.gl.entities.Story;
import fr.uha.ensisa.gl.entities.StoryStatus;
import fr.uha.ensisa.gl.entities.WorkLog;
import fr.uha.ensisa.gl.tarnished.config.PathHelper;
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
    
    @Autowired
    private PathHelper pathHelper;
    
    //setter pour les tests
    void setPathHelper(PathHelper pathHelper) {
        this.pathHelper = pathHelper;
    }
    
    public StoryController() {
        System.out.println("*** StoryController CREATED ***");
    }
    
    //affiche le formulaire de création de story
    @GetMapping("/new")
    public ModelAndView showCreateForm(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long columnId,
            @RequestParam(required = false) Long swimlaneId) {

        ModelAndView mav = new ModelAndView("story-create");

        //ajoute tous les projets pour la sélection
        mav.addObject("projects", repoFactory.getProjectRepo().findAll());

        //passe projectId, columnId et swimlaneId si fournis
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

    //traite la création d'une nouvelle story
    @PostMapping("/create")
    public String createStory(
        @RequestParam(required=true) String title,
        @RequestParam(required=false) String description,
        @RequestParam(required=false) Long projectId,
        @RequestParam(required=false) Long columnId,
        @RequestParam(required = false) Long swimlaneId
    ) throws IOException {
        
        System.out.println("[DEBUG] createStory called - projectId: " + projectId + ", columnId: " + columnId);
        
        //valide le titre
        if (title == null || title.trim().isEmpty()) {
            return pathHelper.redirect("/story/new?error=Title is required&projectId=" + projectId);
        }
        
        //limite la longueur du titre à 59 caractères
        if (title.length() > 59) {
            return pathHelper.redirect("/story/new?error=Title must be less than 59 characters&projectId=" + projectId);
        }
        
        //projectId optionnel pour les stories, si absent on crée une story globale
        
        Story story = new Story();
        story.setTitle(title.trim());
        story.setDescription(description);
        story.setDateCreated(new Date());
        story.setProjectId(projectId);
        
        //définit le status et columnId selon la colonne
        StoryStatus initialStatus = StoryStatus.BACKLOG;
        if (columnId != null) {
            //assigne la story à la colonne spécifiée
            story.setColumnId(columnId);
            fr.uha.ensisa.gl.entities.Column column = repoFactory.getColumnRepo().find(columnId);
            if (column != null) {
                StoryStatus columnStatus = mapColumnNameToStatus(column.getName());
                if (columnStatus != null) {
                    initialStatus = columnStatus;
                }
            }
        } else {
            //si pas de colonne spécifiée et qu'on a un projectId, trouve la colonne BACKLOG
            if (projectId != null) {
                Collection<fr.uha.ensisa.gl.entities.Column> columns = repoFactory.getColumnRepo().findByProject(projectId);
                for (fr.uha.ensisa.gl.entities.Column col : columns) {
                    if ("BACKLOG".equalsIgnoreCase(col.getName())) {
                        story.setColumnId((long) col.getId());
                        break;
                    }
                }
            } else {
                //pas de projectId fourni donc pas de colonne par défaut
                story.setColumnId(null);
            }
        }
        story.setStatus(initialStatus);
        
        //décale toutes les stories existantes de cette colonne (position + 1)
        if (story.getColumnId() != null) {
            Collection<Story> storiesInColumn = repoFactory.getStoryRepo().findByColumn(story.getColumnId());
            for (Story existingStory : storiesInColumn) {
                existingStory.setPosition(existingStory.getPosition() + 1);
                repoFactory.getStoryRepo().persist(existingStory);
            }
        }
        
        //position 0 pour que la nouvelle story apparaisse en haut
        story.setPosition(0);
        
        //initialise subColumn pour les colonnes personnalisées (ni BACKLOG ni DONE)
        if (story.getColumnId() != null) {
            fr.uha.ensisa.gl.entities.Column column = repoFactory.getColumnRepo().find(story.getColumnId());
            if (column != null && !isDefaultColumn(column.getName())) {
                story.setSubColumn("BACKLOG");
                System.out.println("[DEBUG] Story subColumn initialized to BACKLOG for custom column: " + column.getName());
            } else {
                story.setSubColumn(null); //colonnes système donc pas de sous-colonne
            }
        }

        if (swimlaneId != null) {
            story.setSwimlaneId(swimlaneId);
            System.out.println("[DEBUG] Story assigned to swimlane: " + swimlaneId);
        }

        repoFactory.getStoryRepo().persist(story);
        
        System.out.println("[DEBUG] Story created - ID: " + story.getId() + ", ProjectID: " + story.getProjectId() + ", ColumnID: " + story.getColumnId() + ", Position: " + story.getPosition() + ", SubColumn: " + story.getSubColumn());
        System.out.println("[DEBUG] Redirecting to: /board/" + story.getProjectId());
        
        //redirige vers le board du projet si projectId présent, sinon vers la liste des stories
        if (story.getProjectId() != null) {
            return pathHelper.redirect("/board/" + story.getProjectId());
        }
        return pathHelper.redirect("/story/list");
    }
    
    //redirige vers la page d'accueil car les stories doivent être vues dans le contexte d'un projet
    //les stories sont maintenant uniquement accessibles via le board du projet
    @GetMapping("/list")
    public ModelAndView listStories() throws IOException {
        //retourne la vue liste des stories avec toutes les stories
        ModelAndView mav = new ModelAndView("story-list");
        mav.addObject("stories", repoFactory.getStoryRepo().findAll());
        return mav;
    }
    
    //affiche les détails d'une story
    @GetMapping("/{id}")
    public ModelAndView showStory(@PathVariable("id") Long id) throws IOException {
        ModelAndView mav = new ModelAndView("story-detail");
        Story story = repoFactory.getStoryRepo().find(id);
        
        if (story == null) {
            return pathHelper.redirectView("/");
        }
        
        //vérifie que la story a un projectId
        if (story.getProjectId() == null) {
            return pathHelper.redirectView("/");
        }
        
        mav.addObject("story", story);
        return mav;
    }
    
    //affiche le formulaire d'édition d'une story
    @GetMapping("/{id}/edit")
    public ModelAndView editStory(@PathVariable("id") Long id) {
        ModelAndView mav = new ModelAndView("story-edit");
        Story story = repoFactory.getStoryRepo().find(id);
        
        if (story == null) {
            return pathHelper.redirectView("/");
        }
        
        //vérifie que la story a un projectId
        if (story.getProjectId() == null) {
            return pathHelper.redirectView("/");
        }
        
        mav.addObject("story", story);
        mav.addObject("users", repoFactory.getUserRepo().getAll());
        
        //vérifie si la story est dans une colonne par défaut
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
    
    //traite la mise à jour d'une story
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
            return pathHelper.redirect("/story/list");
        }
        
        System.out.println("[DEBUG] Found story - ID: " + story.getId() + ", CurrentTitle: " + story.getTitle() + ", UserAssigned: " + (story.getUserAssigned() != null ? story.getUserAssigned().getName() : "NULL"));
        
        //valide le titre
        if (title == null || title.trim().isEmpty()) {
            System.out.println("[DEBUG] Title validation failed - empty title");
            return pathHelper.redirect("/story/" + id + "/edit?error=Title is required");
        }
        
        //limite la longueur du titre à 59 caractères
        if (title.length() > 59) {
            System.out.println("[DEBUG] Title validation failed - too long");
            return pathHelper.redirect("/story/" + id + "/edit?error=Title must be less than 59 characters");
        }
        
        System.out.println("[DEBUG] Setting new title: " + title.trim());
        story.setTitle(title.trim());
        System.out.println("[DEBUG] Setting new description: " + description);
        story.setDescription(description);
        
        //vérifie si la story est dans une colonne par défaut
        boolean inDefaultColumn = false;
        if (story.getColumnId() != null) {
            fr.uha.ensisa.gl.entities.Column column = repoFactory.getColumnRepo().find(story.getColumnId());
            if (column != null) {
                inDefaultColumn = isDefaultColumn(column.getName());
            }
        }
        
        System.out.println("[DEBUG] Checking if in default column: " + inDefaultColumn);
        
        //permet le changement de status seulement si PAS dans une colonne par défaut
        if (!inDefaultColumn && status != null && !status.isEmpty()) {
            System.out.println("[DEBUG] Updating status to: " + status);
            try {
                story.setStatus(StoryStatus.valueOf(status));
            } catch (IllegalArgumentException e) {
                System.out.println("[DEBUG] Invalid status value: " + status);
                //status invalide, on garde le status actuel
            }
        }
        
        System.out.println("[DEBUG] About to persist story - ID: " + story.getId() + ", UserAssigned: " + (story.getUserAssigned() != null ? story.getUserAssigned().getName() : "NULL"));
        //il faut appeler persist() pour sauvegarder les changements
        //même si les objets en mémoire sont des références, persist() assure la cohérence
        repoFactory.getStoryRepo().persist(story);
        System.out.println("[DEBUG] Story persisted successfully");
        
        //redirige vers le board si la story a un projectId
        if (story.getProjectId() != null) {
            System.out.println("[DEBUG] Redirecting to /board/" + story.getProjectId());
            return pathHelper.redirect("/board/" + story.getProjectId());
        }
        System.out.println("[DEBUG] Redirecting to /story/" + id);
        return pathHelper.redirect("/story/" + id);
    }
    
    //supprime une story
    @PostMapping("/{id}/delete")
    public String deleteStory(@PathVariable("id") Long id) {
        Story story = repoFactory.getStoryRepo().find(id);
        Long projectId = story != null ? story.getProjectId() : null;
        
        repoFactory.getStoryRepo().remove(id);
        
        //redirige vers le board du projet si la story avait un projet, sinon vers home
        if (projectId != null) {
            return pathHelper.redirect("/board/" + projectId);
        }
        //quand pas de projet associé, affiche la liste des stories (les tests d'intégration s'y attendent)
        return pathHelper.redirect("/story/list");
    }
    
    //assigne une story à un utilisateur
    @GetMapping("/{id}/assign")
    public String assignStory(
        @PathVariable("id") Long id,
        @RequestParam(required=true) int userId
    ) {
        System.out.println("[DEBUG] assignStory called - storyId: " + id + ", userId: " + userId);
        Story story = repoFactory.getStoryRepo().find(id);
        System.out.println("[DEBUG] Found story: " + (story != null ? "ID=" + story.getId() : "NULL"));
        
        if (story != null) {
            //trouve l'utilisateur par ID
            fr.uha.ensisa.gl.entities.User user = repoFactory.getUserRepo().find(userId);
            System.out.println("[DEBUG] Found user: " + (user != null ? user.getName() + " (ID=" + user.getId() + ")" : "NULL"));
            if (user != null) {
                story.setUserAssigned(user);
                System.out.println("[DEBUG] Before persist - Story ID: " + story.getId() + ", UserAssigned: " + (story.getUserAssigned() != null ? story.getUserAssigned().getName() : "NULL"));
                repoFactory.getStoryRepo().persist(story); //sauvegarde les changements!
                System.out.println("[DEBUG] After persist - Story saved successfully");
            }
            
            //redirige vers le board du projet si la story a un projet
            if (story.getProjectId() != null) {
                System.out.println("[DEBUG] Redirecting to /board/" + story.getProjectId());
                return pathHelper.redirect("/board/" + story.getProjectId());
            }
        }
        
        System.out.println("[DEBUG] Redirecting to /");
        return pathHelper.redirect("/");
    }
    
    //désassigne une story d'un utilisateur
    @GetMapping("/{id}/unassign")
    public String unassignStory(@PathVariable("id") Long id) {
        Story story = repoFactory.getStoryRepo().find(id);
        
        if (story != null) {
            story.setUserAssigned(null);
            repoFactory.getStoryRepo().persist(story); //sauvegarde les changements!
            
            //redirige vers le board du projet si la story a un projet
            if (story.getProjectId() != null) {
                return pathHelper.redirect("/board/" + story.getProjectId());
            }
        }
        
        return pathHelper.redirect("/");
    }

    @PostMapping("/{id}/timer/start")
    public String startTimer(@PathVariable("id") Long id, @RequestParam(required = false, defaultValue = "1") Long userId) {
        repoFactory.getStoryRepo().startTimer(id, userId);
        return pathHelper.redirect("/story/" + id);
    }

    @PostMapping("/{id}/timer/stop")
    public String stopTimer(@PathVariable("id") Long id, @RequestParam Long workLogId) {
        repoFactory.getStoryRepo().stopTimer(id, workLogId);
        return pathHelper.redirect("/story/" + id);
    }
    
    @PostMapping("/{id}/worklog/add")
    public String addWorkLog(@PathVariable("id") Long id, 
                            @RequestParam(required = false, defaultValue = "0") int days,
                            @RequestParam(required = false, defaultValue = "0") int hours,
                            @RequestParam(required = false, defaultValue = "0") int minutes,
                            @RequestParam(required = false) String comment,
                            @RequestParam(required = false, defaultValue = "1") Long userId) {
        //calcule la durée totale en minutes
        long totalMinutes = (days * 24 * 60) + (hours * 60) + minutes;
        
        if (totalMinutes <= 0) {
            return pathHelper.redirect("/story/" + id + "?error=Duration must be greater than 0");
        }
        
        //limite le commentaire à 45 caractères
        if (comment != null && comment.length() > 45) {
            comment = comment.substring(0, 45);
        }
        
        WorkLog workLog = new WorkLog();
        workLog.setId(System.currentTimeMillis()); //génération simple d'ID
        workLog.setStart(LocalDateTime.now().minus(totalMinutes, ChronoUnit.MINUTES));
        workLog.setEnd(LocalDateTime.now());
        workLog.setDuration(totalMinutes);
        workLog.setUserId(userId);
        workLog.setStoryId(id);
        workLog.setComment(comment);
        
        repoFactory.getStoryRepo().addWorkLog(id, workLog);
        return pathHelper.redirect("/story/" + id);
    }
    
    @PostMapping("/{storyId}/worklog/{workLogId}/delete")
    public String deleteWorkLog(@PathVariable Long storyId, @PathVariable Long workLogId) {
        repoFactory.getStoryRepo().removeWorkLog(storyId, workLogId);
        return pathHelper.redirect("/story/" + storyId);
    }
}
