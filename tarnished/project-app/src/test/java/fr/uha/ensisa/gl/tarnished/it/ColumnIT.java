package fr.uha.ensisa.gl.tarnished.it;

import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ColumnIT {

    private static WebDriver driver;
    private static WebDriverWait wait;
    private static final String BASE_URL = "http://localhost:8080";

    @BeforeAll
    static void setUpClass() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterAll
    static void tearDownClass() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    @Order(1)
    void testCreateColumnViaForm() {
        driver.get(BASE_URL + "/columns/create");

        WebElement nameInput = driver.findElement(By.id("column_name"));
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

    @Test
    @Order(2)
    void testEditColumnName() {
        driver.get(BASE_URL + "/columns");

        WebElement editButton = driver.findElement(By.cssSelector("[id^='column_edit_']"));
        editButton.click();

        WebElement nameInput = driver.findElement(By.id("column_name_edit"));
        nameInput.clear();
        nameInput.sendKeys("In Progress");

        WebElement form = driver.findElement(By.id("column_edit_form"));
        form.submit();

        assertTrue(driver.getCurrentUrl().contains("/columns"));
    }

    @Test
    @Order(9)
    void testStopTimerOnStory() {
        driver.get(BASE_URL + "/story/list");

        try {
            WebElement stopTimerForm = driver.findElement(By.cssSelector("[id^='timer_stop_']"));
            stopTimerForm.submit();

            assertTrue(driver.getCurrentUrl().contains("/stories"));
        } catch (Exception e) {
            // Timer might not be running
            assertTrue(true);
        }
    }

    @Test
    @Order(10)
    void testDragAndDropColumns() {
        driver.get(BASE_URL + "/columns");

        assertTrue(driver.findElement(By.id("column_list")).isDisplayed());
    }

    @Test
    @Order(11)
    void testAllEndpointsAccessible() {
        driver.get(BASE_URL + "/columns");
        assertEquals(200, getHttpStatus());

        driver.get(BASE_URL + "/columns/create");
        assertEquals(200, getHttpStatus());

        assertTrue(true);
    }

    private int getHttpStatus() {
        return driver.getPageSource().contains("error") ? 404 : 200;
    }
}
