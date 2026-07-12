package com.uwindsor.forexscraper;

// Selenium imports for browser automation
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

// Java imports for file writing, dates, and data structures
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * ============================================================
 * MultiPageRatesScraper — Multi-Page Web Scraping with Selenium
 * ============================================================
 * This program extends the daily scraper by crawling the Bank
 * of Canada exchange rates website across TWO different
 * date-range pages and combining all results into a single CSV.
 *
 * How multi-page scraping works in this program:
 * - Page 1 covers the most recent 2-week date range
 * - Page 2 covers the 2-week period before that
 * - Both pages are scraped and results are merged together
 * - The final CSV contains all currencies with all available dates
 *
 * Key design decisions I made:
 * 1. I used a LinkedHashMap to store data because it preserves
 *    the insertion order of currencies and dates
 * 2. I used LocalDate to automatically calculate date ranges
 *    so the program always uses current dates, not hardcoded ones
 * 3. I used putIfAbsent() to avoid overwriting existing currency
 *    data when merging results from both pages
 * 4. I extract ALL date columns from each page (not just one)
 *    so the combined CSV is as complete as possible
 *
 * What I learned from inspecting the Bank of Canada table:
 * - The first row of the table has <th> tags for each date column
 * - The first <th> in that row is "Currencies" (not a date)
 *   so I start reading date headers from index 1, not index 0
 * - Each currency data row has 1 <th> (name) + 5 <td> (rates)
 *
 * @author Oyewole Malik
 * ============================================================
 */
public class MultiPageRatesScraper {

    // Base URL for Bank of Canada daily exchange rates
    // I append date parameters to this URL to load different date ranges
    private static final String BOC_URL =
        "https://www.bankofcanada.ca/rates/exchange/daily-exchange-rates/";

    // This map stores ALL scraped data from ALL pages combined
    // Structure: currency name -> (date string -> rate string)
    // I use LinkedHashMap to keep currencies in the order they appear on the page
    private static final Map<String, Map<String, String>> allData = new LinkedHashMap<>();

    public static void main(String[] args) {

        // Open Chrome browser for scraping
        WebDriver driver = new ChromeDriver();
        System.out.println("=== Multi-Page Rates Scraper: Bank of Canada ===\n");

        try {
            // I use DateTimeFormatter to format dates as "yyyy-MM-dd"
            // which is the format the Bank of Canada URL expects
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            // Get today's date so date ranges are always current
            LocalDate today = LocalDate.now();

            // --- Calculate date ranges for Page 1 and Page 2 ---
            // Page 1: from 13 days ago up to today (covers most recent 2 weeks)
            LocalDate end1   = today;
            LocalDate start1 = today.minusDays(13);

            // Page 2: the 2 weeks before Page 1 (older historical data)
            LocalDate end2   = today.minusDays(14);
            LocalDate start2 = today.minusDays(27);

            // Build the full URLs by appending date parameters
            // The Bank of Canada uses dFrom and dTo as query parameters
            String url1 = BOC_URL + "?dFrom=" + start1.format(fmt) + "&dTo=" + end1.format(fmt);
            String url2 = BOC_URL + "?dFrom=" + start2.format(fmt) + "&dTo=" + end2.format(fmt);

            // --------------------------------------------------------
            // SCRAPE PAGE 1: Most recent 2 weeks of exchange rates
            // --------------------------------------------------------
            System.out.println("--- Page 1: " + start1.format(fmt) + " to " + end1.format(fmt) + " ---");
            System.out.println("URL: " + url1);
            scrapePage(driver, url1);

            // --------------------------------------------------------
            // SCRAPE PAGE 2: Previous 2 weeks of exchange rates
            // --------------------------------------------------------
            System.out.println("\n--- Page 2: " + start2.format(fmt) + " to " + end2.format(fmt) + " ---");
            System.out.println("URL: " + url2);
            scrapePage(driver, url2);

            // --------------------------------------------------------
            // SAVE: Combine both pages into one CSV file
            // --------------------------------------------------------
            saveCSV();

            // Wait 3 seconds before closing for screenshot purposes
            Thread.sleep(3000);

        } catch (InterruptedException e) {
            // Handle case where Thread.sleep is interrupted unexpectedly
            System.out.println("Sleep interrupted: " + e.getMessage());
        } finally {
            // Always close the browser and end the WebDriver session
            driver.quit();
            System.out.println("Browser closed. Multi-page scrape complete!");
        }
    }

    /**
     * Navigates to a given URL, waits for the exchange rates table to load,
     * reads ALL date column headers and ALL currency rows, and stores every
     * valid rate value into the shared allData map for later CSV export.
     *
     * I made this a separate method (not inside main) so it can be called
     * multiple times cleanly — once per page — without repeating code.
     *
     * @param driver the active Selenium WebDriver (Chrome)
     * @param url    the full URL to navigate to and scrape
     */
    private static void scrapePage(WebDriver driver, String url) {

        // Navigate Chrome to the given URL and maximise the window
        driver.get(url);
        driver.manage().window().maximize();

        // Wait up to 20 seconds for the exchange rates table to appear
        // This is important because the page may take time to load fully
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("table")));

        // Find the main rates table on the page
        WebElement table = driver.findElement(By.tagName("table"));

        // --------------------------------------------------------
        // READ DATE COLUMN HEADERS from the first row of the table
        // --------------------------------------------------------
        // The first <tr> row contains <th> tags listing the dates
        // I start at index 1 because index 0 is "Currencies" (not a date)
        List<String> dates = new ArrayList<>();
        List<WebElement> allRows = table.findElements(By.tagName("tr"));

        if (!allRows.isEmpty()) {
            List<WebElement> headerThs = allRows.get(0).findElements(By.tagName("th"));
            for (int i = 1; i < headerThs.size(); i++) {
                String dateText = headerThs.get(i).getText().trim();
                if (!dateText.isEmpty()) {
                    dates.add(dateText); // add each date column header to our list
                }
            }
        }
        System.out.println("  Dates found on this page: " + dates);

        // --------------------------------------------------------
        // READ EACH CURRENCY ROW and store the rates
        // --------------------------------------------------------
        int count = 0;
        for (WebElement row : allRows) {

            // Each data row has 1 <th> for the currency name
            // and multiple <td> tags — one for each date column
            List<WebElement> ths = row.findElements(By.tagName("th"));
            List<WebElement> tds = row.findElements(By.tagName("td"));

            // Only process rows where there is exactly 1 <th> (currency name)
            // Rows with more <th> tags are header rows — we skip those
            if (ths.size() == 1 && !tds.isEmpty()) {

                String currency = ths.get(0).getText().trim();
                if (currency.isEmpty()) continue; // skip blank rows

                // putIfAbsent creates a new inner map for this currency
                // only if it does not already exist — prevents overwriting
                allData.putIfAbsent(currency, new LinkedHashMap<>());

                // Loop through each date column and store the rate value
                // Math.min ensures we never go out of bounds if sizes differ
                for (int i = 0; i < Math.min(dates.size(), tds.size()); i++) {
                    String rate = tds.get(i).getText().trim();

                    // Only store genuine numeric rates — skip holidays and blanks
                    if (!rate.isEmpty()
                            && !rate.equalsIgnoreCase("Bank holiday")
                            && !rate.equals("-")) {
                        allData.get(currency).put(dates.get(i), rate);
                    }
                }
                count++;
            }
        }
        System.out.println("  Currencies scraped from this page: " + count);
    }

    /**
     * Collects all unique dates from both pages and writes the combined
     * data to boc_rates_task2.csv. The CSV has currencies as rows and
     * dates as columns. If a rate is missing for a date, "N/A" is written.
     *
     * I used try-with-resources (try (FileWriter writer = ...)) so the
     * file is automatically closed even if an exception occurs.
     */
    private static void saveCSV() {

        // Gather all unique date strings from all currencies across all pages
        // I check !allDates.contains(date) to avoid adding duplicate dates
        List<String> allDates = new ArrayList<>();
        for (Map<String, String> rates : allData.values()) {
            for (String date : rates.keySet()) {
                if (!allDates.contains(date)) allDates.add(date);
            }
        }

        System.out.println("\nTotal unique dates collected: " + allDates.size());
        System.out.println("Total currencies: " + allData.size());

        // Write the combined data to a CSV file
        // try-with-resources automatically closes the FileWriter when done
        try (FileWriter writer = new FileWriter("boc_rates_task2.csv")) {

            // Write the header row: "Currency, date1, date2, date3..."
            writer.write("Currency");
            for (String date : allDates) writer.write("," + date);
            writer.write("\n");

            // Write one data row per currency
            // getOrDefault returns "N/A" if no rate exists for a given date
            for (Map.Entry<String, Map<String, String>> entry : allData.entrySet()) {
                writer.write(entry.getKey()); // write currency name first
                for (String date : allDates) {
                    writer.write("," + entry.getValue().getOrDefault(date, "N/A"));
                }
                writer.write("\n");
                System.out.println("  " + entry.getKey() + " - "
                        + entry.getValue().size() + " dates recorded");
            }
            System.out.println("\nCombined file saved: boc_rates_task2.csv");

        } catch (IOException e) {
            // Print error message if file writing fails
            System.out.println("CSV error: " + e.getMessage());
        }
    }
}