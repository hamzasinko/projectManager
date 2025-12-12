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
import java.util.List;

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

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        // 1. Create a new project
        driver.get(getBaseUrl() + "project/new");

        String name = "Project " + System.currentTimeMillis();
        driver.findElement(By.id("projectName")).sendKeys(name);
        driver.findElement(By.id("projectDescription")).sendKeys("desc");
        driver.findElement(By.id("createProjectBtn")).click();

        // 2. Open project list
        driver.get(getBaseUrl() + "project/list");

        // 3. Open the first project Edit page
        WebElement projectCard = wait.until(
                ExpectedConditions.elementToBeClickable(By.cssSelector(".card"))
        );
        projectCard.findElement(By.linkText("Edit")).click();

        assertTrue(driver.getCurrentUrl().contains("/project/edit/"));

        // 4. Click "Add Members"
        WebElement addMembersBtn = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("//button[contains(text(),'Add Members')]")
                )
        );
        addMembersBtn.click();

        // 5. Wait for member form to be visible
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("memberForm")));

        // 6. Find first checkbox
        WebElement firstCheckbox = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.cssSelector("#user-list input[type='checkbox']")
                )
        );

        // Checkbox must NOT be checked initially
        assertFalse(firstCheckbox.isSelected(),
                "Initial member checkbox should NOT be checked");

        // Click checkbox
        firstCheckbox.click();
        assertTrue(firstCheckbox.isSelected(),
                "Checkbox should become checked after click");

        // 7. Save changes
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        // 8. Return to project list
        WebElement projectsLink = wait.until(
                ExpectedConditions.elementToBeClickable(By.xpath("//a[text()='Projects']"))
        );
        projectsLink.click();
        wait.until(ExpectedConditions.urlContains("/project/list"));

        // 9. Reopen Edit
        WebElement sameCard = wait.until(
                ExpectedConditions.elementToBeClickable(By.cssSelector(".card"))
        );
        sameCard.findElement(By.linkText("Edit")).click();

        // 10. Reopen Add Members panel
        WebElement addMembersBtn2 = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("//button[contains(text(),'Add Members')]")
                )
        );
        addMembersBtn2.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("memberForm")));

        // 11. Verify checkbox is now prechecked
        WebElement prechecked = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.cssSelector("#user-list input[type='checkbox']")
                )
        );

        assertTrue(prechecked.isSelected(),
                "Checkbox should be prechecked because user is now a member");
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
}
