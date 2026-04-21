package fr.uha.ensisa.gl.tarnished.it;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.*;
import fr.uha.ensisa.eco.metrologie.extension.EcoExtension;
import fr.uha.ensisa.eco.metrologie.extension.annotations.*;

import java.util.List;

@EcoDocker(network = "tarnished-metrologie", clean = true)
@EcoDockerContainer(id = "tarnished-app-1", port = 8080)
@EcoMonitor(containerId = "tarnished-app-1")
@EcoWebDriver(remote = true)
@EcoGatling(userCount = 20, rampDuration = 10)
@ExtendWith(EcoExtension.class)
public class TarnishedTest {

    private static final int PAUSE_COURTE  = 2; // secondes
    private static final int PAUSE_LECTURE = 4;

    /**
     * Scénario : navigation sur le board Kanban de tarnished.
     * Simule un utilisateur qui parcourt la liste des projets
     * et consulte le board.
     */
    @RepeatedTest(5)
    @EcoRunConfig(warmupRepetitions = 1)
    public void navigationBoard(WebDriver driver) throws InterruptedException {
        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Accueil
        driver.get("/gl2526-tarnished/");
        Thread.sleep(PAUSE_COURTE * 1000L);

        // Liste des projets
        driver.get("/gl2526-tarnished/projects");
        Thread.sleep(PAUSE_LECTURE * 1000L);

        // Ouvrir le premier projet dispo
        List<WebElement> liens = driver.findElements(By.cssSelector("a[href*='/projects/']"));
        if (!liens.isEmpty()) {
            liens.get(0).click();
            Thread.sleep(PAUSE_LECTURE * 1000L);

            // Simuler une lecture du board (scroll bas → haut)
            js.executeScript("window.scrollTo(0, document.body.scrollHeight)");
            Thread.sleep(PAUSE_LECTURE * 1000L);
            js.executeScript("window.scrollTo(0, 0)");
            Thread.sleep(PAUSE_COURTE * 1000L);
        }

        // Retour accueil
        driver.get("/gl2526-tarnished/");
        Thread.sleep(PAUSE_COURTE * 1000L);
    }

}
