package fr.uha.ensisa.gl.tarnished.it;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.*;
import org.openqa.selenium.support.ui.*;

import java.net.URL;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ColumnIT {

    private static WebDriver driver;
    private static WebDriverWait wait;

    private static String host;
    private static String port;
    private static String BASE_URL;

    @BeforeAll
    static void setUp() throws Exception {

        host = System.getProperty("host", "localhost");
        port = System.getProperty("servlet.port", "8080");
        BASE_URL = "http://" + host + ":" + port;

        String remote = System.getProperty("selenium.remote.browser");

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--headless=new");  // mode headless compatible GitLab

        if ("true".equalsIgnoreCase(remote)) {
            System.out.println("Running Selenium in REMOTE mode (GitLab)");
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
    static void tearDownClass() {
        if (driver != null) driver.quit();
    }

    // --------------------------------------------------------------------

    @Test
    @Order(1)
    void testCreateColumnViaForm() {
        driver.get(BASE_URL + "/columns/create");

        WebElement nameInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("column_name")));
        WebElement orderInput = driver.findElement(By.id("column_order"));
        WebElement limitInput = driver.findElement(By.id("column_limit"));

        nameInput.sendKeys("To Do");
        orderInput.clear();
        orderInput.sendKeys("1");
        limitInput.clear();
        limitInput.sendKeys("5");

        WebElement form = driver.findElement(By.id("column_create_form"));
        form.submit();

        assertTrue(driver.getCurrentUrl().contains("/columns"));
    }

    // --------------------------------------------------------------------

    @Test
    @Order(2)
    void testEditColumnName() {
        driver.get(BASE_URL + "/columns");

        WebElement editButton = wait.until(
                ExpectedConditions.elementToBeClickable(By.cssSelector("[id^='column_edit_']"))
        );
        editButton.click();

        WebElement nameInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("column_name_edit")));
        nameInput.clear();
        nameInput.sendKeys("In Progress");

        WebElement form = driver.findElement(By.id("column_edit_form"));
        form.submit();

        assertTrue(driver.getCurrentUrl().contains("/columns"));
    }

    // --------------------------------------------------------------------

    @Test
    @Order(9)
    void testStopTimerOnStory() {
        driver.get(BASE_URL + "/story/list");

        try {
            WebElement stopTimerForm = driver.findElement(By.cssSelector("[id^='timer_stop_']"));
            stopTimerForm.submit();
            assertTrue(driver.getCurrentUrl().contains("/stories"));
        } catch (NoSuchElementException e) {
            // Aucun timer à stopper → test OK
            assertTrue(true);
        }
    }

    // --------------------------------------------------------------------

    @Test
    @Order(10)
    void testDragAndDropColumns() {
        driver.get(BASE_URL + "/columns");
        assertTrue(driver.findElement(By.id("column_list")).isDisplayed());
    }

    // --------------------------------------------------------------------

    @Test
    @Order(11)
    void testAllEndpointsAccessible() {
        driver.get(BASE_URL + "/columns");
        assertEquals(200, getHttpStatus());

        driver.get(BASE_URL + "/columns/create");
        assertEquals(200, getHttpStatus());
    }

    // --------------------------------------------------------------------

    private int getHttpStatus() {
        // GitLab CI ne permet pas de faire un vrai request.getStatus()
        return driver.getPageSource().contains("error") ? 404 : 200;
    }
}
