package fr.uha.ensisa.gl.tarnished.controller;

import fr.uha.ensisa.gl.entities.Column;
import fr.uha.ensisa.gl.entities.Project;
import fr.uha.ensisa.gl.tarnished.config.PathHelper;
import fr.uha.ensisa.gl.tarnished.repos.ColumnRepo;
import fr.uha.ensisa.gl.tarnished.repos.ProjectRepo;
import fr.uha.ensisa.gl.tarnished.repos.RepoFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.Locale;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;

@Controller
public class ColumnController {

    @Autowired
    private RepoFactory repoFactory;

    @Autowired
    private PathHelper pathHelper;

    //setter pour les tests
    void setPathHelper(PathHelper pathHelper) {
        this.pathHelper = pathHelper;
    }

    @GetMapping("/columns")
    public String listColumns(Model model) {
        ColumnRepo columnRepo = repoFactory.getColumnRepo();
        Collection<Column> columns = columnRepo.findAll();
        model.addAttribute("columns", columns);
        return "column-list";
    }

    @GetMapping("/columns/create")
    public String showCreateForm(@RequestParam(required = false) Long projectId, Model model) {
        model.addAttribute("projectId", projectId);
        return "column-create";
    }

    @PostMapping("/columns/create")
    public String createColumn(@RequestParam String name,
                               @RequestParam(required = false, defaultValue = "0") int order,
                               @RequestParam(required = false, defaultValue = "0") int limit,
                               @RequestParam(required = false, defaultValue = "false") boolean hasSubColumns,
                               @RequestParam(required = false) Long projectId) {
        ColumnRepo columnRepo = repoFactory.getColumnRepo();
        
        //validation: limite le nom à 25 caractères
        if (name != null && name.length() > 25) {
            name = name.substring(0, 25);
        }
        //anti-duplication (même nom dans le même projet)
        String normalizedName = (name == null) ? "" : name.trim().toLowerCase(Locale.ROOT);

        boolean alreadyExists = columnRepo.findAll().stream().anyMatch(c ->
                c.getName() != null
                        && c.getName().trim().toLowerCase(Locale.ROOT).equals(normalizedName)
                        && (
                        (projectId == null && c.getProject() == null) ||
                                (projectId != null && c.getProject() != null && c.getProject().getId() == projectId)
                )
        );

        if (alreadyExists) {
            return pathHelper.redirect("/columns?error=Column already exists");
        }


        Column column = new Column();
        name = name.trim();
        column.setName(name);
        column.setPosition(order);
        column.setMaxCapacity(limit);
        column.setHasSubColumns(hasSubColumns);
        column.setStories(new ArrayList<>());
        
        if (projectId != null) {
            ProjectRepo projectRepo = repoFactory.getProjectRepo();
            Project project = projectRepo.find(projectId);
            column.setProject(project);
        }
        
        columnRepo.persist(column);
        return pathHelper.redirect("/columns");
    }

    @GetMapping("/columns/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        ColumnRepo columnRepo = repoFactory.getColumnRepo();
        Column column = columnRepo.find(id);
        model.addAttribute("column", column);
        return "column-edit";
    }

    @PostMapping("/columns/{id}/edit")
    public String editColumn(@PathVariable Long id,
                             @RequestParam String name,
                             @RequestParam(required = false, defaultValue = "0") int order,
                             @RequestParam(required = false, defaultValue = "0") int limit) {
        ColumnRepo columnRepo = repoFactory.getColumnRepo();
        Column column = columnRepo.find(id);
        
        //validation: limite le nom à 25 caractères
        if (name != null && name.length() > 25) {
            name = name.substring(0, 25);
        }
        
        if (column != null) {
            column.setName(name);
            column.setPosition(order);
            column.setMaxCapacity(limit);
            columnRepo.persist(column);
        }
        
        return pathHelper.redirect("/columns");
    }

    @PostMapping("/columns/{id}/delete")
    public String deleteColumn(@PathVariable Long id) {
        ColumnRepo columnRepo = repoFactory.getColumnRepo();
        columnRepo.remove(id);
        return pathHelper.redirect("/columns");
    }

    @PostMapping("/columns/{id}/reorder")
    public String reorderColumn(@PathVariable Long id, @RequestParam(required = false, defaultValue = "0") int newOrder) {
        ColumnRepo columnRepo = repoFactory.getColumnRepo();
        columnRepo.reorder(id, newOrder);
        return pathHelper.redirect("/columns");
    }

    @PostMapping("/stories/{storyId}/move")
    public String moveStory(@PathVariable Long storyId,
                            @RequestParam Long toColumnId,
                            @RequestParam(required = false) Long fromColumnId) {
        ColumnRepo columnRepo = repoFactory.getColumnRepo();
        
        try {
            columnRepo.moveStoryBetweenColumns(storyId, fromColumnId, toColumnId);
        } catch (IllegalStateException e) {
            return pathHelper.redirect("/stories?error=Column is full");
        }
        
        return pathHelper.redirect("/stories");
    }
}
