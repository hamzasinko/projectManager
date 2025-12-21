package fr.uha.ensisa.gl.tarnished.controller;

import fr.uha.ensisa.gl.entities.Project;
import fr.uha.ensisa.gl.entities.User;
import fr.uha.ensisa.gl.tarnished.config.PathHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import fr.uha.ensisa.gl.tarnished.repos.RepoFactory;
import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/project")
public class ProjectController {
    @Autowired
    public RepoFactory repoFactory;
    
    @Autowired
    private PathHelper pathHelper;
    
    //setter pour les tests
    void setPathHelper(PathHelper pathHelper) {
        this.pathHelper = pathHelper;
    }
    
    //affiche le formulaire de création de projet
    @GetMapping("/new")
    public ModelAndView showCreateForm() {
        return new ModelAndView("project-create");
    }
    
    //traite la création d'un nouveau projet
    @PostMapping("/create")
    public String createProject(
        @RequestParam(required=true) String name,
        @RequestParam(required=false) String description
    ) throws IOException {
        //validation du nom
        if (name == null || name.trim().isEmpty()) {
            return pathHelper.redirect("/project/new?error=Project name is required");
        }
        if (name.length() > 29) {
            return pathHelper.redirect("/project/new?error=Project name must be less than 29 characters");
        }
        
        Project project = new Project();
        if(repoFactory.getUserRepo().getAll().isEmpty()) {
            User user = new User();
            user.setId(1);
            user.setName("user1");
            user.setPassword("password1");
            user.setEmail("email1@gmail.com");
            repoFactory.getUserRepo().add(user);
        }
        project.setName(name);
        project.setDescription(description);
        project.setOwner(repoFactory.getUserRepo().getAll().get(0));
        repoFactory.getProjectRepo().persist(project);
        
        return pathHelper.redirect("/project/list");
    }

    @GetMapping("/info/{id}")
    public ModelAndView showProject(@PathVariable Long id) {
        Project project = repoFactory.getProjectRepo().find(id);

        if (project == null) {
            return pathHelper.redirectView("/project/list"); //projet non trouvé
        }

        ModelAndView mav = new ModelAndView("project-detail");
        mav.addObject("project", project);
        return mav;
    }

    //liste tous les projets existants
    @GetMapping("/list")
    public ModelAndView listProjects() throws IOException {
        ModelAndView mav = new ModelAndView("project-list");

        mav.addObject("projects", repoFactory.getProjectRepo().findAll());
        
        //mock temporaire
        //mav.addObject("projects", Collections.emptyList());
        
        return mav;
    }
    @GetMapping("/edit/{id}")
    public ModelAndView editProject(@PathVariable Long id) {
        Project project = repoFactory.getProjectRepo().find(id);
        if (project == null) {
            return new ModelAndView("redirect:/project/list");
        }
        
        ModelAndView mav = new ModelAndView("project-edit");
        mav.addObject("project", project);
        if(repoFactory.getUserRepo().getAll().isEmpty()) {
            User user = new User();
            user.setId(1);
            user.setName("user1");
            user.setPassword("password1");
            user.setEmail("email1@gmail.com");
            repoFactory.getUserRepo().add(user);
        }
        List<Integer> memberIds = (project != null && project.getMembers() != null) ?
                project.getMembers().stream().map(User::getId).toList() :
                List.of();
        System.out.println("Member IDs: " + memberIds);
        mav.addObject("memberIds", memberIds);
        mav.addObject("users", repoFactory.getUserRepo().getAll());

        return mav;
    }
    @PostMapping("/edit/{id}")
    public String updateProject(
            @PathVariable Long id,
            @RequestParam(required = true) String name,
            @RequestParam(required = true) String description,
            @RequestParam(required = false, defaultValue = "") List<Long> memberIds
    ) {
        //validation du nom
        if (name == null || name.trim().isEmpty()) {
            return pathHelper.redirect("/project/edit/" + id + "?error=Project name is required");
        }
        if (name.length() > 29) {
            return pathHelper.redirect("/project/edit/" + id + "?error=Project name must be less than 29 characters");
        }
        
        Project project = repoFactory.getProjectRepo().find(id);
        if (project == null) {
            return pathHelper.redirect("/project/list");
        }
        
        project.setName(name);
        project.setDescription(description);
        
        //met à jour les membres: si memberIds est vide, aucun membre
        if (memberIds != null && !memberIds.isEmpty()) {
            project.setMembers(
                repoFactory.getUserRepo().getAll()
                    .stream()
                    .filter(user -> memberIds.contains((long)user.getId()))
                    .toList()
            );
        } else {
            project.setMembers(new java.util.ArrayList<>());
        }
        
        repoFactory.getProjectRepo().update(project);
        return pathHelper.redirect("/project/info/" + project.getId());
    }

    @PostMapping("/delete/{id}")
    public String deleteProject(@PathVariable Long id) {
        //supprime d'abord toutes les stories du projet
        repoFactory.getStoryRepo().findByProject(id).forEach(story -> 
            repoFactory.getStoryRepo().remove(story.getId())
        );
        //supprime ensuite toutes les colonnes du projet
        repoFactory.getColumnRepo().findByProject(id).forEach(column -> 
            repoFactory.getColumnRepo().remove((long)column.getId())
        );
        //enfin supprime le projet
        repoFactory.getProjectRepo().remove(id);
        return pathHelper.redirect("/project/list"); //page avec tous les projets
    }

    @GetMapping("/{id}/stories")
    public ModelAndView showProjectStories(@PathVariable Long id) {
        Project project = repoFactory.getProjectRepo().find(id);
        
        if (project == null) {
            return pathHelper.redirectView("/");
        }
        
        ModelAndView mav = new ModelAndView("project-stories");
        mav.addObject("project", project);
        mav.addObject("stories", repoFactory.getStoryRepo().findByProject(id));
        return mav;
    }
}
