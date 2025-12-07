package fr.uha.ensisa.gl.tarnished.it;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

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

    @Test
    @DisplayName("Should display edit form and prechecked members")
    public void testEditProjectCheckboxes() {
        // Précondition : créer un nouveau projet
        driver.get(getBaseUrl() + "project/new");

        String name = "Project " + System.currentTimeMillis();
        driver.findElement(By.id("projectName")).sendKeys(name);
        driver.findElement(By.id("projectDescription")).sendKeys("desc");
        driver.findElement(By.id("createProjectBtn")).click();

        // Récupère l’URL générée pour l’édition
        driver.get(getBaseUrl() + "project/list");

        // Clique premier bouton Edit ou Open Board → adapter si besoin
        WebElement projectCard = driver.findElement(By.cssSelector(".card"));
        projectCard.findElement(By.linkText("Edit")).click();

        assertTrue(driver.getCurrentUrl().contains("/project/edit/"));

        // Ouvre la section members
        driver.findElement(By.xpath("//button[contains(text(), 'Members')]")).click();

        // Attends liste users
        WebElement firstCheckbox = driver.findElement(By.cssSelector("input[type='checkbox']"));

        // Vérifie que la checkbox est décochée initialement
        assertFalse(firstCheckbox.isSelected(), "Initial member checkbox should NOT be checked");

        // Coche
        firstCheckbox.click();
        assertTrue(firstCheckbox.isSelected(), "Checkbox should become checked after click");

        // Sauvegarde
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        // Recharge page d'édition :
        // Wait until the "Projects" button is visible and click it
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        WebElement projectsLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[text()='Projects']")
        ));
        projectsLink.click();

// Optionally, wait until the project list page loads
        wait.until(ExpectedConditions.urlContains("/project/list"));

        projectCard = driver.findElement(By.cssSelector(".card"));
        projectCard.findElement(By.linkText("Edit")).click();

        driver.findElement(By.xpath("//button[contains(text(), 'Members')]")).click();

        // Vérifie qu’elle est maintenant pré-cochée
        WebElement prechecked = driver.findElement(By.cssSelector("input[type='checkbox']"));
        assertTrue(prechecked.isSelected(),
                "Checkbox should be prechecked because user is now a member");
    }

    @Test
    @DisplayName("Should update name and description")
    public void testEditProjectUpdateFields() {
        driver.get(getBaseUrl() + "project/new");

        driver.findElement(By.id("projectName")).sendKeys("Old Name");
        driver.findElement(By.id("projectDescription")).sendKeys("Old description");
        driver.findElement(By.id("createProjectBtn")).click();

        driver.get(getBaseUrl() + "project/list");

        WebElement projectCard = driver.findElement(By.cssSelector(".card"));
        projectCard.findElement(By.linkText("Edit")).click();

        WebElement nameInput = driver.findElement(By.name("name"));
        WebElement descInput = driver.findElement(By.name("description"));

        nameInput.clear();
        nameInput.sendKeys("Updated Name");

        descInput.clear();
        descInput.sendKeys("Updated Desc");

        driver.findElement(By.cssSelector("button[type='submit']")).click();

        driver.get(getBaseUrl() + "project/info/1");

        String page = driver.getPageSource();

        assertTrue(page.contains("Updated Name"));
        assertTrue(page.contains("Updated Desc"));
    }

    @Test
    @DisplayName("Should delete a project via UI")
    public void testDeleteProjectUI() {
        // 1. Create a new project
        driver.get(getBaseUrl() + "project/new");

        String projectName = "Selenium Delete " + System.currentTimeMillis();
        driver.findElement(By.id("projectName")).sendKeys(projectName);
        driver.findElement(By.id("projectDescription")).sendKeys("To delete");
        driver.findElement(By.id("createProjectBtn")).click();

        // 2. Go to project list
        driver.get(getBaseUrl() + "project/list");

        // 3. Locate the card containing the newly created project
        WebElement card = driver.findElement(
                By.xpath("//h5[contains(text(),'" + projectName + "')]/ancestor::div[contains(@class,'card')]")
        );

        // 4. Extract project ID from data-id attribute
        String projectIdStr = card.getAttribute("data-id");
        assertNotNull(projectIdStr, "Card should have a data-id attribute");
        long projectId = Long.parseLong(projectIdStr);

        // 5. Click the Delete button
        card.findElement(By.xpath(".//button[contains(text(),'Delete')]")).click();

        // 6. Confirm card is visible
        WebElement confirmCard = driver.findElement(By.id("confirmCard-" + projectId));
        assertTrue(confirmCard.isDisplayed(), "Confirmation card should appear");

        // 7. Click Yes, delete
        confirmCard.findElement(By.xpath(".//button[contains(text(),'Yes')]")).click();

        // 8. Verify project is gone from UI
        String pageSource = driver.getPageSource();
        assertFalse(pageSource.contains(projectName), "Project should be deleted from UI and backend");
    }

    @Test
    @DisplayName("Should display project info correctly in UI")
    public void testProjectInfoUI() {
        // 1. Create a project via UI
        driver.get(getBaseUrl() + "/project/new");
        String projectName = "Selenium Display " + System.currentTimeMillis();
        driver.findElement(By.id("projectName")).sendKeys(projectName);
        driver.findElement(By.id("projectDescription")).sendKeys("Display test");
        driver.findElement(By.id("createProjectBtn")).click();

        // 2. Go to project info page
        driver.get(getBaseUrl() + "/project/list");
        WebElement card = driver.findElement(
                By.xpath("//h5[contains(text(),'" + projectName + "')]/ancestor::div[contains(@class,'card')]")
        );
        card.findElement(By.xpath(".//a[contains(text(),'Details')]")).click();

        // 3. Verify project info page displays the correct data
        WebElement nameElem = driver.findElement(By.id("projectNameInfo"));
        WebElement descElem = driver.findElement(By.id("projectDescriptionInfo"));

        assertEquals(projectName, nameElem.getText());
        assertEquals("Display test", descElem.getText());
    }
}
