package fr.uha.ensisa.gl.tarnished.controller;

import fr.uha.ensisa.gl.entities.Story;
import fr.uha.ensisa.gl.entities.StoryStatus;
import fr.uha.ensisa.gl.tarnished.config.PathHelper;
import fr.uha.ensisa.gl.tarnished.repos.RepoFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.Collection;

@Controller
public class HomeController {

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

	@RequestMapping(value="/")
	public ModelAndView home(){
		ModelAndView mav = new ModelAndView("home");
		mav.addObject("projects", repoFactory.getProjectRepo().findAll());
		
		Collection<Story> allStories = repoFactory.getStoryRepo().findAll();
		mav.addObject("recentStories", allStories);
		
		//calcule les stories en cours
		long inProgressCount = allStories.stream()
			.filter(s -> s.getStatus() == StoryStatus.IN_PROGRESS)
			.count();
		mav.addObject("inProgressCount", inProgressCount);
		
		return mav;
	}

	@RequestMapping(value="/hello")
	public String hello() {
		return pathHelper.redirect("/");
	}
}
