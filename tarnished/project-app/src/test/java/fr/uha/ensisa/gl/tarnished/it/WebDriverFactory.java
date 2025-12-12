package fr.uha.ensisa.gl.tarnished.it;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

/**
 * Factory pour créer des instances WebDriver configurées pour CI/CD
 */
public class WebDriverFactory {
    
    /**
     * Crée un WebDriver Chrome configuré pour l'environnement (local ou CI/CD)
     * En CI/CD (détecté via variable d'environnement CI), utilise le mode headless
     */
    public static WebDriver createChromeDriver() {
        WebDriverManager.chromedriver().setup();
        
        ChromeOptions options = new ChromeOptions();
        
        // Détecter l'environnement CI (GitLab, Jenkins, etc.)
        String ci = System.getenv("CI");
        boolean isCI = ci != null && (ci.equalsIgnoreCase("true") || ci.equals("1"));
        
        if (isCI) {
            // Configuration pour environnement CI/CD (headless)
            options.addArguments("--headless=new");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-gpu");
            options.addArguments("--window-size=1920,1080");
            options.addArguments("--disable-extensions");
            options.addArguments("--proxy-server='direct://'");
            options.addArguments("--proxy-bypass-list=*");
            options.addArguments("--start-maximized");
            System.out.println("[WebDriverFactory] Running in CI/CD mode (headless)");
        } else {
            System.out.println("[WebDriverFactory] Running in local mode (with display)");
        }
        
        return new ChromeDriver(options);
    }
}

