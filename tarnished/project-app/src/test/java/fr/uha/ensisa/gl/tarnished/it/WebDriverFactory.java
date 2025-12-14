package fr.uha.ensisa.gl.tarnished.it;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.*;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.URL;

public class WebDriverFactory {

    public static WebDriver createChromeDriver() {

        ChromeOptions options = new ChromeOptions();
        boolean isCI = "true".equalsIgnoreCase(System.getenv("CI"));

        options.addArguments(
                "--headless=new",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--window-size=1920,1080"
        );
        options.setAcceptInsecureCerts(true);

        if (isCI) {
            try {
                System.out.println("[WebDriver] CI → RemoteWebDriver");
                return new RemoteWebDriver(
                        new URL("http://selenium:4444/wd/hub"),
                        options
                );
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        System.out.println("[WebDriver] Local → ChromeDriver");
        WebDriverManager.chromedriver().setup();
        return new ChromeDriver(options);
    }
}