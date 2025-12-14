package fr.uha.ensisa.gl.tarnished.it;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

/**
 * Factory pour créer des instances WebDriver configurées pour local et CI/CD.
 */
public class WebDriverFactory {

    public static WebDriver createChromeDriver() {

        ChromeOptions options = new ChromeOptions();

        // Détecter l'environnement CI (GitLab)
        String ci = System.getenv("CI");
        boolean isCI = ci != null && (ci.equalsIgnoreCase("true") || ci.equals("1"));

        if (isCI) {
            // Configuration CI stable
            options.addArguments(
                    "--headless=new",
                    "--no-sandbox",
                    "--disable-dev-shm-usage",
                    "--window-size=1920,1080"
            );
            options.setAcceptInsecureCerts(true);
            System.out.println("[WebDriverFactory] CI mode (headless)");
        } else {
            System.out.println("[WebDriverFactory] Local mode");
        }

        WebDriverManager.chromedriver().setup();
        return new ChromeDriver(options);
    }
}