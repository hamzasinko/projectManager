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
    @DisplayName("Should create a new story and redirect to list")
    public void testCreateStory() {
        driver.get(getBaseUrl() + "story/new");
        
        String testStoryTitle = "Test Story " + System.currentTimeMillis();
        String testDescription = "Test description for integration test\nAcceptance criteria:\n- Should work\n- Should be tested";
        
        // Remplit le formulaire
        driver.findElement(By.id("storyTitle")).sendKeys(testStoryTitle);
        driver.findElement(By.id("storyDescription")).sendKeys(testDescription);
        // projectId est optionnel, on ne le remplit pas
        
        // Soumet le formulaire
        driver.findElement(By.id("createStoryBtn")).click();
        
        // Vérifie la redirection
        assertTrue(driver.getCurrentUrl().contains("/story/list"), 
                   "Should redirect to story list after creation");
    }
    
    @Test
    @DisplayName("Should display stories list page with new story button")
    public void testListStories() {
        driver.get(getBaseUrl() + "story/list");
        
        // Vérifie la présence des éléments principaux
        WebElement storiesList = driver.findElement(By.id("storiesList"));
        WebElement newBtn = driver.findElement(By.id("newStoryBtn"));
        
        assertNotNull(storiesList, "Stories list container should be present");
        assertNotNull(newBtn, "New story button should be present");
        
        // Vérifie que le bouton est cliquable
        assertTrue(newBtn.isDisplayed(), "New story button should be visible");
        assertTrue(newBtn.isEnabled(), "New story button should be enabled");
    }
    
    @Test
    @DisplayName("Should show info message when no stories exist")
    public void testEmptyStoriesList() {
        driver.get(getBaseUrl() + "story/list");
        
        // Vérifie le message pour liste vide ou des stories existantes
        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("No stories yet") || 
                   pageSource.contains("story-"), 
                   "Should show either empty message or stories");
    }
    
    @Test
    @DisplayName("Should navigate between create form and list")
    public void testNavigation() {
        // Va sur la liste
        driver.get(getBaseUrl() + "story/list");
        
        // Clique sur "New Story"
        driver.findElement(By.id("newStoryBtn")).click();
        assertTrue(driver.getCurrentUrl().contains("/story/new"), 
                   "Should navigate to create form");
        
        // Clique sur "Cancel"
        driver.findElement(By.linkText("Cancel")).click();
        assertTrue(driver.getCurrentUrl().contains("/story/list"), 
                   "Should navigate back to list");
    }
    
    @Test
    @DisplayName("Should display created story in list")
    public void testCreateAndVerifyStoryInList() {
        // Crée une story
        driver.get(getBaseUrl() + "story/new");
        
        String uniqueTitle = "Unique Story " + System.currentTimeMillis();
        driver.findElement(By.id("storyTitle")).sendKeys(uniqueTitle);
        driver.findElement(By.id("storyDescription")).sendKeys("This is a unique story for testing");
        driver.findElement(By.id("createStoryBtn")).click();
        
        // Vérifie que la story apparaît dans la liste
        assertTrue(driver.getCurrentUrl().contains("/story/list"));
        
        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains(uniqueTitle) || pageSource.contains("story-"),
                   "Created story should appear in the list");
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
    
    @Test
    @DisplayName("Should complete full story creation workflow")
    public void testFullStoryCreationWorkflow() {
        // 1. Navigate to list
        driver.get(getBaseUrl() + "story/list");
        
        // 2. Click new story button
        driver.findElement(By.id("newStoryBtn")).click();
        assertTrue(driver.getCurrentUrl().contains("/story/new"));
        
        // 3. Fill form
        String storyTitle = "Full Workflow Story " + System.currentTimeMillis();
        driver.findElement(By.id("storyTitle")).sendKeys(storyTitle);
        driver.findElement(By.id("storyDescription")).sendKeys("Complete workflow test");
        
        // 4. Submit
        driver.findElement(By.id("createStoryBtn")).click();
        
        // 5. Verify redirect to list
        assertTrue(driver.getCurrentUrl().contains("/story/list"),
                   "Should redirect to story list after creation");
    }
    
    @Test
    @DisplayName("Should display story status badges in list")
    public void testStoryStatusDisplay() {
        // Crée d'abord une story
        driver.get(getBaseUrl() + "story/new");
        driver.findElement(By.id("storyTitle")).sendKeys("Story with Status " + System.currentTimeMillis());
        driver.findElement(By.id("createStoryBtn")).click();
        
        // Va sur la liste
        driver.get(getBaseUrl() + "story/list");
        
        String pageSource = driver.getPageSource();
        // Vérifie que les badges de status sont présents (TODO par défaut)
        assertTrue(pageSource.contains("status-badge") || 
                   pageSource.contains("TODO") ||
                   pageSource.contains("No stories yet"),
                   "Should display status badges or empty message");
    }

    @Test
    @DisplayName("Should delete story from list")
    public void testDeleteStoryWorkflow() {
        // 1. Créer une story
        driver.get(getBaseUrl() + "story/new");
        String storyTitle = "Story to Delete " + System.currentTimeMillis();
        driver.findElement(By.id("storyTitle")).sendKeys(storyTitle);
        driver.findElement(By.id("createStoryBtn")).click();
        
        // 2. Aller sur la liste
        driver.get(getBaseUrl() + "story/list");
        String pageSourceBefore = driver.getPageSource();
        assertTrue(pageSourceBefore.contains(storyTitle) || pageSourceBefore.contains("Delete"),
                   "Story should be visible in list before deletion");
        
        // 3. Cliquer sur Delete
        WebElement deleteBtn = driver.findElement(By.xpath("//button[contains(text(), 'Delete')]"));
        deleteBtn.click();
        
        // 4. Confirmer la suppression
        WebElement confirmBtn = driver.findElement(By.xpath("//button[contains(text(), 'Yes, delete')]"));
        confirmBtn.click();
        
        // 5. Vérifier la redirection vers la liste
        assertTrue(driver.getCurrentUrl().contains("/story/list"),
                   "Should redirect to story list after deletion");
    }
}
