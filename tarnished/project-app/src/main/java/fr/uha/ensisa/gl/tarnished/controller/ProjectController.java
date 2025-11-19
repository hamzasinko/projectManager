package fr.uha.ensisa.gl.tarnished.controller;

import fr.uha.ensisa.gl.entities.Project;
import fr.uha.ensisa.gl.tarnished.mems.RepoFactoryMem;
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
    public RepoFactory repoFactory;
    
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
        Project project = new Project();
        project.setName(name);
        project.setDescription(description);
        repoFactory.getProjectRepo().persist(project);
        
        return "redirect:/project/list";
    }
    
    /**
     * Liste tous les projets existants
     */
    @GetMapping("/list")
    public ModelAndView listProjects() throws IOException {
        ModelAndView mav = new ModelAndView("project-list");

        mav.addObject("projects", repoFactory.getProjectRepo().findAll());
        
        // Mock temporaire
        //mav.addObject("projects", Collections.emptyList());
        
        return mav;
    }
}
