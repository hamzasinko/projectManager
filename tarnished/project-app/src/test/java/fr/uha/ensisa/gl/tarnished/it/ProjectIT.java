package fr.uha.ensisa.gl.tarnished.it;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Tests d'intégration Selenium pour la gestion des projets
 * Teste l'application déployée dans Jetty avec un vrai navigateur
 */
public class ProjectIT {
    
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
    @DisplayName("Should display create project form with all required fields")
    public void testShowCreateForm() {
        driver.get(getBaseUrl() + "project/new");
        
        //Vérifie la présence du formulaire
        WebElement nameInput = driver.findElement(By.id("projectName"));
        WebElement descriptionInput = driver.findElement(By.id("projectDescription"));
        WebElement createBtn = driver.findElement(By.id("createProjectBtn"));
        
        assertNotNull(nameInput, "Name input should be present");
        assertNotNull(descriptionInput, "Description input should be present");
        assertNotNull(createBtn, "Create button should be present");
        
        // Vérifie que le champ name est required
        assertEquals("text", nameInput.getAttribute("type"));
        assertTrue(nameInput.getAttribute("required") != null, "Name field should be required");
    }
    
    @Test
    @DisplayName("Should create a new project and redirect to list")
    public void testCreateProject() {
        driver.get(getBaseUrl() + "project/new");
        
        String testProjectName = "Test Project " + System.currentTimeMillis();
        String testDescription = "Test description for integration test";
        
        //Remplit le formulaire
        driver.findElement(By.id("projectName")).sendKeys(testProjectName);
        driver.findElement(By.id("projectDescription")).sendKeys(testDescription);
        
        //Soumet le formulaire
        driver.findElement(By.id("createProjectBtn")).click();
        
        //Vérifie la redirection
        assertTrue(driver.getCurrentUrl().contains("/project/list"), 
                   "Should redirect to project list after creation");
    }
    
    @Test
    @DisplayName("Should display projects list page with new project button")
    public void testListProjects() {
        driver.get(getBaseUrl() + "project/list");
        
        //Vérifie la présence des éléments principaux
        WebElement projectsList = driver.findElement(By.id("projectsList"));
        WebElement newBtn = driver.findElement(By.id("newProjectBtn"));
        
        assertNotNull(projectsList, "Projects list container should be present");
        assertNotNull(newBtn, "New project button should be present");
        
        //Vérifie que le bouton est cliquable
        assertTrue(newBtn.isDisplayed(), "New project button should be visible");
        assertTrue(newBtn.isEnabled(), "New project button should be enabled");
    }
    
    @Test
    @DisplayName("Should show info message when no projects exist")
    public void testEmptyProjectsList() {
        driver.get(getBaseUrl() + "project/list");
        
        // Vérifie le message pour liste vide (tant que mock retourne emptyList)
        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("No projects yet") || 
                   pageSource.contains("project-"), 
                   "Should show either empty message or projects");
    }
    
    @Test
    @DisplayName("Should navigate between create form and list")
    public void testNavigation() {
        //Va sur la liste
        driver.get(getBaseUrl() + "project/list");
        
        //Clique sur "New Project"
        driver.findElement(By.id("newProjectBtn")).click();
        assertTrue(driver.getCurrentUrl().contains("/project/new"), 
                   "Should navigate to create form");
        
        //Clique sur "Cancel"
        driver.findElement(By.linkText("Cancel")).click();
        assertTrue(driver.getCurrentUrl().contains("/project/list"), 
                   "Should navigate back to list");
    }
}
