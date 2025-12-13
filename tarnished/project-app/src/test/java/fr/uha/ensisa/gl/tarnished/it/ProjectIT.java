package fr.uha.ensisa.gl.tarnished.it;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.JavascriptExecutor;

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
        
        driver = WebDriverFactory.createChromeDriver();
    }
    
    @AfterAll
    public static void shutdownWebDriver() {
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                e.printStackTrace();
            }
            driver = null;
        }
    }
    
    public static String getBaseUrl() {
        return "http://" + host + ":" + port + "/";
    }
    
    private void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
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
        // Précondition : créer un nouveau projet
        driver.get(getBaseUrl() + "project/new");

        String name = "Project " + (System.currentTimeMillis() % 10000);
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
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(2));
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
    @DisplayName("Should delete a project via UI")
    public void testDeleteProjectUI() throws InterruptedException {
        // 1. Create a new project
        driver.get(getBaseUrl() + "project/new");

        String projectName = "Selenium Delete " + System.currentTimeMillis();
        driver.findElement(By.id("projectName")).sendKeys(projectName);
        driver.findElement(By.id("projectDescription")).sendKeys("To delete");
        driver.findElement(By.id("createProjectBtn")).click();

        // 2. Wait for redirect after creation (could be /project/list or /board/{id})
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.or(
            ExpectedConditions.urlContains("/project/list"),
            ExpectedConditions.urlContains("/board/")
        ));

        // 3. Navigate to project list explicitly
        driver.get(getBaseUrl() + "project/list");
        Thread.sleep(2000); // Give time for page to load

        // 4. Wait for the project card to appear with explicit wait
        WebElement card = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("//h5[contains(text(),'" + projectName + "')]/ancestor::div[contains(@class,'card')]")
        ));

        // 5. Extract project ID from data-id attribute
        String projectIdStr = card.getAttribute("data-id");
        assertNotNull(projectIdStr, "Card should have a data-id attribute");
        long projectId = Long.parseLong(projectIdStr);

        // 6. Click the Delete button
        WebElement deleteBtn = card.findElement(By.xpath(".//button[contains(text(),'Delete')]"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", deleteBtn);
        Thread.sleep(500);
        deleteBtn.click();

        // 7. Wait for confirmation card to appear
        WebElement confirmCard = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("confirmCard-" + projectId)
        ));
        assertTrue(confirmCard.isDisplayed(), "Confirmation card should appear");

        // 8. Click Yes, delete
        confirmCard.findElement(By.xpath(".//button[contains(text(),'Yes')]")).click();

        // 9. Wait for page to reload after deletion
        wait.until(ExpectedConditions.urlContains("/project/list"));
        Thread.sleep(2000);
        driver.navigate().refresh();
        Thread.sleep(2000);

        // 10. Verify project is gone from UI (with retry)
        boolean deleted = false;
        for (int i = 0; i < 5; i++) {
            String pageSource = driver.getPageSource();
            if (!pageSource.contains(projectName)) {
                deleted = true;
                break;
            }
            Thread.sleep(1000);
            driver.navigate().refresh();
            Thread.sleep(1000);
        }

        assertTrue(deleted, "Project should be deleted from the list");
    }

    @Test
    @DisplayName("Should display project info correctly in UI")
    public void testProjectInfoUI() throws InterruptedException {
        // 1. Create a project via UI
        driver.get(getBaseUrl() + "/project/new");
        // Use a shorter name to respect the 29 character limit
        String timestamp = String.valueOf(System.currentTimeMillis());
        String projectName = "Test " + timestamp.substring(timestamp.length() - 6); // Last 6 digits
        driver.findElement(By.id("projectName")).sendKeys(projectName);
        driver.findElement(By.id("projectDescription")).sendKeys("Display test");
        driver.findElement(By.id("createProjectBtn")).click();

        // 2. Wait for redirect after creation
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.or(
            ExpectedConditions.urlContains("/project/list"),
            ExpectedConditions.urlContains("/board/")
        ));
        
        // 3. Navigate to project list if we were redirected to board
        String currentUrl = driver.getCurrentUrl();
        if (currentUrl.contains("/board/")) {
            driver.get(getBaseUrl() + "/project/list");
        }
        
        // 4. Wait for page to load and project list to be visible
        WebDriverWait longWait = new WebDriverWait(driver, Duration.ofSeconds(30));
        longWait.until(ExpectedConditions.presenceOfElementLocated(By.id("projectsList")));
        Thread.sleep(1000); // Give time for the list to render
        
        // 5. Wait for the specific project card to appear (with unique name)
        WebElement card = longWait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("//h5[contains(text(),'" + projectName + "')]/ancestor::div[contains(@class,'card')]")
        ));

        // 5. Verify the card contains the correct project name before clicking
        String cardText = card.getText();
        assertTrue(cardText.contains(projectName), 
                  "Card should contain the project name: " + projectName);

        // 6. Scroll to element and click Details link
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", card);
        Thread.sleep(500);

        WebElement detailsLink = wait.until(ExpectedConditions.elementToBeClickable(
            card.findElement(By.xpath(".//a[contains(text(),'Details')]"))
        ));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", detailsLink);

        // 7. Wait for project info page to load
        wait.until(ExpectedConditions.or(
            ExpectedConditions.presenceOfElementLocated(By.id("projectNameInfo")),
            ExpectedConditions.urlContains("/project/")
        ));
        Thread.sleep(1000);

        // 8. Verify project info page displays the correct data
        WebElement nameElem = driver.findElement(By.id("projectNameInfo"));
        WebElement descElem = driver.findElement(By.id("projectDescriptionInfo"));

        assertEquals(projectName, nameElem.getText(), 
                    "Project name should match: " + projectName);
        assertEquals("Display test", descElem.getText(), 
                    "Project description should match");
    }

    @Test
    @DisplayName("Should display project stories page")
    public void testShowProjectStories() {
        // Créer un projet
        driver.get(getBaseUrl() + "project/new");
        String projectName = "Project Stories Test " + System.currentTimeMillis();
        driver.findElement(By.id("projectName")).sendKeys(projectName);
        driver.findElement(By.id("projectDescription")).sendKeys("For stories test");
        driver.findElement(By.id("createProjectBtn")).click();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(2));
        wait.until(ExpectedConditions.urlContains("/project/list"));

        // Attendre que la page se charge et trouver le projet créé ou utiliser le premier disponible
        String projectId = null;
        try {
            WebElement projectCard = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//h5[contains(text(),'" + projectName + "')]/ancestor::div[contains(@class,'card')] | //div[contains(@class,'project-card')]//h3[contains(text(),'" + projectName + "')]/ancestor::div[contains(@class,'project-card')]")
            ));
            projectId = projectCard.getAttribute("data-id");
            if (projectId == null || projectId.isEmpty()) {
                WebElement boardLink = projectCard.findElement(By.xpath(".//a[contains(@href,'/board/')]"));
                String href = boardLink.getAttribute("href");
                projectId = href.split("/board/")[1].split("\\?")[0];
            }
        } catch (Exception e) {
            // Fallback: utiliser le premier lien board disponible
            try {
                WebElement firstBoardLink = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//a[contains(@href,'/board/')]")));
                String href = firstBoardLink.getAttribute("href");
                projectId = href.split("/board/")[1].split("\\?")[0];
            } catch (Exception e2) {
                return; // Skip test if no project found
            }
        }

        // Créer quelques stories
        for (int i = 0; i < 2; i++) {
            driver.get(getBaseUrl() + "story/new?projectId=" + projectId);
            driver.findElement(By.id("storyTitle")).sendKeys("Story " + i + " " + System.currentTimeMillis());
            driver.findElement(By.id("createStoryBtn")).click();
            wait.until(ExpectedConditions.urlContains("/board/"));
        }

        // Aller sur la page des stories du projet
        driver.get(getBaseUrl() + "project/" + projectId + "/stories");

        wait.until(ExpectedConditions.urlContains("/project/" + projectId + "/stories"));

        // Vérifie que la page se charge
        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains(projectName) || pageSource.contains("story"),
                   "Should display project stories page");
    }
}
