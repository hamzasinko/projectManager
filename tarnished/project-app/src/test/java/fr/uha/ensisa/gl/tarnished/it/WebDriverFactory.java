package fr.uha.ensisa.gl.tarnished.it;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Factory pour créer des instances WebDriver configurées pour local et CI/CD.
 *
 * En CI, lorsque le service Selenium est utilisé (alias "selenium"),
 * on DOIT utiliser un RemoteWebDriver connecté à http://selenium:4444/wd/hub
 * comme recommandé dans la documentation officielle.
 */
public class WebDriverFactory {

    public static WebDriver createChromeDriver() {

        ChromeOptions options = new ChromeOptions();

        //Options communes
        options.addArguments(
                "--headless=new",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--window-size=1920,1080"
        );
        options.setAcceptInsecureCerts(true);

        //Détection explicite de la CI GitLab
        String ciEnv = System.getenv("CI");
        boolean isCI = ciEnv != null && (ciEnv.equalsIgnoreCase("true") || ciEnv.equals("1"));

        //On active le RemoteWebDriver UNIQUEMENT si selenium.remote.browser=true
        String remoteFlag = System.getProperty("selenium.remote.browser", "false");
        boolean useRemote = remoteFlag.equalsIgnoreCase("true") || remoteFlag.equals("1");

        if (useRemote) {
            String seleniumUrl = System.getProperty(
                    "selenium.remote.url",
                    "http://selenium:4444/wd/hub"
            );
            try {
                System.out.println("[WebDriverFactory] Remote mode → " + seleniumUrl);
                return new RemoteWebDriver(new URI(seleniumUrl).toURL(), options);
            } catch (MalformedURLException | URISyntaxException e) {
                throw new RuntimeException("Invalid Selenium Grid URL: " + seleniumUrl, e);
            }
        }

        //Si on est en CI SANS Selenium distant, on utilise le chromedriver système installé via apk
        if (isCI) {
            String chromeBin = System.getenv("CHROME_BIN");
            if (chromeBin != null && !chromeBin.isBlank()) {
                System.out.println("[WebDriverFactory] CI local mode → ChromeDriver with binary " + chromeBin);
                options.setBinary(chromeBin);
            } else {
                System.out.println("[WebDriverFactory] CI local mode → ChromeDriver (binary from PATH)");
            }
            //Chemin standard du paquet alpine chromium-chromedriver
            System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver");
            return new ChromeDriver(options);
        }

        //Sinon : ChromeDriver local (développement, hors CI)
        System.out.println("[WebDriverFactory] Local mode → ChromeDriver (WebDriverManager)");
        WebDriverManager.chromedriver().setup();
        return new ChromeDriver(options);
    }
}