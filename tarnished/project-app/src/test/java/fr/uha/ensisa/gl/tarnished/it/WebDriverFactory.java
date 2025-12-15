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

        // Options communes
        options.addArguments(
                "--headless=new",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--window-size=1920,1080"
        );
        options.setAcceptInsecureCerts(true);

        // On active le RemoteWebDriver UNIQUEMENT si selenium.remote.browser=true
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

        // Sinon : ChromeDriver local (dev et CI sans service Selenium)
        System.out.println("[WebDriverFactory] Local mode → ChromeDriver");
        WebDriverManager.chromedriver().setup();
        return new ChromeDriver(options);
    }
}