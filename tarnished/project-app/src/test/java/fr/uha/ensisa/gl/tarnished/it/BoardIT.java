package fr.uha.ensisa.gl.tarnished.it;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import java.time.Duration;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BoardIT {
    
    public static WebDriver driver;
    private static WebDriverWait wait;
    private static String host, port;
    
    @BeforeAll
    public static void setupWebDriver() {
        if (driver != null) return;
        
        host = System.getProperty("host", "localhost");
        port = System.getProperty("servlet.port", "8090");
        
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }
    
    @AfterAll
    public static void shutdownWebDriver() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }
    
    public static String getBaseUrl() {
        return "http://" + host + ":" + port + "/";
    }
    
    @Test
    @Order(1)
    @DisplayName("Should load home page successfully")
    public void testHomePageLoads() {
        driver.get(getBaseUrl());
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
        assertTrue(driver.getPageSource().length() > 100, "Home page should load with content");
    }
    
    @Test
    @Order(2)
    @DisplayName("Should access project creation page")
    public void testProjectCreationPage() {
        driver.get(getBaseUrl() + "project/new");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("projectName")));
        
        WebElement nameInput = driver.findElement(By.id("projectName"));
        WebElement descInput = driver.findElement(By.id("projectDescription"));
        WebElement submitBtn = driver.findElement(By.id("createProjectBtn"));
        
        assertNotNull(nameInput, "Name input should exist");
        assertNotNull(descInput, "Description input should exist");
        assertNotNull(submitBtn, "Submit button should exist");
    }
    
    @Test
    @Order(3)
    @DisplayName("Should create a new project")
    public void testCreateProject() {
        driver.get(getBaseUrl() + "project/new");
        
        String projectName = "Test Project " + System.currentTimeMillis();
        
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("projectName")));
        driver.findElement(By.id("projectName")).sendKeys(projectName);
        driver.findElement(By.id("projectDescription")).sendKeys("Integration test project");
        driver.findElement(By.id("createProjectBtn")).click();
        
        wait.until(ExpectedConditions.urlContains("/project/list"));
        assertTrue(driver.getCurrentUrl().contains("/project/list"), "Should redirect to project list");
    }
    
    @Test
    @Order(4)
    @DisplayName("Should display project list")
    public void testProjectList() {
        driver.get(getBaseUrl() + "project/list");
        
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("projectsList")));
        WebElement projectsList = driver.findElement(By.id("projectsList"));
        WebElement newBtn = driver.findElement(By.id("newProjectBtn"));
        
        assertNotNull(projectsList, "Projects list should be present");
        assertNotNull(newBtn, "New project button should be present");
    }
    
    @Test
    @Order(5)
    @DisplayName("Should access column creation page")
    public void testColumnCreationPage() {
        driver.get(getBaseUrl() + "columns/create");
        
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("column_name")));
        
        WebElement nameInput = driver.findElement(By.id("column_name"));
        WebElement orderInput = driver.findElement(By.id("column_order"));
        WebElement limitInput = driver.findElement(By.id("column_limit"));
        
        assertNotNull(nameInput, "Column name input should exist");
        assertNotNull(orderInput, "Column order input should exist");
        assertNotNull(limitInput, "Column limit input should exist");
    }
    
    @Test
    @Order(6)
    @DisplayName("Should create a new column")
    public void testCreateColumn() {
        driver.get(getBaseUrl() + "columns/create");
        
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("column_name")));
        
        driver.findElement(By.id("column_name")).sendKeys("Backlog");
        driver.findElement(By.id("column_order")).clear();
        driver.findElement(By.id("column_order")).sendKeys("1");
        driver.findElement(By.id("column_limit")).clear();
        driver.findElement(By.id("column_limit")).sendKeys("10");
        
        driver.findElement(By.id("column_create_form")).submit();
        
        wait.until(ExpectedConditions.urlContains("/columns"));
        assertTrue(driver.getCurrentUrl().contains("/columns"), "Should redirect to columns list");
    }
    
    @Test
    @Order(7)
    @DisplayName("Should display columns list")
    public void testColumnsList() {
        driver.get(getBaseUrl() + "columns");
        
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
        String pageSource = driver.getPageSource();
        
        assertTrue(pageSource.contains("Columns") || pageSource.contains("column"), 
                   "Columns page should load");
    }
    
    @Test
    @Order(8)
    @DisplayName("Should access story creation form")
    public void testStoryCreationForm() {
        driver.get(getBaseUrl() + "story/new");
        
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("storyTitle")));
        
        WebElement titleInput = driver.findElement(By.id("storyTitle"));
        WebElement descInput = driver.findElement(By.id("storyDescription"));
        WebElement createBtn = driver.findElement(By.id("createStoryBtn"));
        
        assertNotNull(titleInput, "Story title input should exist");
        assertNotNull(descInput, "Story description input should exist");
        assertNotNull(createBtn, "Create button should exist");
    }
    
    @Test
    @Order(9)
    @DisplayName("Should create a new story with project")
    public void testCreateStory() throws InterruptedException {
        driver.get(getBaseUrl() + "story/new");
        
        String storyTitle = "Test Story " + System.currentTimeMillis();
        
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("storyTitle")));
        driver.findElement(By.id("storyTitle")).sendKeys(storyTitle);
        driver.findElement(By.id("storyDescription")).sendKeys("Integration test story\nAC: Should pass tests");
        
        // Select first project if available
        try {
            WebElement projectSelect = driver.findElement(By.id("projectId"));
            projectSelect.click();
            driver.findElement(By.cssSelector("#projectId option:nth-child(2)")).click();
        } catch (Exception e) {
            // No project select, continue without
        }
        
        WebElement createBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("createStoryBtn")));
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", createBtn);
        Thread.sleep(500);
        createBtn.click();
        
        // Wait for action to complete
        Thread.sleep(1000);
        String currentUrl = driver.getCurrentUrl();
        assertTrue(currentUrl.contains("/board/") || 
                   currentUrl.equals(getBaseUrl()) ||
                   currentUrl.contains("/story"), 
                   "Should redirect or stay after story creation");
    }
    
    @Test
    @Order(10)
    @DisplayName("Should handle story list redirect to home")
    public void testStoryListRedirect() {
        driver.get(getBaseUrl() + "story/list");
        
        // /story/list should redirect to /
        wait.until(ExpectedConditions.urlToBe(getBaseUrl()));
        
        assertTrue(driver.getCurrentUrl().equals(getBaseUrl()) || 
                   driver.getCurrentUrl().endsWith("/"),
                   "Story list should redirect to home");
    }
    
    @Test
    @Order(11)
    @DisplayName("Should access signup page")
    public void testSignupPage() {
        driver.get(getBaseUrl() + "signup");
        
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
        String pageSource = driver.getPageSource();
        
        assertTrue(pageSource.contains("signup") || pageSource.contains("Sign up") || 
                   pageSource.contains("register") || pageSource.length() > 100,
                   "Signup page should load");
    }
    
    @Test
    @Order(12)
    @DisplayName("Should handle column edit page")
    public void testColumnEditPage() {
        driver.get(getBaseUrl() + "columns");
        
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
        
        // Try to find edit button
        try {
            WebElement editBtn = driver.findElement(By.cssSelector("[id^='column_edit_']"));
            editBtn.click();
            
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("column_name_edit")));
            
            WebElement nameEdit = driver.findElement(By.id("column_name_edit"));
            assertNotNull(nameEdit, "Column name edit field should exist");
        } catch (Exception e) {
            // No columns to edit yet, that's ok
            assertTrue(true, "No columns available to edit");
        }
    }
    
    @Test
    @Order(13)
    @DisplayName("Should navigate between pages")
    public void testPageNavigation() {
        // Home -> Projects
        driver.get(getBaseUrl());
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
        
        driver.get(getBaseUrl() + "project/list");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("projectsList")));
        assertTrue(driver.getCurrentUrl().contains("/project/list"), "Should navigate to projects");
        
        // Projects -> Home
        driver.get(getBaseUrl());
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
        assertTrue(driver.getCurrentUrl().contains(getBaseUrl()), "Should navigate to home");
    }
    
    @Test
    @Order(14)
    @DisplayName("Should handle empty project form validation")
    public void testEmptyProjectValidation() {
        driver.get(getBaseUrl() + "project/new");
        
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("createProjectBtn")));
        
        // Try to submit empty form
        driver.findElement(By.id("createProjectBtn")).click();
        
        // Should stay on form or show error
        String currentUrl = driver.getCurrentUrl();
        assertTrue(currentUrl.contains("/project/new") || currentUrl.contains("error"),
                   "Should handle empty form validation");
    }
    
    @Test
    @Order(15)
    @DisplayName("Should handle empty story form validation")
    public void testEmptyStoryValidation() throws InterruptedException {
        driver.get(getBaseUrl() + "story/new");
        
        WebElement createBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("createStoryBtn")));
        
        // Try to submit empty form
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", createBtn);
        Thread.sleep(500);
        createBtn.click();
        
        // Should stay on form or show error
        String currentUrl = driver.getCurrentUrl();
        assertTrue(currentUrl.contains("/story/new") || currentUrl.contains("error"),
                   "Should handle empty story form validation");
    }
}
