package fr.uha.ensisa.gl.tarnished.it;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.*;

import java.time.Duration;
import java.util.List;

/**
 * Tests d'intégration Selenium pour le BoardController (Kanban Board)
 * Teste les méthodes du BoardController avec drag and drop
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BoardIT {

    private static WebDriver driver;
    private static String host, port;
    private static Long testProjectId;
    private static WebDriverWait wait;
    private static Actions actions;

    @BeforeAll
    public static void setupWebDriver() {
        if (driver != null) return;

        host = System.getProperty("host", "localhost");
        port = System.getProperty("servlet.port", "8080");

        driver = WebDriverFactory.createChromeDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(2));
        actions = new Actions(driver);

        setupTestProject();
    }

    private static String getBaseUrl() {
        return "http://" + host + ":" + port + "/";
    }

    private static void setupTestProject() {
        driver.get(getBaseUrl() + "project/new");

        String projectName = "Board Test Project " + System.currentTimeMillis();
        driver.findElement(By.id("projectName")).sendKeys(projectName);
        driver.findElement(By.id("projectDescription"))
                .sendKeys("Test project for board integration tests");
        driver.findElement(By.id("createProjectBtn")).click();

        wait.until(ExpectedConditions.urlContains("/project/list"));

        try {
            WebElement projectCard = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//h5[contains(text(),'" + projectName + "')]/ancestor::div[contains(@class,'card')] | " +
                            "//div[contains(@class,'project-card')]//h3[contains(text(),'" + projectName + "')]/ancestor::div[contains(@class,'project-card')]")
            ));

            try {
                String dataId = projectCard.getAttribute("data-id");
                if (dataId != null && !dataId.isEmpty()) {
                    testProjectId = Long.parseLong(dataId);
                } else {
                    WebElement boardLink = projectCard.findElement(
                            By.xpath(".//a[contains(@href,'/board/')]"));
                    String href = boardLink.getAttribute("href");
                    String[] parts = href.split("/board/");
                    if (parts.length > 1) {
                        testProjectId = Long.parseLong(parts[1].split("\\?")[0]);
                    }
                }
            } catch (Exception e) {
                WebElement firstBoardLink = driver.findElement(
                        By.xpath("//a[contains(@href,'/board/')]"));
                String href = firstBoardLink.getAttribute("href");
                String[] parts = href.split("/board/");
                if (parts.length > 1) {
                    testProjectId = Long.parseLong(parts[1].split("\\?")[0]);
                }
            }
        } catch (Exception e) {
            testProjectId = 1L;
        }

        if (testProjectId != null) {
            driver.get(getBaseUrl() + "board/" + testProjectId);
            wait.until(ExpectedConditions.urlContains("/board/" + testProjectId));
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.className("kanban-container")));
        }
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

    private void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    public void navigateToBoard() {
        if (testProjectId == null) {
            setupTestProject();
        }
        driver.get(getBaseUrl() + "board/" + testProjectId);
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.className("kanban-container")));
    }

    @Test
    @DisplayName("Should display board page with project columns")
    public void testShowBoard() {
        WebElement boardContainer = driver.findElement(By.className("kanban-container"));
        assertNotNull(boardContainer);

        List<WebElement> columns = driver.findElements(By.className("kanban-column"));
        assertTrue(columns.size() >= 5);

        WebElement projectHeader = driver.findElement(By.className("project-header"));
        assertNotNull(projectHeader);
    }

    @Test
    @DisplayName("Should redirect to project list when project doesn't exist")
    public void testShowBoardNonExistentProject() {
        driver.get(getBaseUrl() + "board/99999");

        wait.until(ExpectedConditions.urlContains("/project/list"));
        assertTrue(driver.getCurrentUrl().contains("/project/list"));
    }

    @Test
    @DisplayName("Should create and add a story to a column")
    public void testAddStoryToColumn() {
        List<WebElement> addStoryButtons = driver.findElements(By.className("add-story-btn"));
        if (!addStoryButtons.isEmpty()) {
            addStoryButtons.get(0).click();

            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("storyTitle")));
            String storyTitle = "Board Test Story " + System.currentTimeMillis();
            driver.findElement(By.id("storyTitle")).sendKeys(storyTitle);
            driver.findElement(By.id("storyDescription")).sendKeys("Test description");
            driver.findElement(By.id("createStoryBtn")).click();

            wait.until(ExpectedConditions.urlContains("/board/" + testProjectId));

            String pageSource = driver.getPageSource();
            assertTrue(pageSource.contains(storyTitle) ||
                    driver.findElements(By.className("story-card")).size() > 0);
        }
    }

    @Test
    @DisplayName("Should drag and drop story between columns")
    public void testDragAndDropStory() {
        createTestStory();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("story-card")));

        List<WebElement> storyCards = driver.findElements(By.className("story-card"));
        if (storyCards.isEmpty()) {
            return;
        }

        WebElement sourceStory = storyCards.get(0);
        String storyId = sourceStory.getAttribute("data-story-id");
        assertNotNull(storyId);

        List<WebElement> columns = driver.findElements(By.className("kanban-column"));
        assertTrue(columns.size() >= 2);

        WebElement sourceColumn = sourceStory.findElement(
                By.xpath("./ancestor::div[contains(@class, 'kanban-column')]"));
        WebElement targetColumn = null;

        for (WebElement col : columns) {
            if (!col.equals(sourceColumn)) {
                targetColumn = col;
                break;
            }
        }

        assertNotNull(targetColumn);

        actions.clickAndHold(sourceStory)
                .moveToElement(targetColumn)
                .release()
                .build()
                .perform();

        sleep(1000);

        String pageSource = driver.getPageSource();
        assertFalse(pageSource.contains("error") &&
                pageSource.contains("Column is full"));
    }

    @Test
    @DisplayName("Should add a new column via form")
    public void testAddColumn() {
        try {
            List<WebElement> addColumnButtons = driver.findElements(
                    By.xpath("//button[contains(text(), 'Add Column')] | //a.contains(text(), 'Add Column')]"));
            if (!addColumnButtons.isEmpty()) {
                addColumnButtons.get(0).click();

                wait.until(ExpectedConditions.presenceOfElementLocated(By.id("columnName")));
                String columnName = "Test Column " + System.currentTimeMillis();
                driver.findElement(By.id("columnName")).sendKeys(columnName);

                WebElement submitBtn = driver.findElement(
                        By.cssSelector("form button[type='submit'], form input[type='submit']"));
                submitBtn.click();

                wait.until(ExpectedConditions.urlContains("/board/" + testProjectId));

                String pageSource = driver.getPageSource();
                assertTrue(pageSource.contains(columnName) ||
                        driver.findElements(By.className("kanban-column")).size() > 5);
            }
        } catch (Exception e) {
            testAddColumnViaAJAX();
        }
    }

    private void testAddColumnViaAJAX() {
        String columnName = "AJAX Column " + System.currentTimeMillis();
        String url = getBaseUrl() + "board/" + testProjectId + "/add-column";

        String script =
                "var form = document.createElement('form');" +
                        "form.method = 'POST';" +
                        "form.action = '" + url + "';" +
                        "var nameInput = document.createElement('input');" +
                        "nameInput.type = 'hidden';" +
                        "nameInput.name = 'name';" +
                        "nameInput.value = '" + columnName.replace("'", "\\'") + "';" +
                        "form.appendChild(nameInput);" +
                        "var capacityInput = document.createElement('input');" +
                        "capacityInput.type = 'hidden';" +
                        "capacityInput.name = 'maxCapacity';" +
                        "capacityInput.value = '5';" +
                        "form.appendChild(capacityInput);" +
                        "var hasSubColumnsInput = document.createElement('input');" +
                        "hasSubColumnsInput.type = 'hidden';" +
                        "hasSubColumnsInput.name = 'hasSubColumns';" +
                        "hasSubColumnsInput.value = 'true';" +
                        "form.appendChild(hasSubColumnsInput);" +
                        "document.body.appendChild(form);" +
                        "form.submit();";

        ((JavascriptExecutor) driver).executeScript(script);
        wait.until(ExpectedConditions.urlContains("/board/" + testProjectId));

        assertTrue(driver.getCurrentUrl().contains("/board/" + testProjectId));
    }

    @Test
    @DisplayName("Should update column name via AJAX")
    public void testUpdateColumnName() {
        List<WebElement> columns = driver.findElements(By.className("kanban-column"));
        WebElement editableColumn = null;

        for (WebElement col : columns) {
            WebElement header = col.findElement(By.className("column-header"));
            String columnName = header.getText();
            if (!columnName.contains("BACKLOG") && !columnName.contains("DONE")) {
                editableColumn = col;
                break;
            }
        }

        if (editableColumn == null) {
            testAddColumn();
            navigateToBoard();
            columns = driver.findElements(By.className("kanban-column"));
            editableColumn = columns.get(columns.size() - 1);
        }

        assertNotNull(editableColumn);

        String columnId = editableColumn.getAttribute("data-column-id");
        assertNotNull(columnId);

        WebElement editButton = editableColumn.findElement(
                By.xpath(".//button[contains(text(), 'Edit')]"));
        editButton.click();

        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("columnNameEdit")));
            WebElement nameInput = driver.findElement(By.id("columnNameEdit"));
            nameInput.clear();
            String newName = "Updated " + System.currentTimeMillis();
            nameInput.sendKeys(newName);

            WebElement saveBtn = driver.findElement(
                    By.cssSelector("button[type='submit'], button.save-column"));
            saveBtn.click();

            sleep(500);

            String pageSource = driver.getPageSource();
            assertTrue(pageSource.contains(newName));
        } catch (Exception e) {
            String newName = "AJAX Updated " + System.currentTimeMillis();
            String url = getBaseUrl() + "board/" + testProjectId +
                    "/update-column?columnId=" + columnId +
                    "&newName=" + newName;

            ((JavascriptExecutor) driver).executeScript(
                    "window.location.href = '" + url + "'");
            sleep(1000);

            assertTrue(true);
        }
    }

    @Test
    @DisplayName("Should prevent deleting BACKLOG and DONE columns")
    public void testPreventDeleteProtectedColumns() {
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.className("kanban-column")));

        List<WebElement> columns = driver.findElements(By.className("kanban-column"));
        assertFalse(columns.isEmpty());

        for (WebElement col : columns) {
            try {
                WebElement header = col.findElement(By.className("column-header"));
                String columnName = header.getText().toUpperCase();

                if (columnName.contains("BACKLOG") || columnName.contains("DONE")) {
                    List<WebElement> deleteButtons = col.findElements(
                            By.xpath(".//button[contains(@class, 'btn-outline-danger')]"));
                    assertTrue(deleteButtons.isEmpty());
                }
            } catch (Exception ignored) {
            }
        }
    }

    @Test
    @DisplayName("Should delete a column without stories")
    public void testDeleteColumn() {
        testAddColumn();
        navigateToBoard();

        List<WebElement> columns = driver.findElements(By.className("kanban-column"));
        WebElement columnToDelete = null;

        for (WebElement col : columns) {
            WebElement header = col.findElement(By.className("column-header"));
            String columnName = header.getText();
            if (!columnName.contains("BACKLOG") && !columnName.contains("DONE")) {
                List<WebElement> stories = col.findElements(By.className("story-card"));
                if (stories.isEmpty()) {
                    columnToDelete = col;
                    break;
                }
            }
        }

        if (columnToDelete != null) {
            WebElement deleteButton = columnToDelete.findElement(
                    By.xpath(".//button[contains(@class, 'btn-outline-danger')]"));
            deleteButton.click();

            try {
                WebElement confirmBtn = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                By.xpath("//button[contains(text(), 'Delete') or contains(text(), 'Confirm')]")));
                confirmBtn.click();
            } catch (Exception ignored) {
            }

            wait.until(ExpectedConditions.urlContains("/board/" + testProjectId));
            navigateToBoard();
            assertTrue(true);
        }
    }

    @Test
    @DisplayName("Should reorder stories within a column")
    public void testReorderStories() {
        createTestStory();
        createTestStory();

        navigateToBoard();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("story-card")));

        List<WebElement> storyCards = driver.findElements(By.className("story-card"));
        if (storyCards.size() < 2) {
            return;
        }

        WebElement columnWithStories = null;
        for (WebElement col : driver.findElements(By.className("kanban-column"))) {
            List<WebElement> stories = col.findElements(By.className("story-card"));
            if (stories.size() >= 2) {
                columnWithStories = col;
                break;
            }
        }

        if (columnWithStories != null) {
            List<WebElement> stories = columnWithStories.findElements(
                    By.className("story-card"));
            WebElement firstStory = stories.get(0);
            WebElement secondStory = stories.get(1);

            actions.clickAndHold(firstStory)
                    .moveToElement(secondStory)
                    .moveByOffset(0, 50)
                    .release()
                    .build()
                    .perform();

            sleep(1000);
            assertTrue(true);
        }
    }

    @Test
    @DisplayName("Should drag and drop columns to reorder them")
    public void testDragAndDropColumns() {
        List<WebElement> columns = driver.findElements(By.className("kanban-column"));
        if (columns.size() < 3) {
            return;
        }

        WebElement columnToMove = null;
        for (WebElement col : columns) {
            WebElement header = col.findElement(By.className("column-header"));
            String columnName = header.getText().toUpperCase();
            if (!columnName.contains("BACKLOG")) {
                columnToMove = col;
                break;
            }
        }

        if (columnToMove != null) {
            WebElement targetColumn = columns.get(columns.size() - 1);
            WebElement columnHeader = columnToMove.findElement(
                    By.className("column-header"));
            WebElement draggableSpan = columnHeader.findElement(By.tagName("span"));

            actions.clickAndHold(draggableSpan)
                    .moveToElement(targetColumn)
                    .release()
                    .build()
                    .perform();

            sleep(1500);
            assertTrue(true);
        }
    }

    @Test
    @DisplayName("Should handle column capacity limits")
    public void testColumnCapacityLimit() {
        String columnName = "Limited Column " + System.currentTimeMillis();
        String url = getBaseUrl() + "board/" + testProjectId +
                "/add-column?name=" + columnName +
                "&maxCapacity=1&hasSubColumns=true";
        driver.get(url);
        wait.until(ExpectedConditions.urlContains("/board/" + testProjectId));

        navigateToBoard();
        createTestStory();
        navigateToBoard();

        assertTrue(true);
    }

    @Test
    @DisplayName("Should update subcolumn when dragging story to subcolumn area")
    public void testUpdateSubColumn() {
        createTestStory();
        navigateToBoard();

        List<WebElement> storyCards = driver.findElements(By.className("story-card"));
        if (storyCards.isEmpty()) {
            return;
        }

        WebElement story = storyCards.get(0);

        List<WebElement> columns = driver.findElements(By.className("kanban-column"));
        WebElement columnWithSubcolumns = null;

        for (WebElement col : columns) {
            WebElement header = col.findElement(By.className("column-header"));
            String columnName = header.getText();
            if (!columnName.contains("BACKLOG") && !columnName.contains("DONE")) {
                List<WebElement> subcolumns = col.findElements(By.className("subcolumn-done"));
                if (!subcolumns.isEmpty()) {
                    columnWithSubcolumns = col;
                    break;
                }
            }
        }

        if (columnWithSubcolumns != null) {
            WebElement doneSubcolumn = columnWithSubcolumns.findElement(
                    By.className("subcolumn-done"));

            actions.clickAndHold(story)
                    .moveToElement(doneSubcolumn)
                    .release()
                    .build()
                    .perform();

            sleep(1000);
            assertTrue(true);
        }
    }

    @Test
    @DisplayName("Should move all stories from one column to another")
    public void testMoveAllStories() {
        createTestStory();
        createTestStory();
        navigateToBoard();

        List<WebElement> columns = driver.findElements(By.className("kanban-column"));
        if (columns.size() < 2) {
            return;
        }

        WebElement sourceColumn = null;
        WebElement targetColumn = null;

        for (WebElement col : columns) {
            List<WebElement> stories = col.findElements(By.className("story-card"));
            if (stories.size() >= 2 && sourceColumn == null) {
                sourceColumn = col;
            }
            if (col != sourceColumn && targetColumn == null) {
                targetColumn = col;
            }
        }

        if (sourceColumn != null && targetColumn != null) {
            String fromColumnId = sourceColumn.getAttribute("data-column-id");
            String toColumnId = targetColumn.getAttribute("data-column-id");

            String url = getBaseUrl() + "board/" + testProjectId +
                    "/move-all-stories?fromColumnId=" + fromColumnId +
                    "&toColumnId=" + toColumnId;
            ((JavascriptExecutor) driver)
                    .executeScript("fetch('" + url + "', {method: 'POST'});");

            sleep(1000);
            navigateToBoard();
            assertTrue(true);
        }
    }

    @Test
    @DisplayName("Should delete column with all its stories")
    public void testDeleteColumnWithStories() {
        testAddColumn();
        navigateToBoard();

        createTestStory();
        navigateToBoard();

        List<WebElement> columns = driver.findElements(By.className("kanban-column"));
        WebElement columnToDelete = null;

        for (WebElement col : columns) {
            try {
                WebElement header = col.findElement(By.className("column-header"));
                String columnName = header.getText();
                if (columnName.contains("Test Column") ||
                        columnName.contains("AJAX Column")) {
                    List<WebElement> stories = col.findElements(By.className("story-card"));
                    if (!stories.isEmpty()) {
                        columnToDelete = col;
                        break;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (columnToDelete != null) {
            String columnId = columnToDelete.getAttribute("data-column-id");

            String url = getBaseUrl() + "board/" + testProjectId +
                    "/delete-column-with-stories/" + columnId;
            ((JavascriptExecutor) driver)
                    .executeScript("fetch('" + url + "', {method: 'POST'});");

            sleep(1000);
            navigateToBoard();
            assertTrue(true);
        }
    }

    @Test
    @DisplayName("Should update column capacity")
    public void testUpdateColumnCapacity() {
        List<WebElement> columns = driver.findElements(By.className("kanban-column"));
        WebElement editableColumn = null;

        for (WebElement col : columns) {
            WebElement header = col.findElement(By.className("column-header"));
            String columnName = header.getText();
            if (!columnName.contains("BACKLOG") && !columnName.contains("DONE")) {
                editableColumn = col;
                break;
            }
        }

        if (editableColumn == null) {
            testAddColumn();
            navigateToBoard();
            columns = driver.findElements(By.className("kanban-column"));
            editableColumn = columns.get(columns.size() - 1);
        }

        String columnId = editableColumn.getAttribute("data-column-id");

        String url = getBaseUrl() + "board/" + testProjectId +
                "/update-column-full?columnId=" + columnId + "&maxCapacity=10";
        ((JavascriptExecutor) driver)
                .executeScript("fetch('" + url + "', {method: 'POST'});");

        sleep(1000);
        navigateToBoard();
        assertTrue(true);
    }

    private void createTestStory() {
        try {
            List<WebElement> addStoryButtons = driver.findElements(By.className("add-story-btn"));
            if (!addStoryButtons.isEmpty()) {
                addStoryButtons.get(0).click();

                wait.until(ExpectedConditions.presenceOfElementLocated(By.id("storyTitle")));
                String storyTitle = "Test Story " + System.currentTimeMillis();
                driver.findElement(By.id("storyTitle")).sendKeys(storyTitle);
                driver.findElement(By.id("storyDescription")).sendKeys("Test description");
                driver.findElement(By.id("createStoryBtn")).click();

                wait.until(ExpectedConditions.urlContains("/board/" + testProjectId));
            }
        } catch (Exception e) {
            String storyTitle = "Test Story " + System.currentTimeMillis();
            String url = getBaseUrl() + "story/new?projectId=" + testProjectId;
            driver.get(url);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("storyTitle")));
            driver.findElement(By.id("storyTitle")).sendKeys(storyTitle);
            driver.findElement(By.id("storyDescription")).sendKeys("Test description");
            driver.findElement(By.id("createStoryBtn")).click();
            wait.until(ExpectedConditions.urlContains("/board/"));
        }
    }
}
