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
            options.addArguments("--disable-software-rasterizer");
            options.addArguments("--disable-setuid-sandbox");
            options.addArguments("--remote-debugging-port=9222");
            options.addArguments("--disable-background-timer-throttling");
            options.addArguments("--disable-backgrounding-occluded-windows");
            options.addArguments("--disable-breakpad");
            options.addArguments("--disable-component-extensions-with-background-pages");
            options.addArguments("--disable-features=TranslateUI,BlinkGenPropertyTrees");
            options.addArguments("--disable-ipc-flooding-protection");
            options.addArguments("--disable-renderer-backgrounding");
            options.addArguments("--enable-features=NetworkService,NetworkServiceInProcess");
            options.addArguments("--force-color-profile=srgb");
            options.addArguments("--hide-scrollbars");
            options.addArguments("--metrics-recording-only");
            options.addArguments("--mute-audio");
            options.setAcceptInsecureCerts(true);
            System.out.println("[WebDriverFactory] Running in CI/CD mode (headless)");
        } else {
            System.out.println("[WebDriverFactory] Running in local mode (with display)");
        }
        
        return new ChromeDriver(options);
    }
}

