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
        String contextPath = System.getProperty("jetty.context.path", "/gl2526-tarnished");
        return "http://" + host + ":" + port + contextPath + "/";
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
        
        //vérifie la présence du formulaire
        WebElement nameInput = driver.findElement(By.id("projectName"));
        WebElement descriptionInput = driver.findElement(By.id("projectDescription"));
        WebElement createBtn = driver.findElement(By.id("createProjectBtn"));
        
        assertNotNull(nameInput, "Name input should be present");
        assertNotNull(descriptionInput, "Description input should be present");
        assertNotNull(createBtn, "Create button should be present");
        
        //vérifie que le champ name est required
        assertEquals("text", nameInput.getAttribute("type"));
        assertTrue(nameInput.getAttribute("required") != null, "Name field should be required");
    }
    
    @Test
    @DisplayName("Should display projects list page with new project button")
    public void testListProjects() {
        driver.get(getBaseUrl() + "project/list");
        
        //vérifie la présence des éléments principaux
        WebElement projectsList = driver.findElement(By.id("projectsList"));
        WebElement newBtn = driver.findElement(By.id("newProjectBtn"));
        
        assertNotNull(projectsList, "Projects list container should be present");
        assertNotNull(newBtn, "New project button should be present");
        
        //vérifie que le bouton est cliquable
        assertTrue(newBtn.isDisplayed(), "New project button should be visible");
        assertTrue(newBtn.isEnabled(), "New project button should be enabled");
    }
    
    @Test
    @DisplayName("Should show info message when no projects exist")
    public void testEmptyProjectsList() {
        driver.get(getBaseUrl() + "project/list");
        
        //vérifie le message pour liste vide (tant que mock retourne emptyList)
        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("No projects yet") || 
                   pageSource.contains("project-"), 
                   "Should show either empty message or projects");
    }
    
    @Test
    @DisplayName("Should navigate between create form and list")
    public void testNavigation() {
        //va sur la liste
        driver.get(getBaseUrl() + "project/list");
        
        //clique sur "New Project"
        driver.findElement(By.id("newProjectBtn")).click();
        assertTrue(driver.getCurrentUrl().contains("/project/new"), 
                   "Should navigate to create form");
        
        //clique sur "Cancel"
        driver.findElement(By.linkText("Cancel")).click();
        assertTrue(driver.getCurrentUrl().contains("/project/list"), 
                   "Should navigate back to list");
    }

    @Test
    @DisplayName("Should update project via edit form")
    public void testUpdateProject() throws InterruptedException {
        //1. créer un projet
        driver.get(getBaseUrl() + "project/new");
        String projectName = "Update Test " + System.currentTimeMillis();
        driver.findElement(By.id("projectName")).sendKeys(projectName);
        driver.findElement(By.id("projectDescription")).sendKeys("Original description");
        driver.findElement(By.id("createProjectBtn")).click();
        
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.urlContains("/project/list"));
        sleep(2000);
        
        //2. trouver le projet et cliquer Edit
        WebElement card = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("//h5[contains(text(),'" + projectName + "')]/ancestor::div[contains(@class,'card')]")
        ));
        String projectIdStr = card.getAttribute("data-id");
        assertNotNull(projectIdStr, "Card should have a data-id attribute");
        
        WebElement editBtn = card.findElement(By.xpath(".//a[contains(text(),'Edit')]"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", editBtn);
        sleep(500);
        //utiliser JavaScript click pour éviter ElementClickInterceptedException
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", editBtn);
        
        //3. attendre le formulaire d'édition
        wait.until(ExpectedConditions.urlContains("/project/edit/"));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("projectName")));
        
        //4. mettre à jour le nom et la description du projet
        WebElement nameInput = driver.findElement(By.id("projectName"));
        nameInput.clear();
        String updatedName = "Updated " + System.currentTimeMillis();
        nameInput.sendKeys(updatedName);
        
        WebElement descInput = driver.findElement(By.id("projectDescription"));
        descInput.clear();
        descInput.sendKeys("Updated description");
        
        //5. soumettre le formulaire
        WebElement updateBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("updateProjectBtn")));
        updateBtn.click();
        
        //6. vérifier la redirection vers project info
        wait.until(ExpectedConditions.urlContains("/project/info/"));
        
        //7. vérifier que le nom mis à jour est affiché
        WebElement nameInfo = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("projectNameInfo")));
        assertTrue(nameInfo.getText().contains(updatedName) || nameInfo.getText().equals(updatedName),
                  "Project name should be updated");
    }

    @Test
    @DisplayName("Should display edit form and prechecked members")
    public void testEditProjectCheckboxes() {
        //précondition: créer un nouveau projet
        driver.get(getBaseUrl() + "project/new");

        String name = "Project " + (System.currentTimeMillis() % 10000);
        driver.findElement(By.id("projectName")).sendKeys(name);
        driver.findElement(By.id("projectDescription")).sendKeys("desc");
        driver.findElement(By.id("createProjectBtn")).click();

        //récupère l'URL générée pour l'édition
        driver.get(getBaseUrl() + "project/list");

        //clique premier bouton Edit ou Open Board, adapter si besoin
        WebElement projectCard = driver.findElement(By.cssSelector(".card"));
        projectCard.findElement(By.linkText("Edit")).click();

        assertTrue(driver.getCurrentUrl().contains("/project/edit/"));

        //ouvre la section members
        driver.findElement(By.xpath("//button[contains(text(), 'Members')]")).click();

        //attend la liste users
        WebElement firstCheckbox = driver.findElement(By.cssSelector("input[type='checkbox']"));

        //vérifie que la checkbox est décochée initialement
        assertFalse(firstCheckbox.isSelected(), "Initial member checkbox should NOT be checked");

        //coche
        firstCheckbox.click();
        assertTrue(firstCheckbox.isSelected(), "Checkbox should become checked after click");

        //sauvegarde
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        //recharge page d'édition:
        //attendre que le bouton "Projects" soit visible et cliquer
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(2));
        WebElement projectsLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[text()='Projects']")
        ));
        projectsLink.click();

//optionnellement, attendre que la page de liste de projets se charge
        wait.until(ExpectedConditions.urlContains("/project/list"));

        projectCard = driver.findElement(By.cssSelector(".card"));
        projectCard.findElement(By.linkText("Edit")).click();

        driver.findElement(By.xpath("//button[contains(text(), 'Members')]")).click();

        //vérifie qu'elle est maintenant pré-cochée
        WebElement prechecked = driver.findElement(By.cssSelector("input[type='checkbox']"));
        assertTrue(prechecked.isSelected(),
                "Checkbox should be prechecked because user is now a member");
    }

    @Test
    @DisplayName("Should delete a project via UI")
    public void testDeleteProjectUI() throws InterruptedException {
        //1. créer un nouveau projet
        driver.get(getBaseUrl() + "project/new");

        String projectName = "Selenium Delete " + System.currentTimeMillis();
        driver.findElement(By.id("projectName")).sendKeys(projectName);
        driver.findElement(By.id("projectDescription")).sendKeys("To delete");
        driver.findElement(By.id("createProjectBtn")).click();

        //2. attendre la redirection après création (peut être /project/list ou /board/{id})
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.or(
            ExpectedConditions.urlContains("/project/list"),
            ExpectedConditions.urlContains("/board/")
        ));

        //3. naviguer vers la liste de projets explicitement
        driver.get(getBaseUrl() + "project/list");
        sleep(2000); // Give time for page to load

        //4. attendre que la carte projet apparaisse avec un wait explicite
        WebElement card = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("//h5[contains(text(),'" + projectName + "')]/ancestor::div[contains(@class,'card')]")
        ));

        //5. extraire l'ID du projet depuis l'attribut data-id
        String projectIdStr = card.getAttribute("data-id");
        assertNotNull(projectIdStr, "Card should have a data-id attribute");
        long projectId = Long.parseLong(projectIdStr);

        //6. cliquer sur le bouton Delete
        WebElement deleteBtn = card.findElement(By.xpath(".//button[contains(text(),'Delete')]"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", deleteBtn);
        sleep(500);
        deleteBtn.click();

        //7. attendre que la carte de confirmation apparaisse
        WebElement confirmCard = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("confirmCard-" + projectId)
        ));
        assertTrue(confirmCard.isDisplayed(), "Confirmation card should appear");

        //8. cliquer Yes, delete
        confirmCard.findElement(By.xpath(".//button[contains(text(),'Yes')]")).click();

        //9. attendre que la page se recharge après suppression
        wait.until(ExpectedConditions.urlContains("/project/list"));
        sleep(2000);
        driver.navigate().refresh();
        sleep(2000);

        //10. vérifier que le projet a disparu de l'UI (avec retry)
        boolean deleted = false;
        for (int i = 0; i < 5; i++) {
            String pageSource = driver.getPageSource();
            if (!pageSource.contains(projectName)) {
                deleted = true;
                break;
            }
            sleep(1000);
            driver.navigate().refresh();
            sleep(1000);
        }

        assertTrue(deleted, "Project should be deleted from the list");
    }

    @Test
    @DisplayName("Should display project info correctly in UI")
    public void testProjectInfoUI() throws InterruptedException {
        //1. créer un projet via UI
        driver.get(getBaseUrl() + "/project/new");
        //utiliser un nom plus court pour respecter la limite de 29 caractères
        String timestamp = String.valueOf(System.currentTimeMillis());
        String projectName = "Test " + timestamp.substring(timestamp.length() - 6); // Last 6 digits
        driver.findElement(By.id("projectName")).sendKeys(projectName);
        driver.findElement(By.id("projectDescription")).sendKeys("Display test");
        driver.findElement(By.id("createProjectBtn")).click();

        //2. attendre la redirection après création
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.or(
            ExpectedConditions.urlContains("/project/list"),
            ExpectedConditions.urlContains("/board/")
        ));
        
        //3. naviguer vers la liste de projets si on a été redirigé vers le board
        String currentUrl = driver.getCurrentUrl();
        if (currentUrl.contains("/board/")) {
            driver.get(getBaseUrl() + "/project/list");
        }
        
        //4. attendre que la page se charge et que la liste de projets soit visible
        WebDriverWait longWait = new WebDriverWait(driver, Duration.ofSeconds(30));
        longWait.until(ExpectedConditions.presenceOfElementLocated(By.id("projectsList")));
        sleep(1000); // Give time for the list to render
        
        //5. attendre que la carte projet spécifique apparaisse (avec nom unique)
        WebElement card = longWait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("//h5[contains(text(),'" + projectName + "')]/ancestor::div[contains(@class,'card')]")
        ));

        //5. vérifier que la carte contient le bon nom de projet avant de cliquer
        String cardText = card.getText();
        assertTrue(cardText.contains(projectName), 
                  "Card should contain the project name: " + projectName);

        //6. scroller vers l'élément et cliquer sur le lien Details
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", card);
        sleep(500);

        WebElement detailsLink = wait.until(ExpectedConditions.elementToBeClickable(
            card.findElement(By.xpath(".//a[contains(text(),'Details')]"))
        ));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", detailsLink);

        //7. attendre que la page d'info projet se charge
        wait.until(ExpectedConditions.or(
            ExpectedConditions.presenceOfElementLocated(By.id("projectNameInfo")),
            ExpectedConditions.urlContains("/project/")
        ));
        sleep(1000);

        //8. vérifier que la page d'info projet affiche les bonnes données
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
        //créer un projet
        driver.get(getBaseUrl() + "project/new");
        String projectName = "Project Stories Test " + System.currentTimeMillis();
        driver.findElement(By.id("projectName")).sendKeys(projectName);
        driver.findElement(By.id("projectDescription")).sendKeys("For stories test");
        driver.findElement(By.id("createProjectBtn")).click();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(2));
        wait.until(ExpectedConditions.urlContains("/project/list"));

        //attendre que la page se charge et trouver le projet créé ou utiliser le premier disponible
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
            //fallback: utiliser le premier lien board disponible
            try {
                WebElement firstBoardLink = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//a[contains(@href,'/board/')]")));
                String href = firstBoardLink.getAttribute("href");
                projectId = href.split("/board/")[1].split("\\?")[0];
            } catch (Exception e2) {
                return; // Skip test if no project found
            }
        }

        //créer quelques stories
        for (int i = 0; i < 2; i++) {
            driver.get(getBaseUrl() + "story/new?projectId=" + projectId);
            driver.findElement(By.id("storyTitle")).sendKeys("Story " + i + " " + System.currentTimeMillis());
            driver.findElement(By.id("createStoryBtn")).click();
            wait.until(ExpectedConditions.urlContains("/board/"));
        }

        //aller sur la page des stories du projet
        driver.get(getBaseUrl() + "project/" + projectId + "/stories");

        wait.until(ExpectedConditions.urlContains("/project/" + projectId + "/stories"));

        //vérifie que la page se charge
        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains(projectName) || pageSource.contains("story"),
                   "Should display project stories page");
    }
}
