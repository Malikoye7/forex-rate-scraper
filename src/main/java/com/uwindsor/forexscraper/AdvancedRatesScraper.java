package com.uwindsor.forexscraper;

// Selenium wildcard import — covers WebDriver, By, WebElement,
// JavascriptExecutor, TakesScreenshot, OutputType, TimeoutException etc.
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.io.FileHandler;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

// Java imports for file and time management
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * ============================================================
 * AdvancedRatesScraper — Advanced Selenium Commands
 * ============================================================
 * This program demonstrates six advanced Selenium WebDriver
 * techniques applied to the Bank of Canada exchange rates page.
 *
 * The six techniques demonstrated are:
 *
 * 1. Explicit Wait for Page Title
 *    - Uses ExpectedConditions.titleContains() to verify
 *      the correct page is loaded before any action is taken
 *
 * 2. Cookie / Popup Handling
 *    - Uses a try-catch with WebDriverWait to look for and
 *      dismiss any cookie consent popups automatically
 *    - If no popup appears, the program continues without error
 *
 * 3. Wait for Element Visibility
 *    - Uses ExpectedConditions.visibilityOfElementLocated()
 *      which is stricter than just checking if the element
 *      exists — it confirms the element is actually visible
 *
 * 4. JavaScript Execution
 *    - Uses JavascriptExecutor to run JS inside the browser
 *    - Scrolls the page to the rates table using scrollIntoView
 *    - Reads the page title and total number of links via JS
 *
 * 5. Programmatic Screenshot
 *    - Uses TakesScreenshot to capture the browser automatically
 *    - Saves the image as boc_screenshot_task3.png without any
 *      manual action from the user
 *
 * 6. Per-Element Data Extraction with Waits
 *    - Applies the same table scraping method as the daily
 *      scraper but after explicit waits confirm the page is ready
 *    - Saves results to boc_rates_task3.csv
 *
 * Why I used ChromeOptions here:
 * ChromeOptions lets me pass launch settings to Chrome before
 * it opens. I used --start-maximized to ensure Chrome opens
 * in full screen so all page elements are fully visible.
 *
 * @author Oyewole Malik
 * ============================================================
 */
public class AdvancedRatesScraper {

    // Bank of Canada daily exchange rates page URL
    private static final String BOC_URL =
        "https://www.bankofcanada.ca/rates/exchange/daily-exchange-rates/";

    public static void main(String[] args) {

        // ChromeOptions allows me to customise Chrome's launch settings
        // --start-maximized opens the browser in full screen automatically
        // This is more reliable than calling driver.manage().window().maximize()
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");

        // Pass the ChromeOptions into the ChromeDriver constructor
        WebDriver driver = new ChromeDriver(options);

        System.out.println("=== Advanced Rates Scraper: Selenium Techniques ===\n");

        try {
            // ---------------------------------------------------------------
            // ADVANCED TECHNIQUE 1: Explicit Wait for Page Title
            // ---------------------------------------------------------------
            // The problem with basic scraping is acting before the page loads.
            // titleContains() waits until the browser title bar contains
            // "Bank of Canada" — this confirms we are on the right page.
            // If the title never matches within 20 seconds, a TimeoutException
            // is thrown, which prevents scraping the wrong page by mistake.
            System.out.println("-- Technique 1: Explicit Wait for Page Title --");
            driver.get(BOC_URL);

            // Create a WebDriverWait with 20 second maximum timeout
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

            // Wait until the page title contains "Bank of Canada"
            // Returns true when confirmed, which I store in titleLoaded
            boolean titleLoaded = wait.until(
                ExpectedConditions.titleContains("Bank of Canada")
            );
            System.out.println("  Page title confirmed: " + driver.getTitle());
            System.out.println("  Title wait succeeded: " + titleLoaded);

            // ---------------------------------------------------------------
            // ADVANCED TECHNIQUE 2: Cookie / Privacy Popup Handling
            // ---------------------------------------------------------------
            // Many websites show a cookie consent banner when first visited.
            // My program checks for one using a CSS selector that matches
            // common cookie button patterns (class or id containing "accept").
            // I used a SEPARATE WebDriverWait of only 5 seconds for this
            // because I do not want to wait 20 seconds if no popup appears.
            // The try-catch means the program continues gracefully either way.
            System.out.println("\n-- Technique 2: Cookie Popup Handling --");
            try {
                WebDriverWait popupWait = new WebDriverWait(driver, Duration.ofSeconds(5));
                WebElement acceptBtn = popupWait.until(
                    ExpectedConditions.elementToBeClickable(
                        By.cssSelector("button[class*='accept'], button[id*='accept'], " +
                                       "button[class*='cookie'], a[class*='accept']")
                    )
                );
                // If a button is found, click it to dismiss the popup
                acceptBtn.click();
                System.out.println("  Cookie popup found and dismissed.");
            } catch (TimeoutException e) {
                // No popup appeared within 5 seconds — this is fine, just continue
                System.out.println("  No cookie popup appeared — continuing.");
            }

            // ---------------------------------------------------------------
            // ADVANCED TECHNIQUE 3: Wait for Element to Be Visible on Screen
            // ---------------------------------------------------------------
            // visibilityOfElementLocated is stronger than presenceOfElementLocated:
            // - presenceOfElementLocated: element exists in HTML (may be hidden)
            // - visibilityOfElementLocated: element is actually visible on screen
            // I used visibility here to make sure the table is truly ready
            // before I try to read its contents in Techniques 5 and 6.
            System.out.println("\n-- Technique 3: Wait for Table to Be Visible --");
            WebElement table = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.tagName("table"))
            );
            System.out.println("  Exchange rate table is visible on screen.");

            // ---------------------------------------------------------------
            // ADVANCED TECHNIQUE 4: JavaScript Execution inside the Browser
            // ---------------------------------------------------------------
            // Selenium's JavascriptExecutor lets me run any JavaScript code
            // directly inside the Chrome browser from my Java program.
            // I cast the WebDriver to JavascriptExecutor to access this feature.
            // Use case 1: Scroll the page to bring the table into view
            // Use case 2: Read page metadata (title, link count) using JS
            System.out.println("\n-- Technique 4: JavaScript Execution --");
            JavascriptExecutor js = (JavascriptExecutor) driver;

            // Scroll smoothly so the exchange rate table is in the viewport
            // arguments[0] refers to the "table" WebElement I pass in
            js.executeScript("arguments[0].scrollIntoView({behavior: 'smooth'});", table);
            System.out.println("  Scrolled to the exchange rates table using JavaScript.");

            // Read page metadata using JavaScript — confirms JS execution works
            String pageTitle = (String) js.executeScript("return document.title;");
            Long linkCount   = (Long)   js.executeScript("return document.links.length;");
            System.out.println("  JS document.title   : " + pageTitle);
            System.out.println("  JS document.links   : " + linkCount + " links");

            // Brief pause of 1.5 seconds so the smooth scroll is visible
            Thread.sleep(1500);

            // ---------------------------------------------------------------
            // ADVANCED TECHNIQUE 5: Programmatic Screenshot
            // ---------------------------------------------------------------
            // TakesScreenshot is a Selenium interface that captures the browser.
            // I cast the WebDriver to TakesScreenshot to use this feature.
            // getScreenshotAs(OutputType.FILE) saves the screenshot as a File object.
            // FileHandler.copy() then copies it to my chosen file path and name.
            // This proves automated visual documentation without any manual action.
            System.out.println("\n-- Technique 5: Taking a Screenshot --");
            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            FileHandler.copy(screenshot, new File("boc_screenshot_task3.png"));
            System.out.println("  Screenshot saved: boc_screenshot_task3.png");

            // ---------------------------------------------------------------
            // ADVANCED TECHNIQUE 6: Data Extraction After Explicit Waits
            // ---------------------------------------------------------------
            // Now that all waits have confirmed the page is fully ready,
            // I extract the exchange rate data using the same table structure
            // Page structure: <th> = currency name, <td> = rate value.
            // The key difference here is that ALL the explicit waits above
            // guarantee the table is loaded and visible before I read it.
            System.out.println("\n-- Technique 6: Scrape with Per-Element Waits --");
            List<WebElement> rows = table.findElements(By.tagName("tr"));
            FileWriter csvWriter = new FileWriter("boc_rates_task3.csv");
            csvWriter.write("Currency,Rate (CAD per unit)\n");

            int count = 0;
            for (WebElement row : rows) {

                // Get currency name from <th> and rates from <td> elements
                List<WebElement> headers = row.findElements(By.tagName("th"));
                List<WebElement> cells   = row.findElements(By.tagName("td"));

                // Process only data rows (exactly 1 <th> + multiple <td>)
                if (headers.size() == 1 && !cells.isEmpty()) {
                    String currency = headers.get(0).getText().trim();
                    String rate = "";

                    // Find the first valid (non-holiday, non-empty) rate value
                    for (WebElement cell : cells) {
                        String text = cell.getText().trim();
                        if (!text.isEmpty()
                                && !text.equalsIgnoreCase("Bank holiday")
                                && !text.equals("-")) {
                            rate = text;
                            break; // stop at the first valid rate
                        }
                    }

                    // Write to CSV if both currency and rate are valid
                    if (!currency.isEmpty() && !rate.isEmpty()) {
                        csvWriter.write(currency + "," + rate + "\n");
                        System.out.println("  " + currency + " -> " + rate + " CAD");
                        count++;
                    }
                }
            }

            // Close the CSV writer to flush and save the file
            csvWriter.close();
            System.out.println("\n  Total currencies saved: " + count);
            System.out.println("  File saved: boc_rates_task3.csv");

            // Wait 3 seconds before closing for screenshot
            Thread.sleep(3000);

        } catch (IOException e) {
            // Handles file writing errors (e.g. permission denied, disk full)
            System.out.println("File error: " + e.getMessage());
        } catch (InterruptedException e) {
            // Handles unexpected interruption of Thread.sleep()
            System.out.println("Sleep interrupted: " + e.getMessage());
        } finally {
            // Always close Chrome and end the WebDriver session
            // finally block runs whether or not an exception occurred
            driver.quit();
            System.out.println("\nBrowser closed. Advanced scrape complete!");
        }
    }
}