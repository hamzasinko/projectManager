package fr.uha.ensisa.gl.tarnished.it;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.URL;
import java.time.Duration;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProjectIT {

    private static WebDriver driver;
    private static WebDriverWait wait;

    private static String host, port;
    private static String BASE_URL;

    @BeforeAll
    public static void setupWebDriver() throws Exception {

        host = System.getProperty("host", "localhost");
        port = System.getProperty("servlet.port", "8080");
        BASE_URL = "http://" + host + ":" + port + "/";

        String remote = System.getProperty("selenium.remote.browser");

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--headless=new");

        if ("true".equalsIgnoreCase(remote)) {
            System.out.println("Running Selenium in REMOTE mode (GitLab CI)");
            driver = new org.openqa.selenium.remote.RemoteWebDriver(
                    new URL("http://selenium:4444"),
                    options
            );
        } else {
            System.out.println("Running Selenium in LOCAL mode");
            WebDriverManager.chromedriver().setup();
            driver = new ChromeDriver(options);
        }

        wait = new WebDriverWait(driver, Duration.ofSeconds(8));
    }

    @AfterAll
    public static void shutdownWebDriver() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }

    private static String getBaseUrl() {
        return BASE_URL;
    }

    // ---------------------------------------------------------------
    // TESTS
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Should display create project form with all required fields")
    public void testShowCreateForm() {
        driver.get(getBaseUrl() + "project/new");

        WebElement nameInput = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.id("projectName"))
        );
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

        WebElement projectsList = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.id("projectsList"))
        );
        WebElement newBtn = driver.findElement(By.id("newProjectBtn"));

        assertNotNull(projectsList);
        assertTrue(newBtn.isDisplayed());
    }

    @Test
    @DisplayName("Should show info message when no projects exist")
    public void testEmptyProjectsList() {
        driver.get(getBaseUrl() + "project/list");
        String pageSource = driver.getPageSource();

        assertTrue(
                pageSource.contains("No projects yet")
                        || pageSource.contains("project-")
        );
    }

    @Test
    @DisplayName("Should navigate between create form and list")
    public void testNavigation() {
        driver.get(getBaseUrl() + "project/list");

        wait.until(ExpectedConditions.elementToBeClickable(By.id("newProjectBtn"))).click();
        assertTrue(driver.getCurrentUrl().contains("/project/new"));

        wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Cancel"))).click();
        assertTrue(driver.getCurrentUrl().contains("/project/list"));
    }

    @Test
    @DisplayName("Should display edit form and prechecked members")
    public void testEditProjectCheckboxes() {

        // --- Create project ---
        driver.get(getBaseUrl() + "project/new");
        String name = "Project " + System.currentTimeMillis();

        driver.findElement(By.id("projectName")).sendKeys(name);
        driver.findElement(By.id("projectDescription")).sendKeys("desc");
        driver.findElement(By.id("createProjectBtn")).click();

        // --- Go to list ---
        driver.get(getBaseUrl() + "project/list");

        WebElement card = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector(".card")
        ));
        card.findElement(By.linkText("Edit")).click();

        assertTrue(driver.getCurrentUrl().contains("/project/edit/"));

        // Open Members section
        wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(text(), 'Members')]")
        )).click();

        WebElement checkbox = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[type='checkbox']"))
        );

        assertFalse(checkbox.isSelected());
        checkbox.click();
        assertTrue(checkbox.isSelected());

        driver.findElement(By.cssSelector("button[type='submit']")).click();

        // Re-open project edit page
        WebDriverWait waitShort = new WebDriverWait(driver, Duration.ofSeconds(3));
        waitShort.until(ExpectedConditions.elementToBeClickable(By.xpath("//a[text()='Projects']"))).click();

        driver.get(getBaseUrl() + "project/list");
        card = driver.findElement(By.cssSelector(".card"));
        card.findElement(By.linkText("Edit")).click();

        driver.findElement(By.xpath("//button[contains(text(), 'Members')]")).click();

        WebElement prechecked = driver.findElement(By.cssSelector("input[type='checkbox']"));
        assertTrue(prechecked.isSelected());
    }

    @Test
    @DisplayName("Should delete a project via UI")
    public void testDeleteProjectUI() {

        // Create project
        driver.get(getBaseUrl() + "project/new");
        String projectName = "Selenium Delete " + System.currentTimeMillis();
        driver.findElement(By.id("projectName")).sendKeys(projectName);
        driver.findElement(By.id("projectDescription")).sendKeys("To delete");
        driver.findElement(By.id("createProjectBtn")).click();

        // Go to list
        driver.get(getBaseUrl() + "project/list");

        WebElement card = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//h5[contains(text(),'" + projectName + "')]/ancestor::div[contains(@class,'card')]")
        ));

        String id = card.getAttribute("data-id");
        assertNotNull(id);

        // delete
        card.findElement(By.xpath(".//button[contains(text(),'Delete')]")).click();

        WebElement confirmCard = driver.findElement(By.id("confirmCard-" + id));
        assertTrue(confirmCard.isDisplayed());

        confirmCard.findElement(By.xpath(".//button[contains(text(),'Yes')]")).click();

        assertFalse(driver.getPageSource().contains(projectName));
    }

    @Test
    @DisplayName("Should display project info correctly in UI")
    public void testProjectInfoUI() {
        driver.get(getBaseUrl() + "project/new");

        String projectName = "Selenium Display " + System.currentTimeMillis();
        driver.findElement(By.id("projectName")).sendKeys(projectName);
        driver.findElement(By.id("projectDescription")).sendKeys("Display test");
        driver.findElement(By.id("createProjectBtn")).click();

        driver.get(getBaseUrl() + "project/list");

        WebElement card = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//h5[contains(text(),'" + projectName + "')]/ancestor::div[contains(@class,'card')]")
        ));

        card.findElement(By.xpath(".//a[contains(text(),'Details')]")).click();

        WebElement nameElem = driver.findElement(By.id("projectNameInfo"));
        WebElement descElem = driver.findElement(By.id("projectDescriptionInfo"));

        assertEquals(projectName, nameElem.getText());
        assertEquals("Display test", descElem.getText());
    }
}
