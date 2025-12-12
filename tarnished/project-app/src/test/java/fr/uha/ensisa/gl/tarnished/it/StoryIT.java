package fr.uha.ensisa.gl.tarnished.it;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Tests d'intégration Selenium pour la gestion des stories
 * Teste l'application déployée dans Jetty avec un vrai navigateur
 */
public class StoryIT {
    
    public static WebDriver driver;
    private static String host, port;
    
    @BeforeAll
    public static void setupWebDriver() {
        if (driver != null) return;
        
        host = System.getProperty("host", "localhost");
        port = System.getProperty("servlet.port", "8080");
        
        WebDriverManager.chromedriver().setup();
        driver = new ChromeDriver();
    }
    
    @AfterAll
    public static void shutdownWebDriver() {
        if (driver != null) {
            driver.quit();
            try {
                driver.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            driver = null;
        }
    }
    
    public static String getBaseUrl() {
        return "http://" + host + ":" + port + "/";
    }
    
    @Test
    @DisplayName("Should display create story form with all required fields")
    public void testShowCreateForm() {
        driver.get(getBaseUrl() + "story/new");
        
        // Vérifie la présence du formulaire
        WebElement titleInput = driver.findElement(By.id("storyTitle"));
        WebElement descriptionInput = driver.findElement(By.id("storyDescription"));
        WebElement projectSelect = driver.findElement(By.id("projectId"));
        WebElement createBtn = driver.findElement(By.id("createStoryBtn"));
        
        assertNotNull(titleInput, "Title input should be present");
        assertNotNull(descriptionInput, "Description input should be present");
        assertNotNull(projectSelect, "Project select should be present");
        assertNotNull(createBtn, "Create button should be present");
        
        // Vérifie que le champ title est required
        assertEquals("text", titleInput.getAttribute("type"));
        assertTrue(titleInput.getAttribute("required") != null, "Title field should be required");
    }

    @Test
    @DisplayName("Should handle form validation for empty title")
    public void testCreateStoryWithEmptyTitle() {
        driver.get(getBaseUrl() + "story/new");
        
        // Laisse le titre vide mais remplit la description
        driver.findElement(By.id("storyDescription")).sendKeys("Description without title");
        
        // Tente de soumettre (HTML5 validation devrait empêcher)
        WebElement titleInput = driver.findElement(By.id("storyTitle"));
        
        // Vérifie que le champ est required
        assertTrue(titleInput.getAttribute("required") != null,
                   "Title field should be marked as required");
    }
    
    @Test
    @DisplayName("Should display project select dropdown")
    public void testProjectSelectDropdown() {
        driver.get(getBaseUrl() + "story/new");
        
        WebElement projectSelect = driver.findElement(By.id("projectId"));
        assertNotNull(projectSelect, "Project select should exist");
        
        // Vérifie qu'il y a au moins l'option "No project"
        String selectHtml = projectSelect.getAttribute("outerHTML");
        assertTrue(selectHtml.contains("option"), "Select should have options");
    }
}
