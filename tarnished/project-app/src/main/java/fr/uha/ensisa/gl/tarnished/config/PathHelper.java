package fr.uha.ensisa.gl.tarnished.config;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * Service utilitaire pour gérer les chemins de l'application de manière dynamique.
 * Détecte automatiquement le contexte path depuis la requête HTTP pour éviter
 * les chemins codés en dur et permettre le déploiement sous différents chemins.
 */
@Component
public class PathHelper {

    /**
     * Récupère le contexte path de base de l'application depuis la requête HTTP.
     * Par exemple : "/gl2526-tarnished" ou "/" si déployé à la racine.
     * 
     * @return Le contexte path avec un slash final (ex: "/gl2526-tarnished/" ou "/")
     */
    public String getContextPath() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String contextPath = request.getContextPath();
            // S'assure qu'il y a un slash final
            return contextPath.isEmpty() ? "/" : (contextPath.endsWith("/") ? contextPath : contextPath + "/");
        }
        // Fallback si pas de requête (tests unitaires)
        return "/";
    }

    /**
     * Construit un chemin complet en ajoutant le contexte path au chemin relatif.
     * 
     * @param relativePath Le chemin relatif (ex: "project/list" ou "/project/list")
     * @return Le chemin complet avec le contexte (ex: "/gl2526-tarnished/project/list")
     */
    public String buildPath(String relativePath) {
        String contextPath = getContextPath();
        // Enlève le slash final du contexte path pour éviter les doubles slashes
        String base = contextPath.equals("/") ? "" : contextPath.substring(0, contextPath.length() - 1);
        
        // Normalise le chemin relatif
        if (relativePath == null || relativePath.isEmpty()) {
            return base + "/";
        }
        
        // Si le chemin commence déjà par /, on l'utilise tel quel
        if (relativePath.startsWith("/")) {
            return base + relativePath;
        }
        
        // Sinon, on ajoute le slash
        return base + "/" + relativePath;
    }

    /**
     * Construit une redirection Spring MVC.
     * Spring MVC gère automatiquement le contexte path pour les redirections "redirect:/...",
     * donc on retourne simplement le chemin avec le préfixe redirect:.
     * 
     * @param relativePath Le chemin relatif (ex: "project/list" ou "/project/list")
     * @return La chaîne de redirection (ex: "redirect:/project/list")
     */
    public String redirect(String relativePath) {
        // Spring MVC gère automatiquement le contexte path
        if (relativePath == null || relativePath.isEmpty()) {
            return "redirect:/";
        }
        // S'assure que le chemin commence par /
        if (!relativePath.startsWith("/")) {
            return "redirect:/" + relativePath;
        }
        return "redirect:" + relativePath;
    }

    /**
     * Construit une redirection ModelAndView.
     * 
     * @param relativePath Le chemin relatif (ex: "project/list" ou "/project/list")
     * @return Un ModelAndView avec la redirection correcte
     */
    public org.springframework.web.servlet.ModelAndView redirectView(String relativePath) {
        return new org.springframework.web.servlet.ModelAndView(redirect(relativePath));
    }
}
