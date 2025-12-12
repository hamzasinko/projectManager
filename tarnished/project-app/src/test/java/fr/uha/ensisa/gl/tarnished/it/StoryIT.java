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
public class StoryIT {

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

    // ---------------------------------------------------
    // TESTS
    // ---------------------------------------------------

    @Test
    @DisplayName("Should display create story form with all required fields")
    public void testShowCreateForm() {
        driver.get(getBaseUrl() + "story/new");

        WebElement titleInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("storyTitle")));
        WebElement descriptionInput = driver.findElement(By.id("storyDescription"));
        WebElement projectSelect = driver.findElement(By.id("projectId"));
        WebElement createBtn = driver.findElement(By.id("createStoryBtn"));

        assertNotNull(titleInput);
        assertNotNull(descriptionInput);
        assertNotNull(projectSelect);
        assertNotNull(createBtn);

        assertEquals("text", titleInput.getAttribute("type"));
        assertTrue(titleInput.getAttribute("required") != null);
    }

    @Test
    @DisplayName("Should create a new story and redirect to list")
    public void testCreateStory() {
        driver.get(getBaseUrl() + "story/new");

        String title = "Test Story " + System.currentTimeMillis();
        driver.findElement(By.id("storyTitle")).sendKeys(title);
        driver.findElement(By.id("storyDescription")).sendKeys("Integration test description");
        driver.findElement(By.id("createStoryBtn")).click();

        assertTrue(driver.getCurrentUrl().contains("/story/list"));
    }

    @Test
    @DisplayName("Should display stories list page with new story button")
    public void testListStories() {
        driver.get(getBaseUrl() + "story/list");

        WebElement storiesList = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("storiesList")));
        WebElement newBtn = driver.findElement(By.id("newStoryBtn"));

        assertNotNull(storiesList);
        assertTrue(newBtn.isDisplayed());
        assertTrue(newBtn.isEnabled());
    }

    @Test
    @DisplayName("Should navigate between create form and list")
    public void testNavigation() {
        driver.get(getBaseUrl() + "story/list");
        wait.until(ExpectedConditions.elementToBeClickable(By.id("newStoryBtn"))).click();
        assertTrue(driver.getCurrentUrl().contains("/story/new"));

        wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Cancel"))).click();
        assertTrue(driver.getCurrentUrl().contains("/story/list"));
    }

    @Test
    @DisplayName("Should delete story from list")
    public void testDeleteStoryWorkflow() {
        driver.get(getBaseUrl() + "story/new");
        String title = "Story to Delete " + System.currentTimeMillis();
        driver.findElement(By.id("storyTitle")).sendKeys(title);
        driver.findElement(By.id("createStoryBtn")).click();

        driver.get(getBaseUrl() + "story/list");
        WebElement deleteBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(text(), 'Delete')]")
        ));
        deleteBtn.click();

        WebElement confirmBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(text(), 'Yes, delete')]")
        ));
        confirmBtn.click();

        assertTrue(driver.getCurrentUrl().contains("/story/list"));
        assertFalse(driver.getPageSource().contains(title));
    }
}
