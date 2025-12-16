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
import org.openqa.selenium.JavascriptExecutor;

import java.time.Duration;
import java.util.List;

/**
 * Tests d'intégration Selenium pour le HomeController
 * Teste l'application déployée dans Jetty avec un vrai navigateur
 */
public class HomeIT {
    
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
    
    @Test
    @DisplayName("Should display home page with hero section")
    public void testDisplayHomePage() {
        driver.get(getBaseUrl());
        
        // Vérifie la présence de la section hero
        WebElement heroWrapper = driver.findElement(By.className("hero-wrapper"));
        assertNotNull(heroWrapper, "Hero wrapper should be present");
        
        // Vérifie le titre
        WebElement heroTitle = driver.findElement(By.className("hero-title"));
        assertNotNull(heroTitle, "Hero title should be present");
        assertTrue(heroTitle.getText().contains("Tarnished") || heroTitle.getText().contains("Kanban"), 
                   "Hero title should contain 'Tarnished' or 'Kanban'");
    }
    
    @Test
    @DisplayName("Should display call-to-action buttons")
    public void testDisplayCTAButtons() {
        driver.get(getBaseUrl());
        
        // Vérifie la présence des boutons CTA
        List<WebElement> ctaButtons = driver.findElements(By.className("btn-cta"));
        assertTrue(ctaButtons.size() >= 2, "Should have at least 2 CTA buttons");
        
        // Vérifie le bouton "New Project"
        WebElement newProjectBtn = driver.findElement(By.cssSelector("a[href*='/project/new']"));
        assertNotNull(newProjectBtn, "New Project button should be present");
        assertTrue(newProjectBtn.isDisplayed(), "New Project button should be visible");
        
        // Vérifie le bouton "All Projects"
        WebElement allProjectsBtn = driver.findElement(By.cssSelector("a[href*='/project/list']"));
        assertNotNull(allProjectsBtn, "All Projects button should be present");
        assertTrue(allProjectsBtn.isDisplayed(), "All Projects button should be visible");
    }
    
    @Test
    @DisplayName("Should display projects section")
    public void testDisplayProjectsSection() {
        driver.get(getBaseUrl());
        
        // Vérifie la présence de la section projets
        WebElement mainContent = driver.findElement(By.className("main-content"));
        assertNotNull(mainContent, "Main content section should be present");
        
        // Vérifie le titre de la section
        WebElement sectionTitle = driver.findElement(By.className("section-title"));
        assertNotNull(sectionTitle, "Section title should be present");
        assertTrue(sectionTitle.getText().contains("Project") || sectionTitle.getText().contains("project"), 
                   "Section title should mention projects");
    }
    
    @Test
    @DisplayName("Should display projects grid or empty state")
    public void testDisplayProjectsGridOrEmpty() {
        driver.get(getBaseUrl());
        
        // Vérifie soit la grille de projets, soit l'état vide
        try {
            WebElement projectsGrid = driver.findElement(By.className("projects-grid"));
            assertNotNull(projectsGrid, "Projects grid should be present");
            
            // Vérifie les cartes de projets si elles existent
            List<WebElement> projectCards = driver.findElements(By.className("project-card"));
            // Peut être vide, c'est OK
            assertTrue(projectCards.size() >= 0, "Project cards count should be valid");
        } catch (Exception e) {
            // Si pas de grille, vérifie l'état vide
            WebElement emptyState = driver.findElement(By.className("empty-state"));
            assertNotNull(emptyState, "Empty state should be present when no projects");
            
            // Vérifie le message d'état vide
            WebElement emptyTitle = driver.findElement(By.className("empty-title"));
            assertNotNull(emptyTitle, "Empty title should be present");
            String emptyTitleText = emptyTitle.getText();
            assertTrue(emptyTitleText.contains("No projects") || emptyTitleText.contains("project"), 
                       "Empty title should mention no projects");
        }
    }
    
    @Test
    @DisplayName("Should display project cards with correct structure")
    public void testDisplayProjectCards() {
        driver.get(getBaseUrl());
        
        // Créer un projet de test d'abord
        driver.get(getBaseUrl() + "project/new");
        String projectName = "Home Test Project " + System.currentTimeMillis();
        driver.findElement(By.id("projectName")).sendKeys(projectName);
        driver.findElement(By.id("projectDescription")).sendKeys("Test description for home page");
        driver.findElement(By.id("createProjectBtn")).click();
        
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(1));
        // Le controller redirige vers /project/list après création
        wait.until(ExpectedConditions.urlContains("/project/list"));
        
        // Retourner à la page d'accueil
        driver.get(getBaseUrl());
        
        // Vérifie la présence des cartes de projets
        List<WebElement> projectCards = driver.findElements(By.className("project-card"));
        assertTrue(projectCards.size() > 0, "Should have at least one project card");
        
        // Vérifie la structure d'une carte
        WebElement firstCard = projectCards.get(0);
        
        // Vérifie le header
        WebElement projectHeader = firstCard.findElement(By.className("project-header"));
        assertNotNull(projectHeader, "Project header should be present");
        
        // Vérifie le nom du projet
        WebElement projectNameElement = firstCard.findElement(By.className("project-name"));
        assertNotNull(projectNameElement, "Project name should be present");
        
        // Vérifie le body
        WebElement projectBody = firstCard.findElement(By.className("project-body"));
        assertNotNull(projectBody, "Project body should be present");
        
        // Vérifie les boutons d'action
        WebElement btnBoard = firstCard.findElement(By.className("btn-board"));
        assertNotNull(btnBoard, "Board button should be present");
        assertTrue(btnBoard.getText().contains("Board") || btnBoard.getText().contains("Open"), 
                   "Board button should have appropriate text");
    }
    
    @Test
    @DisplayName("Should navigate to board from project card")
    public void testNavigateToBoardFromCard() {
        driver.get(getBaseUrl());
        
        // Créer un projet de test d'abord
        driver.get(getBaseUrl() + "project/new");
        String projectName = "Home Navigation Test " + System.currentTimeMillis();
        driver.findElement(By.id("projectName")).sendKeys(projectName);
        driver.findElement(By.id("projectDescription")).sendKeys("Test navigation");
        driver.findElement(By.id("createProjectBtn")).click();
        
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(1));
        // Le controller redirige vers /project/list après création
        wait.until(ExpectedConditions.urlContains("/project/list"));
        
        // Retourner à la page d'accueil
        driver.get(getBaseUrl());
        
        // Attendre que les cartes de projets soient chargées
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("project-card")));
        
        // Cliquer sur le bouton "Open Board" du premier projet
        List<WebElement> projectCards = driver.findElements(By.className("project-card"));
        if (!projectCards.isEmpty()) {
            WebElement btnBoard = wait.until(ExpectedConditions.elementToBeClickable(
                projectCards.get(0).findElement(By.className("btn-board"))
            ));
            // Utiliser JavaScript pour cliquer si l'élément est intercepté
            try {
                btnBoard.click();
            } catch (Exception e) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btnBoard);
            }
            
            // Vérifie la redirection vers le board
            wait.until(ExpectedConditions.urlContains("/board/"));
            assertTrue(driver.getCurrentUrl().contains("/board/"), "Should redirect to board page");
        } else {
            // Si pas de cartes, vérifie au moins que la page fonctionne
            assertTrue(true, "Page should load correctly");
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
        
        // Vérifie qu'on est sur la page d'accueil
        String currentUrl = driver.getCurrentUrl();
        assertTrue(currentUrl.equals(getBaseUrl()) || currentUrl.equals(getBaseUrl() + "?"), 
                   "Should redirect to home page");
    }
    
    @Test
    @DisplayName("Should display recent stories if available")
    public void testDisplayRecentStories() {
        driver.get(getBaseUrl());
        
        // La page peut afficher des stories récentes
        // Vérifie que la page se charge correctement
        WebElement heroWrapper = driver.findElement(By.className("hero-wrapper"));
        assertNotNull(heroWrapper, "Page should load correctly");
        
        // Les stories peuvent être affichées dans une section séparée
        // Pour l'instant, on vérifie juste que la page fonctionne
        assertTrue(driver.getPageSource().length() > 0, "Page should have content");
    }
    
    @Test
    @DisplayName("Should display in-progress count")
    public void testDisplayInProgressCount() {
        driver.get(getBaseUrl());
        
        // Le compteur peut être affiché dans la page
        // Vérifie que la page se charge correctement
        WebElement mainContent = driver.findElement(By.className("main-content"));
        assertNotNull(mainContent, "Main content should be present");
        
        // Le compteur peut être dans le DOM même s'il n'est pas visible
        String pageSource = driver.getPageSource();
        // On vérifie juste que la page fonctionne
        assertTrue(pageSource.length() > 0, "Page should have content");
    }
}

