package com.uwindsor.forexscraper;

// Selenium imports needed to control the Chrome browser
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

// Java imports for writing the CSV file and managing time
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * ============================================================
 * DailyRatesScraper — Basic Web Scraping with Selenium
 * ============================================================
 * This program uses Selenium WebDriver to automatically open
 * the Bank of Canada daily exchange rates website in Chrome,
 * interact with page elements such as links and tables,
 * extract all available currency exchange rates, and save
 * the results into a CSV file for further analysis.
 *
 * Website used: Bank of Canada - Daily Exchange Rates
 * URL: https://www.bankofcanada.ca/rates/exchange/daily-exchange-rates/
 *
 * Why this data source:
 * The Bank of Canada is Canada's official central bank and
 * publishes daily exchange rates for 24 world currencies
 * against the Canadian dollar — a clean, reliable, public
 * data source that is well suited to a web-scraping project.
 *
 * What I discovered about the page structure:
 * - Currency names are stored inside HTML <th> tags (table headers)
 * - Exchange rate values are stored inside HTML <td> tags (table data)
 * - The table shows the last 5 business days across multiple columns
 * - On weekends or holidays, cells show "Bank holiday" instead of a number
 * - I had to skip those "Bank holiday" cells to get real rate values
 *
 * @author Oyewole Malik
 * ============================================================
 */
public class DailyRatesScraper {

    // The Bank of Canada daily exchange rates URL
    // I stored it as a constant so it is easy to change if needed
    private static final String BOC_URL =
        "https://www.bankofcanada.ca/rates/exchange/daily-exchange-rates/";

    public static void main(String[] args) {

        // Create a new Chrome browser instance using Selenium WebDriver
        // Selenium Manager (version 4.21.0) automatically downloads the
        // correct ChromeDriver — no manual setup was needed on my machine
        WebDriver driver = new ChromeDriver();

        System.out.println("=== Daily Rates Scraper: Bank of Canada Exchange Rates ===\n");

        try {
            // --------------------------------------------------------
            // STEP 1: Open the Bank of Canada website in Chrome
            // --------------------------------------------------------
            // driver.get() tells Selenium to navigate to the given URL
            // driver.manage().window().maximize() makes Chrome full screen
            // so all elements on the page are visible and accessible
            System.out.println("Step 1: Opening Bank of Canada website...");
            driver.get(BOC_URL);
            driver.manage().window().maximize();

            // Print the page title and URL to confirm we are on the right page
            System.out.println("  Page title : " + driver.getTitle());
            System.out.println("  Current URL: " + driver.getCurrentUrl());

            // --------------------------------------------------------
            // STEP 2: Wait for the exchange rates table to fully load
            // --------------------------------------------------------
            // I used WebDriverWait instead of Thread.sleep() because
            // WebDriverWait is smarter — it stops waiting as soon as
            // the element appears, instead of always waiting the full time.
            // I set 20 seconds as the maximum wait time to be safe.
            System.out.println("\nStep 2: Waiting for rates table to load...");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

            // presenceOfElementLocated checks if the <table> tag exists in the HTML
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("table")));
            System.out.println("  Page loaded successfully.");

            // --------------------------------------------------------
            // STEP 3: Interact with various elements on the page
            // --------------------------------------------------------
            // I used findElements (plural) to get ALL anchor <a> tags on the page
            // Each <a> tag represents a clickable link on the website
            // This shows Selenium can find and interact with many element types
            List<WebElement> links = driver.findElements(By.tagName("a"));
            System.out.println("\nStep 3: Interacting with page elements:");
            System.out.println("  - Total links found: " + links.size());
            System.out.println("  - Sample links found on the page:");

            // Print the first 5 non-empty link texts as examples
            // I trim() each link text to remove extra whitespace
            int shown = 0;
            for (WebElement link : links) {
                String text = link.getText().trim();
                if (!text.isEmpty() && shown < 5) {
                    System.out.println("      * " + text);
                    shown++;
                }
            }

            // --------------------------------------------------------
            // STEP 4: Extract exchange rate data from the rates table
            // --------------------------------------------------------
            // After inspecting the Bank of Canada page, I discovered:
            // - The table header row uses <th> tags for column headings
            // - Each currency row has ONE <th> tag (the currency name)
            //   followed by MULTIPLE <td> tags (one rate per business day)
            // - When a date is a bank holiday, the <td> contains "Bank holiday"
            //   instead of a number, so I check for that and skip it
            System.out.println("\nStep 4: Extracting exchange rates from table...");
            WebElement ratesTable = driver.findElement(By.tagName("table"));
            List<WebElement> rows = ratesTable.findElements(By.tagName("tr"));
            System.out.println("  - Total rows found in table: " + rows.size());

            // --------------------------------------------------------
            // STEP 5: Save all extracted data to a CSV file
            // --------------------------------------------------------
            // CSV (Comma Separated Values) is a simple format that can be
            // opened in Excel or any spreadsheet application
            // I write the header row first, then one line per currency
            System.out.println("\nStep 5: Saving data to CSV...");
            FileWriter csvWriter = new FileWriter("boc_rates_task1.csv");
            csvWriter.write("Currency,Rate (CAD per unit)\n");

            int count = 0;
            System.out.println("\n  Currencies extracted:");
            System.out.println("  ----------------------");

            // Loop through every row in the table
            for (WebElement row : rows) {

                // Get <th> elements (currency name) and <td> elements (rate values)
                List<WebElement> headers = row.findElements(By.tagName("th"));
                List<WebElement> cells   = row.findElements(By.tagName("td"));

                // I only process rows that have exactly 1 <th> (currency name)
                // and at least 1 <td> (rate value). This skips the header row
                // which has multiple <th> tags for the date column headings.
                if (headers.size() == 1 && !cells.isEmpty()) {

                    // Get the currency name from the <th> element
                    String currency = headers.get(0).getText().trim();

                    // Loop through all <td> cells to find the first real rate
                    // I skip any cell that says "Bank holiday", "-", or is empty
                    // because those mean no rate was published for that date
                    String rate = "";
                    for (WebElement cell : cells) {
                        String text = cell.getText().trim();
                        if (!text.isEmpty()
                                && !text.equalsIgnoreCase("Bank holiday")
                                && !text.equals("-")) {
                            rate = text;
                            break; // stop as soon as we find a valid rate
                        }
                    }

                    // Only write to CSV if both currency name and rate are valid
                    if (!currency.isEmpty() && !rate.isEmpty()) {
                        csvWriter.write(currency + "," + rate + "\n");
                        System.out.println("  " + currency + " -> " + rate + " CAD");
                        count++;
                    }
                }
            }

            // Close the CSV file writer to save the file properly
            csvWriter.close();
            System.out.println("\n  Total currencies scraped: " + count);
            System.out.println("  File saved: boc_rates_task1.csv");

            // Wait 4 seconds before closing so I can take a screenshot
            // of the Chrome browser for reference
            Thread.sleep(4000);

        } catch (IOException e) {
            // This catches errors when writing to the CSV file
            // For example: if the file is open in another program
            System.out.println("CSV write error: " + e.getMessage());
        } catch (InterruptedException e) {
            // This catches errors if the Thread.sleep() is interrupted
            System.out.println("Sleep interrupted: " + e.getMessage());
        } finally {
            // Always close the browser at the end, even if an error occurred
            // driver.quit() closes Chrome and ends the WebDriver session
            driver.quit();
            System.out.println("\nBrowser closed. Daily scrape complete!");
        }
    }
}