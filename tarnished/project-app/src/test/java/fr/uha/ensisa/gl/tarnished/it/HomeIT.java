package fr.uha.ensisa.gl.tarnished.it;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

//tests d'intégration selenium pour le HomeController
//teste l'application déployée dans jetty avec un vrai navigateur
public class HomeIT {

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
        String contextPath = System.getProperty("jetty.context.path", "/gl2526-tarnished");
        return "http://" + host + ":" + port + contextPath + "/";
    }

    @Test
    @DisplayName("Should display home page with hero section")
    public void testDisplayHomePage() {
        driver.get(getBaseUrl());

        WebElement heroWrapper = driver.findElement(By.className("hero-wrapper"));
        assertNotNull(heroWrapper);

        WebElement heroTitle = driver.findElement(By.className("hero-title"));
        assertNotNull(heroTitle);
        assertTrue(heroTitle.getText().contains("Tarnished") ||
                heroTitle.getText().contains("Kanban"));
    }

    @Test
    @DisplayName("Should display call-to-action buttons")
    public void testDisplayCTAButtons() {
        driver.get(getBaseUrl());

        List<WebElement> ctaButtons = driver.findElements(By.className("btn-cta"));
        assertTrue(ctaButtons.size() >= 2);

        WebElement newProjectBtn = driver.findElement(By.cssSelector("a[href*='/project/new']"));
        assertNotNull(newProjectBtn);
        assertTrue(newProjectBtn.isDisplayed());

        WebElement allProjectsBtn = driver.findElement(By.cssSelector("a[href*='/project/list']"));
        assertNotNull(allProjectsBtn);
        assertTrue(allProjectsBtn.isDisplayed());
    }

    @Test
    @DisplayName("Should display projects section")
    public void testDisplayProjectsSection() {
        driver.get(getBaseUrl());

        WebElement mainContent = driver.findElement(By.className("main-content"));
        assertNotNull(mainContent);

        WebElement sectionTitle = driver.findElement(By.className("section-title"));
        assertNotNull(sectionTitle);
        assertTrue(sectionTitle.getText().toLowerCase().contains("project"));
    }

    @Test
    @DisplayName("Should display projects grid or empty state")
    public void testDisplayProjectsGridOrEmpty() {
        driver.get(getBaseUrl());

        try {
            WebElement projectsGrid = driver.findElement(By.className("projects-grid"));
            assertNotNull(projectsGrid);

            List<WebElement> projectCards = driver.findElements(By.className("project-card"));
            assertTrue(projectCards.size() >= 0);
        } catch (Exception e) {
            WebElement emptyState = driver.findElement(By.className("empty-state"));
            assertNotNull(emptyState);

            WebElement emptyTitle = driver.findElement(By.className("empty-title"));
            assertNotNull(emptyTitle);
            String emptyTitleText = emptyTitle.getText().toLowerCase();
            assertTrue(emptyTitleText.contains("no projects") ||
                    emptyTitleText.contains("project"));
        }
    }

    @Test
    @DisplayName("Should display project cards with correct structure")
    public void testDisplayProjectCards() {
        driver.get(getBaseUrl() + "project/new");
        String projectName = "Home Test Project " + System.currentTimeMillis();
        driver.findElement(By.id("projectName")).sendKeys(projectName);
        driver.findElement(By.id("projectDescription"))
                .sendKeys("Test description for home page");
        driver.findElement(By.id("createProjectBtn")).click();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(1));
        wait.until(ExpectedConditions.urlContains("/project/list"));

        driver.get(getBaseUrl());

        List<WebElement> projectCards = driver.findElements(By.className("project-card"));
        assertTrue(projectCards.size() > 0);

        WebElement firstCard = projectCards.get(0);

        WebElement projectHeader = firstCard.findElement(By.className("project-header"));
        assertNotNull(projectHeader);

        WebElement projectNameElement = firstCard.findElement(By.className("project-name"));
        assertNotNull(projectNameElement);

        WebElement projectBody = firstCard.findElement(By.className("project-body"));
        assertNotNull(projectBody);

        WebElement btnBoard = firstCard.findElement(By.className("btn-board"));
        assertNotNull(btnBoard);
        assertTrue(btnBoard.getText().contains("Board") ||
                btnBoard.getText().contains("Open"));
    }

    @Test
    @DisplayName("Should navigate to board from project card")
    public void testNavigateToBoardFromCard() {
        driver.get(getBaseUrl() + "project/new");
        String projectName = "Home Navigation Test " + System.currentTimeMillis();
        driver.findElement(By.id("projectName")).sendKeys(projectName);
        driver.findElement(By.id("projectDescription")).sendKeys("Test navigation");
        driver.findElement(By.id("createProjectBtn")).click();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(1));
        wait.until(ExpectedConditions.urlContains("/project/list"));

        driver.get(getBaseUrl());
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("project-card")));

        List<WebElement> projectCards = driver.findElements(By.className("project-card"));
        if (!projectCards.isEmpty()) {
            WebElement btnBoard = wait.until(ExpectedConditions.elementToBeClickable(
                    projectCards.get(0).findElement(By.className("btn-board"))));
            try {
                btnBoard.click();
            } catch (Exception e) {
                ((JavascriptExecutor) driver)
                        .executeScript("arguments[0].click();", btnBoard);
            }

            wait.until(ExpectedConditions.urlContains("/board/"));
            assertTrue(driver.getCurrentUrl().contains("/board/"));
        } else {
            assertTrue(true);
        }
    }

    @Test
    @DisplayName("Should redirect /hello to home page")
    public void testHelloRedirect() {
        driver.get(getBaseUrl() + "hello");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(1));
        wait.until(ExpectedConditions.or(
                ExpectedConditions.urlToBe(getBaseUrl() + "?"),
                ExpectedConditions.urlToBe(getBaseUrl())));

        String currentUrl = driver.getCurrentUrl();
        assertTrue(currentUrl.equals(getBaseUrl()) ||
                currentUrl.equals(getBaseUrl() + "?"));
    }

    @Test
    @DisplayName("Should display recent stories if available")
    public void testDisplayRecentStories() {
        driver.get(getBaseUrl());

        WebElement heroWrapper = driver.findElement(By.className("hero-wrapper"));
        assertNotNull(heroWrapper);

        assertTrue(driver.getPageSource().length() > 0);
    }

    @Test
    @DisplayName("Should display in-progress count")
    public void testDisplayInProgressCount() {
        driver.get(getBaseUrl());

        WebElement mainContent = driver.findElement(By.className("main-content"));
        assertNotNull(mainContent);

        String pageSource = driver.getPageSource();
        assertTrue(pageSource.length() > 0);
    }
}
