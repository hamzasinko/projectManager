package fr.uha.ensisa.gl.tarnished.controller;

import fr.uha.ensisa.gl.entities.Story;
import fr.uha.ensisa.gl.entities.StoryStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import fr.uha.ensisa.gl.tarnished.repos.RepoFactory;

import java.io.IOException;
import java.util.Date;

@Controller
@RequestMapping("/story")
public class StoryController {
    
    @Autowired
    public RepoFactory repoFactory;
    
    /**
     * Affiche le formulaire de création de story
     */
    @GetMapping("/new")
    public ModelAndView showCreateForm() {
        ModelAndView mav = new ModelAndView("story-create");
        // Add all projects to select from
        mav.addObject("projects", repoFactory.getProjectRepo().findAll());
        return mav;
    }
    
    /**
     * Traite la création d'une nouvelle story
     */
    @PostMapping("/create")
    public String createStory(
        @RequestParam(required=true) String title,
        @RequestParam(required=false) String description,
        @RequestParam(required=false) Long projectId
    ) throws IOException {
        
        // Validate title
        if (title == null || title.trim().isEmpty()) {
            return "redirect:/story/new?error=Title is required";
        }
        
        Story story = new Story();
        story.setTitle(title);
        story.setDescription(description);
        story.setStatus(StoryStatus.TODO);
        story.setDateCreated(new Date());
        
        repoFactory.getStoryRepo().persist(story);
        
        return "redirect:/story/list";
    }
    
    /**
     * Liste toutes les stories existantes
     */
    @GetMapping("/list")
    public ModelAndView listStories() throws IOException {
        ModelAndView mav = new ModelAndView("story-list");
        mav.addObject("stories", repoFactory.getStoryRepo().findAll());
        return mav;
    }
    
    /**
     * Affiche les détails d'une story
     */
    @GetMapping("/{id}")
    public ModelAndView showStory(@PathVariable long id) throws IOException {
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
    public ModelAndView editStory(@PathVariable long id) {
        ModelAndView mav = new ModelAndView("story-edit");
        Story story = repoFactory.getStoryRepo().find(id);
        
        if (story == null) {
            return new ModelAndView("redirect:/story/list");
        }
        
        mav.addObject("story", story);
        mav.addObject("users", repoFactory.getUserRepo().getAll());
        return mav;
    }
    
    /**
     * Traite la mise à jour d'une story
     */
    @PostMapping("/{id}/edit")
    public String updateStory(
        @PathVariable long id,
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
        
        story.setTitle(title);
        story.setDescription(description);
        
        if (status != null && !status.isEmpty()) {
            try {
                story.setStatus(StoryStatus.valueOf(status));
            } catch (IllegalArgumentException e) {
                // Invalid status, keep current status
            }
        }
        
        // Note: StoryRepoMem doesn't have update method, but since objects are references
        // the changes are automatically persisted in memory
        
        return "redirect:/story/" + id;
    }
    
    /**
     * Supprime une story
     */
    @PostMapping("/{id}/delete")
    public String deleteStory(@PathVariable long id) {
        repoFactory.getStoryRepo().remove(id);
        return "redirect:/story/list";
    }
}
