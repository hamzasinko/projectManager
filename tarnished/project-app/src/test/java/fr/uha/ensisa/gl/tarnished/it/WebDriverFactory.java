package fr.uha.ensisa.gl.tarnished.it;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Factory pour créer des instances WebDriver configurées pour local et CI/CD.
 */
public class WebDriverFactory {

    /**
     * Crée un WebDriver Chrome configuré pour l'environnement (local ou CI/CD).
     * - Local : ChromeDriver classique.
     * - CI avec -Dselenium.remote.browser=true : RemoteWebDriver vers le service selenium.
     */
    public static WebDriver createChromeDriver() {
        // Flag pour décider local vs remote
        boolean remote = Boolean.getBoolean("selenium.remote.browser");

        ChromeOptions options = new ChromeOptions();

        // Détecter l'environnement CI (GitLab, Jenkins, etc.)
        String ci = System.getenv("CI");
        boolean isCI = ci != null && (ci.equalsIgnoreCase("true") || ci.equals("1"));

        if (isCI) {
            // Configuration pour environnement CI/CD (headless)
            options.addArguments(
                    "--headless=new",
                    "--no-sandbox",
                    "--disable-dev-shm-usage",
                    "--disable-gpu",
                    "--window-size=1920,1080",
                    "--disable-extensions",
                    "--disable-software-rasterizer",
                    "--disable-setuid-sandbox",
                    "--remote-debugging-port=9222",
                    "--disable-background-timer-throttling",
                    "--disable-backgrounding-occluded-windows",
                    "--disable-breakpad",
                    "--disable-component-extensions-with-background-pages",
                    "--disable-features=TranslateUI,BlinkGenPropertyTrees",
                    "--disable-ipc-flooding-protection",
                    "--disable-renderer-backgrounding",
                    "--enable-features=NetworkService,NetworkServiceInProcess",
                    "--force-color-profile=srgb",
                    "--hide-scrollbars",
                    "--metrics-recording-only",
                    "--mute-audio"
            );
            options.setAcceptInsecureCerts(true);
            System.out.println("[WebDriverFactory] Running in CI/CD mode (headless)");
        } else {
            System.out.println("[WebDriverFactory] Running in local mode (with display)");
        }

        if (remote) {
            System.out.println("[WebDriverFactory] Using REMOTE WebDriver (selenium:4444)");
            try {
                URL gridUrl = new URL("http://selenium:4444/wd/hub");
                return new RemoteWebDriver(gridUrl, options);
            } catch (MalformedURLException e) {
                throw new RuntimeException("Invalid Selenium Grid URL", e);
            }
        } else {
            System.out.println("[WebDriverFactory] Using LOCAL ChromeDriver");
            WebDriverManager.chromedriver().setup();
            return new ChromeDriver(options);
        }
    }
}
