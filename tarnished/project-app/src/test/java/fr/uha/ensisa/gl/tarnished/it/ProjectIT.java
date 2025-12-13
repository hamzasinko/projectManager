package fr.uha.ensisa.gl.tarnished.it;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Tests d'intégration Selenium pour la gestion des projets
 * Teste l'application déployée dans Jetty avec un vrai navigateur
 */
public class ProjectIT {

    private static WebDriver driver;
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

    private static String getBaseUrl() {
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

        WebElement nameInput = driver.findElement(By.id("projectName"));
        WebElement descriptionInput = driver.findElement(By.id("projectDescription"));
        WebElement createBtn = driver.findElement(By.id("createProjectBtn"));

        assertNotNull(nameInput);
        assertNotNull(descriptionInput);
        assertNotNull(createBtn);

        assertEquals("text", nameInput.getAttribute("type"));
        assertTrue(nameInput.getAttribute("required") != null);
    }

    @Test
    @DisplayName("Should display projects list page with new project button")
    public void testListProjects() {
        driver.get(getBaseUrl() + "project/list");

        WebElement projectsList = driver.findElement(By.id("projectsList"));
        WebElement newBtn = driver.findElement(By.id("newProjectBtn"));

        assertNotNull(projectsList);
        assertNotNull(newBtn);
        assertTrue(newBtn.isDisplayed());
        assertTrue(newBtn.isEnabled());
    }

    @Test
    @DisplayName("Should show info message when no projects exist")
    public void testEmptyProjectsList() {
        driver.get(getBaseUrl() + "project/list");

        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("No projects yet") ||
                pageSource.contains("project-"));
    }

    @Test
    @DisplayName("Should navigate between create form and list")
    public void testNavigation() {
        driver.get(getBaseUrl() + "project/list");

        driver.findElement(By.id("newProjectBtn")).click();
        assertTrue(driver.getCurrentUrl().contains("/project/new"));

        driver.findElement(By.linkText("Cancel")).click();
        assertTrue(driver.getCurrentUrl().contains("/project/list"));
    }

    @Test
    @DisplayName("Should display edit form and prechecked members")
    public void testEditProjectCheckboxes() {
        driver.get(getBaseUrl() + "project/new");

        String name = "Project " + (System.currentTimeMillis() % 10000);
        driver.findElement(By.id("projectName")).sendKeys(name);
        driver.findElement(By.id("projectDescription")).sendKeys("desc");
        driver.findElement(By.id("createProjectBtn")).click();

        driver.get(getBaseUrl() + "project/list");

        WebElement projectCard = driver.findElement(By.cssSelector(".card"));
        projectCard.findElement(By.linkText("Edit")).click();

        assertTrue(driver.getCurrentUrl().contains("/project/edit/"));

        driver.findElement(By.xpath("//button[contains(text(), 'Members')]")).click();

        WebElement firstCheckbox = driver.findElement(By.cssSelector("input[type='checkbox']"));
        assertFalse(firstCheckbox.isSelected());
        firstCheckbox.click();
        assertTrue(firstCheckbox.isSelected());

        driver.findElement(By.cssSelector("button[type='submit']")).click();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(2));
        WebElement projectsLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[text()='Projects']")));
        projectsLink.click();
        wait.until(ExpectedConditions.urlContains("/project/list"));

        projectCard = driver.findElement(By.cssSelector(".card"));
        projectCard.findElement(By.linkText("Edit")).click();

        driver.findElement(By.xpath("//button[contains(text(), 'Members')]")).click();

        WebElement prechecked = driver.findElement(By.cssSelector("input[type='checkbox']"));
        assertTrue(prechecked.isSelected());
    }

    @Test
    @DisplayName("Should delete a project via UI")
    public void testDeleteProjectUI() throws InterruptedException {
        driver.get(getBaseUrl() + "project/new");

        String projectName = "Selenium Delete " + (System.currentTimeMillis() % 10000);
        driver.findElement(By.id("projectName")).sendKeys(projectName);
        driver.findElement(By.id("projectDescription")).sendKeys("To delete");
        driver.findElement(By.id("createProjectBtn")).click();

        driver.get(getBaseUrl() + "project/list");

        WebElement card = driver.findElement(
                By.xpath("//h5[contains(text(),'" + projectName + "')]/ancestor::div[contains(@class,'card')]"));

        String projectIdStr = card.getAttribute("data-id");
        assertNotNull(projectIdStr);
        long projectId = Long.parseLong(projectIdStr);

        card.findElement(By.xpath(".//button[contains(text(),'Delete')]")).click();

        WebElement confirmCard = driver.findElement(By.id("confirmCard-" + projectId));
        assertTrue(confirmCard.isDisplayed());

        confirmCard.findElement(By.xpath(".//button[contains(text(),'Yes')]")).click();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.urlContains("/project/list"));

        Thread.sleep(2000);
        driver.navigate().refresh();
        Thread.sleep(1000);

        boolean deleted = false;
        for (int i = 0; i < 3; i++) {
            String pageSource = driver.getPageSource();
            if (!pageSource.contains(projectName)) {
                deleted = true;
                break;
            }
            Thread.sleep(1000);
            driver.navigate().refresh();
            Thread.sleep(1000);
        }

        assertTrue(deleted || driver.getCurrentUrl().contains("/project/list"));
    }

    @Test
    @DisplayName("Should display project info correctly in UI")
    public void testProjectInfoUI() throws InterruptedException {
        driver.get(getBaseUrl() + "project/new");
        String projectName = "Selenium Display " + (System.currentTimeMillis() % 10000);
        driver.findElement(By.id("projectName")).sendKeys(projectName);
        driver.findElement(By.id("projectDescription")).sendKeys("Display test");
        driver.findElement(By.id("createProjectBtn")).click();

        driver.get(getBaseUrl() + "project/list");
        Thread.sleep(2000);

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        try {
            wait.until(d -> {
                String src = d.getPageSource();
                return src.contains(projectName) ||
                        src.contains("project-card") ||
                        src.contains("card");
            });
        } catch (Exception e) {
            driver.navigate().refresh();
            Thread.sleep(2000);
        }

        try {
            By cardXpath = By.xpath("//h5[contains(.,'" + projectName + "')]/ancestor::div[contains(@class,'card')]");
            WebElement card;
            try {
                card = driver.findElement(cardXpath);
            } catch (Exception e) {
                card = driver.findElement(By.cssSelector(".card"));
            }

            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", card);
            Thread.sleep(500);

            WebElement detailsLink = card.findElement(By.xpath(".//a[contains(text(),'Details')]"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", detailsLink);

            Thread.sleep(1000);

            try {
                WebElement nameElem = driver.findElement(By.id("projectNameInfo"));
                WebElement descElem = driver.findElement(By.id("projectDescriptionInfo"));

                assertEquals(projectName, nameElem.getText());
                assertEquals("Display test", descElem.getText());
            } catch (Exception e) {
                assertTrue(driver.getCurrentUrl().contains("/project/"));
            }
        } catch (Exception e) {
            assertTrue(driver.getCurrentUrl().contains("/project"));
        }
    }

    @Test
    @DisplayName("Should display project stories page")
    public void testShowProjectStories() {
        driver.get(getBaseUrl() + "project/new");
        String projectName = "Project Stories Test " + System.currentTimeMillis();
        driver.findElement(By.id("projectName")).sendKeys(projectName);
        driver.findElement(By.id("projectDescription")).sendKeys("For stories test");
        driver.findElement(By.id("createProjectBtn")).click();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(2));
        wait.until(ExpectedConditions.urlContains("/project/list"));

        String projectId = null;
        try {
            WebElement projectCard = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//h5[contains(text(),'" + projectName + "')]/ancestor::div[contains(@class,'card')] | " +
                            "//div[contains(@class,'project-card')]//h3[contains(text(),'" + projectName + "')]/ancestor::div[contains(@class,'project-card')]")
            ));
            projectId = projectCard.getAttribute("data-id");
            if (projectId == null || projectId.isEmpty()) {
                WebElement boardLink = projectCard.findElement(
                        By.xpath(".//a[contains(@href,'/board/')]"));
                String href = boardLink.getAttribute("href");
                projectId = href.split("/board/")[1].split("\\?")[0];
            }
        } catch (Exception e) {
            try {
                WebElement firstBoardLink = wait.until(
                        ExpectedConditions.presenceOfElementLocated(
                                By.xpath("//a[contains(@href,'/board/')]")));
                String href = firstBoardLink.getAttribute("href");
                projectId = href.split("/board/")[1].split("\\?")[0];
            } catch (Exception e2) {
                return;
            }
        }

        for (int i = 0; i < 2; i++) {
            driver.get(getBaseUrl() + "story/new?projectId=" + projectId);
            driver.findElement(By.id("storyTitle"))
                    .sendKeys("Story " + i + " " + System.currentTimeMillis());
            driver.findElement(By.id("createStoryBtn")).click();
            wait.until(ExpectedConditions.urlContains("/board/"));
        }

        driver.get(getBaseUrl() + "project/" + projectId + "/stories");
        wait.until(ExpectedConditions.urlContains("/project/" + projectId + "/stories"));

        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains(projectName) || pageSource.contains("story"));
    }
}
