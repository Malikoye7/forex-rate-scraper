# Forex Rate Scraper

A Java + Selenium web scraper that automatically extracts daily foreign-exchange rates from the [Bank of Canada](https://www.bankofcanada.ca/rates/exchange/daily-exchange-rates/) — Canada's official central bank — and saves them to CSV for analysis.

Built in three progressively more advanced stages, from a basic single-page scrape to a resilient scraper using explicit waits, popup handling, JavaScript execution, and automated screenshots.

## What it does

The Bank of Canada publishes daily rates for 24 world currencies against the Canadian dollar. This project drives a real Chrome browser with Selenium WebDriver to read that data straight from the live page and export it.

| Program | What it demonstrates |
|---|---|
| `DailyRatesScraper` | Basic scraping — opens the page, reads the rates table (`<th>` currency names, `<td>` values), skips "Bank holiday" cells, writes `boc_rates.csv` |
| `MultiPageRatesScraper` | Multi-page crawling — scrapes two date-range pages, merges results with a `LinkedHashMap`, auto-calculates date ranges with `LocalDate` |
| `AdvancedRatesScraper` | Production-grade techniques — explicit waits, cookie/popup handling, `JavascriptExecutor`, automated screenshots, `ChromeOptions` |

## Techniques used

- **Selenium WebDriver** driving Chrome (Selenium Manager auto-resolves the driver — no manual setup)
- **Explicit waits** (`WebDriverWait` + `ExpectedConditions`) instead of brittle `Thread.sleep`
- **Robust parsing** — handles bank-holiday cells, multi-column date ranges, and header/data rows
- **JavaScript execution** for scrolling and DOM inspection
- **Automated screenshots** via `TakesScreenshot`
- **CSV export** with `FileWriter`

## Tech Stack

- Java 11
- Selenium Java 4.21
- Maven
- Google Chrome

## Getting started

**Prerequisites:** JDK 11+, Maven, and Google Chrome installed.

```bash
# Compile
mvn compile

# Run any of the three scrapers
mvn exec:java -Dexec.mainClass="com.uwindsor.forexscraper.DailyRatesScraper"
mvn exec:java -Dexec.mainClass="com.uwindsor.forexscraper.MultiPageRatesScraper"
mvn exec:java -Dexec.mainClass="com.uwindsor.forexscraper.AdvancedRatesScraper"
```

Each run opens Chrome, scrapes the live page, and writes a CSV to the project directory.

## Sample output

```csv
Currency,2026-07-06,2026-07-07,2026-07-08
US dollar,1.3642,1.3651,1.3628
Euro,1.4810,1.4795,1.4832
British pound,1.7501,1.7488,1.7520
```

## Project Structure

```
src/main/java/com/uwindsor/forexscraper/
  DailyRatesScraper.java       — single-page scrape to CSV
  MultiPageRatesScraper.java   — multi-page crawl + merge
  AdvancedRatesScraper.java    — waits, popups, JS, screenshots
pom.xml
```

## License

MIT — see [LICENSE](LICENSE).
