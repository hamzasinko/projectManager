package fr.uha.ensisa.gl.tarnished.controller;

import fr.uha.ensisa.gl.entities.Swimlane;
import fr.uha.ensisa.gl.entities.Project;
import fr.uha.ensisa.gl.tarnished.config.PathHelper;
import fr.uha.ensisa.gl.tarnished.repos.ProjectRepo;
import fr.uha.ensisa.gl.tarnished.repos.RepoFactory;
import fr.uha.ensisa.gl.tarnished.repos.StoryRepo;
import fr.uha.ensisa.gl.tarnished.repos.SwimlaneRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/swimlane")
public class SwimlaneController {

    @Autowired
    private RepoFactory repoFactory;

    @Autowired
    private PathHelper pathHelper;

    //setter pour les tests
    void setPathHelper(PathHelper pathHelper) {
        this.pathHelper = pathHelper;
    }

    @GetMapping("/new")
    public ModelAndView newSwimlaneForm(@RequestParam(required = false) Integer projectId) {
        ProjectRepo projectRepo = repoFactory.getProjectRepo();

        if (projectId == null || projectRepo.find(projectId) == null) {
            return pathHelper.redirectView("/");
        }

        ModelAndView mav = new ModelAndView("swimlane-create");
        mav.addObject("projectId", projectId);
        return mav;
    }

    @PostMapping("/create")
    public ModelAndView createSwimlane(
            @RequestParam(required = true) String name,
            @RequestParam(required = false) Long projectId
    ) {
        SwimlaneRepo swimlaneRepo = repoFactory.getSwimlaneRepo();
        ProjectRepo projectRepo = repoFactory.getProjectRepo();
        StoryRepo storyRepo = repoFactory.getStoryRepo();

        if (projectId == null || projectRepo.find(projectId) == null) {
            return pathHelper.redirectView("/");
        }

        Swimlane swimlane = new Swimlane();
        swimlane.setName(name);
        swimlane.setProjectId(projectId.intValue());
        swimlaneRepo.persist(swimlane);

        Project project = projectRepo.find(projectId);

        if (project.getSwimlanes() == null) {
            project.setSwimlanes(new ArrayList<>());
        }

        boolean isFirstSwimlane = project.getSwimlanes().isEmpty();

        project.getSwimlanes().add(swimlane);
        projectRepo.update(project);

        if (isFirstSwimlane) {
            storyRepo.findAll().stream()
                    .filter(s -> s.getProjectId() != null && s.getProjectId().equals(projectId))
                    .forEach(s -> {
                        s.setSwimlaneId(swimlane.getId());
                        storyRepo.persist(s);
                    });
        }

        return pathHelper.redirectView("/board/" + projectId);
    }

    @PostMapping("/delete/{id}")
    public ModelAndView deleteSwimlane(@PathVariable long id,
            @RequestParam(required = false) Long targetSwimlaneId) {
        SwimlaneRepo swimlaneRepo = repoFactory.getSwimlaneRepo();
        ProjectRepo projectRepo = repoFactory.getProjectRepo();
        StoryRepo storyRepo = repoFactory.getStoryRepo();

        Swimlane swimlane = swimlaneRepo.find(id);
        if (swimlane == null) {
            return pathHelper.redirectView("/");
        }

        Long projectId = (long) swimlane.getProjectId();
        Project project = projectRepo.find(projectId);
        if (project == null) {
            return pathHelper.redirectView("/");
        }

        swimlaneRepo.remove(id);
        project.getSwimlanes().removeIf(s -> s.getId() == id);

        List<Swimlane> remainingSwimlanes = project.getSwimlanes();
        storyRepo.findAll().stream()
                .filter(story -> story.getProjectId() != null && story.getProjectId().equals(projectId))
                .filter(story -> story.getSwimlaneId() != null && story.getSwimlaneId() == id)
                .forEach(story -> {
                    if (!remainingSwimlanes.isEmpty()) {
                        story.setSwimlaneId(targetSwimlaneId);
                    } else {
                        story.setSwimlaneId(null);
                    }
                    storyRepo.persist(story);
                });

        projectRepo.update(project);

        return pathHelper.redirectView("/board/" + projectId);
    }

    @GetMapping("/edit/{id}")
    @ResponseBody
    public Map<String, Object> editSwimlane(@PathVariable long id) {
        SwimlaneRepo swimlaneRepo = repoFactory.getSwimlaneRepo();
        Swimlane swimlane = swimlaneRepo.find(id);

        if (swimlane == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Swimlane not found");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", swimlane.getId());
        response.put("name", swimlane.getName());
        response.put("projectId", swimlane.getProjectId()); // needed for redirect

        return response;
    }

    @PostMapping("/update")
    public ModelAndView updateSwimlane(
            @RequestParam long id,
            @RequestParam String name
    ) {
        SwimlaneRepo swimlaneRepo = repoFactory.getSwimlaneRepo();
        ProjectRepo projectRepo = repoFactory.getProjectRepo();

        Swimlane swimlane = swimlaneRepo.find(id);
        if (swimlane == null) {
            return pathHelper.redirectView("/");
        }

        swimlane.setName(name);
        swimlaneRepo.persist(swimlane);

        Project project = projectRepo.find((long) swimlane.getProjectId());
        if (project != null && project.getSwimlanes() != null) {
            project.getSwimlanes().replaceAll(s -> s.getId() == id ? swimlane : s);
            projectRepo.update(project);
        }

        return pathHelper.redirectView("/board/" + swimlane.getProjectId());
    }
}
