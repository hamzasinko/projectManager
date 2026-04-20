package fr.uha.ensisa.gl.tarnished.controller;

import fr.uha.ensisa.gl.entities.Column;
import fr.uha.ensisa.gl.entities.Project;
import fr.uha.ensisa.gl.entities.Story;
import fr.uha.ensisa.gl.entities.Swimlane;
import fr.uha.ensisa.gl.tarnished.config.PathHelper;
import fr.uha.ensisa.gl.tarnished.repos.RepoFactory;
import fr.uha.ensisa.gl.tarnished.repos.SwimlaneRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/board")
public class BoardController {

    @Autowired
    private RepoFactory repoFactory;

    @Autowired
    private PathHelper pathHelper;

    //setter pour les tests
    void setRepoFactory(RepoFactory repoFactory) {
        this.repoFactory = repoFactory;
    }

    void setPathHelper(PathHelper pathHelper) {
        this.pathHelper = pathHelper;
    }

    //affiche le kanban board d'un projet
    @GetMapping("/{projectId}")
    public ModelAndView showBoard(@PathVariable Long projectId) {
        ModelAndView mav = new ModelAndView("board");

        SwimlaneRepo swimlaneRepo = repoFactory.getSwimlaneRepo();
        Project project = repoFactory.getProjectRepo().find(projectId);
        if (project == null) {
            return pathHelper.redirectView("/project/list");
        }

        //récupère toutes les colonnes du projet
        Collection<Column> columns = repoFactory.getColumnRepo().findByProject(projectId);
        
        //pour chaque colonne on récupère ses stories
        for (Column column : columns) {
            if (column.getName() == null) {
                continue; // skip columns with null names
            }
            String columnName = column.getName().toUpperCase(Locale.ROOT);
            
            //on initialise hasSubColumns à true pour les colonnes par défaut sauf BACKLOG, DONE et BLOCKED
            if (!column.isHasSubColumns() && 
                (columnName.equals("IN PROGRESS") || 
                 columnName.equals("REVIEW"))) {
                column.setHasSubColumns(true);
                repoFactory.getColumnRepo().persist(column);
            }
                        //forcer BLOCKED à ne PAS avoir de sous-colonnes (même si elle en avait avant)
            if (columnName.equals("BLOCKED") && column.isHasSubColumns()) {
                column.setHasSubColumns(false);
                repoFactory.getColumnRepo().persist(column);
            }
            
            Collection<Story> stories = repoFactory.getStoryRepo().findByColumn((long) column.getId());
            column.setStories(new java.util.ArrayList<>(stories));
        }

        List<Swimlane> swimlanes = swimlaneRepo.findAll().stream()
                .filter(s -> s.getProjectId() == projectId.intValue())
                .toList();

        boolean hasSwimlanes = (swimlanes != null && !swimlanes.isEmpty());

        mav.addObject("swimlanes", swimlanes);
        mav.addObject("hasSwimlanes", hasSwimlanes);
        mav.addObject("project", project);
        mav.addObject("columns", columns);

        return mav;
    }

    //déplace une story vers une autre colonne (ajax)
    @PostMapping("/{projectId}/move-story")
    @ResponseBody
    public String moveStory(
            @PathVariable Long projectId,
            @RequestParam Long storyId,
            @RequestParam Long toColumnId,
            @RequestParam(required = false) Long fromColumnId,
            @RequestParam(required = false) String newStatus,
            @RequestParam(required = false) String subColumn,
            @RequestParam(required = false) Long swimlaneId) {
        
        try {
            //vérifie si la colonne cible est pleine avant de déplacer
            Column targetColumn = repoFactory.getColumnRepo().find(toColumnId);
            if (targetColumn != null && targetColumn.getMaxCapacity() > 0) {
                Collection<Story> storiesInColumn = repoFactory.getStoryRepo().findByColumn(toColumnId);
                if (storiesInColumn.size() >= targetColumn.getMaxCapacity()) {
                    return "{\"success\":false,\"error\":\"Column is full (max " + targetColumn.getMaxCapacity() + ")\"}";
                }
            }
            
            repoFactory.getColumnRepo().moveStoryBetweenColumns(storyId, fromColumnId, toColumnId);
            
            //met à jour position, subcolumn et status selon la colonne cible
            String newStatusStr = null;
            Story story = repoFactory.getStoryRepo().find(storyId);
            if (story != null) {
                System.out.println("[DEBUG] moveStory - storyId: " + storyId + ", toColumnId: " + toColumnId + ", subColumn param: " + subColumn);
                System.out.println("[DEBUG] Before move - Story ID: " + story.getId() + ", SubColumn: " + story.getSubColumn() + ", Status: " + story.getStatus());
                
                //position 0 pour mettre en haut
                story.setPosition(0);
                
                //sauvegarde subcolumn si fourni
                if (subColumn != null && !subColumn.isEmpty()) {
                    story.setSubColumn(subColumn);
                    System.out.println("[DEBUG] SubColumn set from parameter: " + subColumn);
                } else {
                    //par défaut BACKLOG pour les colonnes personnalisées
                    if (targetColumn != null && !"BACKLOG".equals(targetColumn.getName()) && !"DONE".equals(targetColumn.getName())) {
                        story.setSubColumn("BACKLOG");
                        System.out.println("[DEBUG] SubColumn defaulted to BACKLOG for custom column");
                    } else {
                        story.setSubColumn(null);
                        System.out.println("[DEBUG] SubColumn set to null for base column");
                    }
                }
                
                //toujours mettre à jour le status selon le nom de la colonne pour les colonnes par défaut
                //colonnes par défaut: BACKLOG, IN PROGRESS, REVIEW, DONE, BLOCKED
                //les colonnes perso et sous-colonnes ne changent pas le status
                if (targetColumn != null) {
                    String columnName = targetColumn.getName();
                    fr.uha.ensisa.gl.entities.StoryStatus mappedStatus = mapColumnNameToStatus(columnName);
                    if (mappedStatus != null) {
                        //colonne par défaut, on doit mettre à jour le status
                        story.setStatus(mappedStatus);
                        newStatusStr = mappedStatus.name();
                        System.out.println("[DEBUG] Status updated to: " + newStatusStr + " based on default column: " + columnName);
                    } else {
                        //colonne perso, on garde le status actuel
                        System.out.println("[DEBUG] Custom column detected, status unchanged: " + columnName);
                    }
                }
                //il faut persister pour sauvegarder position et status
                if (swimlaneId != null) {
                    story.setSwimlaneId(swimlaneId);
                    System.out.println("[DEBUG] Swimlane updated to: " + swimlaneId);
                }
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

    //crée une nouvelle colonne pour un projet (GET pour compatibilité tests)
    @GetMapping("/{projectId}/add-column")
    public String addColumnGet(
            @PathVariable Long projectId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false, defaultValue = "0") int maxCapacity,
            @RequestParam(required = false, defaultValue = "false") boolean hasSubColumns) {
        //si name pas fourni, redirige vers le board avec param pour afficher le formulaire
        if (name == null || name.trim().isEmpty()) {
            return pathHelper.redirect("/board/" + projectId + "?showAddColumn=true");
        }
        return addColumn(projectId, name, maxCapacity, hasSubColumns);
    }

    //crée une nouvelle colonne pour un projet
    @PostMapping("/{projectId}/add-column")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public String addColumn(
            @PathVariable Long projectId,
            @RequestParam String name,
            @RequestParam(required = false, defaultValue = "0") int maxCapacity,
            @RequestParam(required = false, defaultValue = "false") boolean hasSubColumns) {

        Project project = repoFactory.getProjectRepo().find(projectId);
        if (project == null) {
            return pathHelper.redirect("/project/list");
        }

        //validation: vérifie que name n'est pas null
        if (name == null) {
            throw new NullPointerException("Column name cannot be null");
        }

        //trim le nom
        name = name.trim();
        
        //validation: limite le nom à 25 caractères
        if (name.length() > 25) {
            name = name.substring(0, 25);
        }

        Column column = new Column();
        column.setName(name);
        column.setProject(project);
        column.setMaxCapacity(maxCapacity);
        column.setHasSubColumns(hasSubColumns);
        
        //trouve la colonne DONE et place la nouvelle colonne juste avant
        Collection<Column> existingColumns = repoFactory.getColumnRepo().findByProject(projectId);
        Column doneColumn = null;
        for (Column col : existingColumns) {
            if (col != null && col.getName() != null && "DONE".equalsIgnoreCase(col.getName().trim())) {
                doneColumn = col;
                break;
            }
        }
        
        if (doneColumn != null) {
            //place la nouvelle colonne juste avant DONE
            int donePosition = doneColumn.getPosition();
            column.setPosition(donePosition);
            
            //décale toutes les colonnes à partir de DONE (incluant DONE) vers la droite
            for (Column col : existingColumns) {
                if (col != null && col.getPosition() >= donePosition) {
                    col.setPosition(col.getPosition() + 1);
                    repoFactory.getColumnRepo().persist(col);
                }
            }
        } else {
            //si pas de DONE, place à la fin
            column.setPosition(existingColumns.size() + 1);
        }

        repoFactory.getColumnRepo().persist(column);

        return pathHelper.redirect("/board/" + projectId);
    }
    
    //réordonne les stories dans une colonne (ajax)
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
    
    //supprime une colonne
    @PostMapping("/{projectId}/delete-column/{columnId}")
    public String deleteColumn(
            @PathVariable Long projectId,
            @PathVariable Long columnId) {
        
        Column column = repoFactory.getColumnRepo().find(columnId);
        if (column == null) {
            return pathHelper.redirect("/board/" + projectId + "?error=Column not found");
        }
        
        //interdit la suppression de BACKLOG et DONE
        String columnName = column.getName().toUpperCase(Locale.ROOT).replace(" ", "_");
        if ("BACKLOG".equals(columnName) || "DONE".equals(columnName)) {
            return pathHelper.redirect("/board/" + projectId + "?error=Cannot delete " + column.getName() + " column");
        }
        
        //vérifie si la colonne contient des stories
        Collection<Story> storiesInColumn = repoFactory.getStoryRepo().findByColumn(columnId);
        if (!storiesInColumn.isEmpty()) {
            return pathHelper.redirect("/board/" + projectId + "?error=Cannot delete column with stories. Please move stories first");
        }
        
        repoFactory.getColumnRepo().remove(columnId);
        return pathHelper.redirect("/board/" + projectId);
    }
    
    //supprime une colonne avec toutes ses stories
    @PostMapping("/{projectId}/delete-column-with-stories/{columnId}")
    @ResponseBody
    public String deleteColumnWithStories(
            @PathVariable Long projectId,
            @PathVariable Long columnId) {
        
        Column column = repoFactory.getColumnRepo().find(columnId);
        if (column == null) {
            return "error";
        }
        
        //supprime d'abord toutes les stories de la colonne
        Collection<Story> storiesInColumn = repoFactory.getStoryRepo().findByColumn(columnId);
        for (Story story : storiesInColumn) {
            repoFactory.getStoryRepo().remove(story.getId());
        }
        
        //puis supprime la colonne
        repoFactory.getColumnRepo().remove(columnId);
        return "success";
    }
    
    //déplace toutes les stories d'une colonne vers une autre
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
    
    //réordonne les colonnes (ajax)
    @PostMapping("/{projectId}/reorder-columns")
    @ResponseBody
    public String reorderColumns(
            @PathVariable Long projectId,
            @RequestParam String columnOrder) {
        
        String[] columnIds = columnOrder.split(",");

        //s'assure que BACKLOG est toujours en position 1
        Long backlogId = null;
        for (fr.uha.ensisa.gl.entities.Column c : repoFactory.getColumnRepo().findByProject(projectId)) {
            if (c != null && c.getName() != null && "BACKLOG".equalsIgnoreCase(c.getName().trim())) {
                backlogId = (long) c.getId();
                break;
            }
        }

        int pos = 1;
        //si backlog existe, on le met en premier
        if (backlogId != null) {
            repoFactory.getColumnRepo().reorder(backlogId, pos++);
        }

        //puis on applique l'ordre pour les colonnes restantes, en sautant backlog si présent
        for (int i = 0; i < columnIds.length; i++) {
            Long columnId = Long.parseLong(columnIds[i]);
            if (backlogId != null && columnId.equals(backlogId)) continue;
            repoFactory.getColumnRepo().reorder(columnId, pos++);
        }

        return "{\"success\":true}";
    }
    
    //met à jour le nom d'une colonne (GET pour compatibilité tests)
    @GetMapping("/{projectId}/update-column")
    public String updateColumnNameGet(
            @PathVariable Long projectId,
            @RequestParam Long columnId,
            @RequestParam String newName) {
        try {
            Column column = repoFactory.getColumnRepo().find(columnId);
            if (column == null) {
                return pathHelper.redirect("/board/" + projectId + "?error=Column not found");
            }
            
            //validation du nom
            if (newName == null || newName.trim().isEmpty()) {
                return pathHelper.redirect("/board/" + projectId + "?error=Column name cannot be empty");
            }
            
            column.setName(newName.trim());
            repoFactory.getColumnRepo().persist(column);
        } catch (Exception e) {
            return pathHelper.redirect("/board/" + projectId + "?error=" + e.getMessage());
        }
        return pathHelper.redirect("/board/" + projectId);
    }
    
    //met à jour le nom d'une colonne (ajax)
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
            
            //validation du nom
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
    
    //met à jour une colonne (nom + capacité) (ajax)
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
            
            //met à jour le nom si fourni
            if (newName != null && !newName.trim().isEmpty()) {
                column.setName(newName.trim());
            }
            
            //met à jour la capacité
            column.setMaxCapacity(maxCapacity);
            repoFactory.getColumnRepo().persist(column);
            
            return "{\"success\":true}";
        } catch (Exception e) {
            return "{\"success\":false,\"error\":\"" + e.getMessage() + "\"}";
        }
    }
    
    //map le nom de colonne vers StoryStatus enum
    //gère les colonnes par défaut: BACKLOG, IN PROGRESS (ou IN_PROGRESS), REVIEW, DONE, BLOCKED
    private fr.uha.ensisa.gl.entities.StoryStatus mapColumnNameToStatus(String columnName) {
        if (columnName == null) return null;
        //normalise: majuscules et remplace espaces par underscores, trim
        String normalized = columnName.toUpperCase(Locale.ROOT).trim().replace(" ", "_").replace("-", "_");
        
        //matches directs
        switch (normalized) {
            case "BACKLOG": return fr.uha.ensisa.gl.entities.StoryStatus.BACKLOG;
            case "IN_PROGRESS": return fr.uha.ensisa.gl.entities.StoryStatus.IN_PROGRESS;
            case "REVIEW": return fr.uha.ensisa.gl.entities.StoryStatus.REVIEW;
            case "DONE": return fr.uha.ensisa.gl.entities.StoryStatus.DONE;
            case "BLOCKED": return fr.uha.ensisa.gl.entities.StoryStatus.BLOCKED;
            default: 
                //vérifications supplémentaires pour les variations
                if (normalized.contains("BACKLOG")) return fr.uha.ensisa.gl.entities.StoryStatus.BACKLOG;
                if (normalized.contains("IN_PROGRESS") || normalized.contains("INPROGRESS")) return fr.uha.ensisa.gl.entities.StoryStatus.IN_PROGRESS;
                if (normalized.contains("REVIEW")) return fr.uha.ensisa.gl.entities.StoryStatus.REVIEW;
                if (normalized.contains("DONE")) return fr.uha.ensisa.gl.entities.StoryStatus.DONE;
                if (normalized.contains("BLOCKED")) return fr.uha.ensisa.gl.entities.StoryStatus.BLOCKED;
                return null;
        }
    }
}
