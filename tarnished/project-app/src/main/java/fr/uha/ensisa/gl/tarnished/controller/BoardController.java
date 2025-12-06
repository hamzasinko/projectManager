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
     * Déplace une story vers une autre colonne
     */
    @PostMapping("/{projectId}/move-story")
    public String moveStory(
            @PathVariable Long projectId,
            @RequestParam Long storyId,
            @RequestParam Long toColumnId,
            @RequestParam(required = false) Long fromColumnId) {
        
        try {
            repoFactory.getColumnRepo().moveStoryBetweenColumns(storyId, fromColumnId, toColumnId);
        } catch (IllegalStateException e) {
            return "redirect:/board/" + projectId + "?error=Column is full";
        }

        return "redirect:/board/" + projectId;
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
}
