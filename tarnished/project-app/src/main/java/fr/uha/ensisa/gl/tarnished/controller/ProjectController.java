package fr.uha.ensisa.gl.tarnished.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import fr.uha.ensisa.gl.tarnished.repos.RepoFactory;
import java.io.IOException;
import java.util.Collections;

@Controller
@RequestMapping("/project")
public class ProjectController {
    
    @Autowired
    public RepoFactory repoFactory; // Public pour Mockito
    
    /**
     * Affiche le formulaire de création de projet
     */
    @GetMapping("/new")
    public ModelAndView showCreateForm() {
        return new ModelAndView("project-create");
    }
    
    /**
     * Traite la création d'un nouveau projet
     */
    @PostMapping("/create")
    public String createProject(
        @RequestParam(required=true) String name,
        @RequestParam(required=false) String description
    ) throws IOException {
        // TODO: Backend - uncomment when ProjectRepo ready
        // Project project = new Project(name, description);
        // repoFactory.getProjectRepo().persist(project);
        
        return "redirect:/project/list";
    }
    
    /**
     * Liste tous les projets existants
     */
    @GetMapping("/list")
    public ModelAndView listProjects() throws IOException {
        ModelAndView mav = new ModelAndView("project-list");
        
        // TODO: Backend - uncomment when ProjectRepo ready
        // mav.addObject("projects", repoFactory.getProjectRepo().findAll());
        
        // Mock temporaire
        mav.addObject("projects", Collections.emptyList());
        
        return mav;
    }
}
