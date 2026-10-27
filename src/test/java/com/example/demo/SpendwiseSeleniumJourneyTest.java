package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(OrderAnnotation.class)
class SpendwiseSeleniumJourneyTest {

    private static final String ACCOUNT_NAME = "Selenium Journey User";
    private static final String ACCOUNT_EMAIL = "selenium.journey@example.test";
    private static final String EXPENSE_DESCRIPTION = "selenium-journey-expense";

    @LocalServerPort
    private int port;

    private WebDriver driver;
    private WebDriverWait wait;
    private Path downloadDirectory;

    @RegisterExtension
    final AfterTestExecutionCallback failureScreenshot = new AfterTestExecutionCallback() {
        @Override
        public void afterTestExecution(ExtensionContext context) {
            if (context.getExecutionException().isPresent() && driver != null) {
                captureFailureScreenshot(context.getRequiredTestMethod().getName());
            }
        }
    };

    @BeforeEach
    void startBrowser() throws IOException {
        downloadDirectory = Path.of("target", "selenium-downloads", UUID.randomUUID().toString())
                .toAbsolutePath();
        Files.createDirectories(downloadDirectory);

        EdgeOptions options = new EdgeOptions();
        options.addArguments(
                "--headless=new",
                "--disable-gpu",
                "--window-size=1440,1000",
                "--no-first-run",
                "--no-default-browser-check",
                "--disable-extensions");
        options.addArguments("--user-data-dir=" + downloadDirectory.resolve("edge-profile").toString());
        Map<String, Object> preferences = new HashMap<>();
        preferences.put("download.default_directory", downloadDirectory.toString());
        preferences.put("download.prompt_for_download", false);
        preferences.put("safebrowsing.enabled", true);
        options.setExperimentalOption("prefs", preferences);

        driver = new EdgeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    @AfterEach
    void stopBrowser() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }

    @Test
    @Order(1)
    void createAccountShowsSuccessAndProfile() {
        driver.get(baseUrl());
        assertEquals("Set up your account", driver.findElement(By.tagName("h1")).getText());

        driver.findElement(By.name("name")).sendKeys(ACCOUNT_NAME);
        driver.findElement(By.name("email")).sendKeys(ACCOUNT_EMAIL);
        driver.findElement(By.cssSelector("form[action='/account'] button[type='submit']")).click();

        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.cssSelector("[role='status']"), "Account profile saved successfully."));
        assertEquals(ACCOUNT_NAME, driver.findElement(By.cssSelector(".profile-name")).getText());
        assertEquals(ACCOUNT_EMAIL, driver.findElement(By.cssSelector(".profile-email")).getText());
        captureSuccessScreenshot("SJ-01-account-created");
    }

    @Test
    @Order(2)
    void addExpenseUpdatesRecentTransactionsAndTotals() {
        driver.get(baseUrl());
        driver.findElement(By.name("amount")).sendKeys("123.45");
        driver.findElement(By.name("category")).sendKeys("Selenium Test");
        driver.findElement(By.name("description")).sendKeys(EXPENSE_DESCRIPTION);
        driver.findElement(By.cssSelector("form[action='/expenses'] button[type='submit']")).click();

        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.cssSelector("[role='status']"), "Expense recorded. Your summary is up to date."));
        assertTrue(driver.findElement(By.cssSelector("tbody")).getText().contains(EXPENSE_DESCRIPTION));
        assertEquals("₹123.45", driver.findElement(By.cssSelector(".metrics .metric:nth-child(1) .metric-value")).getText(), "Total spending should show the sum of all expense amounts.");
        assertEquals("₹123.45", driver.findElement(By.cssSelector(".metrics .metric:nth-child(2) .metric-value")).getText(), "This month should show the month total.");
        assertTrue(driver.findElement(By.cssSelector(".category-list")).getText().contains("Selenium Test"));
        captureSuccessScreenshot("SJ-02-expense-summary");
    }

    @Test
    @Order(3)
    void negativeAmountIsBlockedAndDoesNotCreateATransaction() {
        driver.get(baseUrl());
        int transactionsBefore = driver.findElements(By.cssSelector("tbody tr")).size();
        WebElement amount = driver.findElement(By.name("amount"));
        amount.sendKeys("-5");

        boolean belowMinimum = (Boolean) ((JavascriptExecutor) driver)
                .executeScript("return arguments[0].validity.rangeUnderflow;", amount);
        assertTrue(belowMinimum, "A negative amount should violate the input minimum.");

        driver.findElement(By.cssSelector("form[action='/expenses'] button[type='submit']")).click();
        assertEquals(baseUrl(), driver.getCurrentUrl(), "Browser validation should prevent form submission.");
        assertEquals(transactionsBefore, driver.findElements(By.cssSelector("tbody tr")).size());
        String validationMessage = (String) ((JavascriptExecutor) driver)
                .executeScript("return arguments[0].validationMessage;", amount);
        assertFalse(validationMessage.isBlank());
        captureSuccessScreenshot("SJ-03-invalid-amount-blocked");
    }

    @Test
    @Order(4)
    void downloadCsvAndReceiptWithTheRecordedExpense() throws IOException {
        driver.get(baseUrl());
        driver.findElement(By.cssSelector("a.download")).click();
        Path csv = waitForDownload("expense-summary.csv");
        String csvContent = Files.readString(csv, StandardCharsets.UTF_8);
        assertTrue(csvContent.startsWith("Receipt ID,Date,Category,Description,Amount"));
        assertTrue(csvContent.contains(EXPENSE_DESCRIPTION));
        assertTrue(csvContent.contains("123.45"));

        WebElement receiptLink = driver.findElement(By.cssSelector("a.receipt"));
        String receiptUrl = receiptLink.getAttribute("href");
        String receiptId = receiptUrl.replaceFirst(".*/expenses/(\\d+)/receipt$", "$1");
        receiptLink.click();

        Path receipt = waitForDownload("expense-receipt-" + receiptId + ".txt");
        String receiptContent = Files.readString(receipt, StandardCharsets.UTF_8);
        assertTrue(receiptContent.contains(EXPENSE_DESCRIPTION));
        assertTrue(receiptContent.contains(ACCOUNT_NAME));
        assertTrue(receiptContent.contains("INR ₹123.45"));
        driver.get(baseUrl());
        captureSuccessScreenshot("SJ-04-csv-and-receipt-download");
    }

    private String baseUrl() {
        return "http://localhost:" + port + "/";
    }

    private Path waitForDownload(String fileName) {
        Path file = downloadDirectory.resolve(fileName);
        return wait.until(ignored -> Files.isRegularFile(file) ? file : null);
    }

    private void captureSuccessScreenshot(String screenshotName) {
        try {
            Path directory = Path.of("Evidence", "Task-09", "screenshots").toAbsolutePath();
            Files.createDirectories(directory);
            Path destination = directory.resolve(screenshotName + ".png");
            Path temporaryScreenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE).toPath();
            Files.copy(temporaryScreenshot, destination, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Saved Selenium journey screenshot: " + destination);
        } catch (Exception screenshotError) {
            throw new IllegalStateException("Could not save Selenium journey screenshot: " + screenshotError.getMessage(), screenshotError);
        }
    }
    private void captureFailureScreenshot(String testName) {
        try {
            Path directory = Path.of("Evidence", "Task-09", "failure-screenshots").toAbsolutePath();
            Files.createDirectories(directory);
            String safeName = testName.replaceAll("[^A-Za-z0-9._-]", "_");
            Path destination = directory.resolve(safeName + ".png");
            Path temporaryScreenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE).toPath();
            Files.copy(temporaryScreenshot, destination, StandardCopyOption.REPLACE_EXISTING);
            System.err.println("Saved Selenium failure screenshot: " + destination);
        } catch (Exception screenshotError) {
            System.err.println("Could not save Selenium failure screenshot: " + screenshotError.getMessage());
        }
    }
}


