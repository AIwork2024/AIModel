import org.apache.commons.text.similarity.FuzzyScore;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Locale;

public class AISelfHealingDriver {
    private WebDriver driver;
    private FuzzyScore fuzzyScore;

    // You can adjust these constants based on your application.
    private static final int SCORE_THRESHOLD = 2; // For debugging, lower threshold
    private static final int MAX_LOGGED_CANDIDATES = 50; // Log only a subset to not overwhelm the console

    public AISelfHealingDriver(WebDriver driver) {
        this.driver = driver;
        this.fuzzyScore = new FuzzyScore(Locale.ENGLISH);
    }

    public WebElement findElementAI(By by, String expectedText) {
        WebElement element;
        try {
            element = driver.findElement(by);
        } catch (NoSuchElementException e) {
            System.out.println("Standard locator failed. Starting AI-based recovery...");
            element = aiRecoverElement(expectedText);
        }

        if (element != null) {
            // Scroll the element into view
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
            // Wait until the element is clickable
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            try {
                element = wait.until(ExpectedConditions.elementToBeClickable(element));
            } catch (TimeoutException te) {
                System.out.println("Timeout: element not clickable; trying JavaScript focus.");
                ((JavascriptExecutor) driver).executeScript("arguments[0].focus();", element);
            }
        } else {
            System.out.println("AI Recovery returned null. Check the expected text and page content.");
        }
        return element;
    }

    private WebElement aiRecoverElement(String expectedText) {
        try {
            // Wait until the body element is present (dynamic content)
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

            // Parse the current page source
            String pageSource = driver.getPageSource();
            Document doc = Jsoup.parse(pageSource);
            Elements allElements = doc.getAllElements();

            String bestMatchText = null;
            String bestMatchXPath = null;
            int bestScore = 0;
            String normalizedExpected = expectedText.toLowerCase().trim();

            int loggedCandidates = 0;

            for (Element element : allElements) {
                String tagName = element.tagName().toLowerCase();
                // Skip common container tags
                if (tagName.equals("html") || tagName.equals("body") ||
                        tagName.equals("head") || tagName.equals("script") ||
                        tagName.equals("style")) {
                    continue;
                }
                String text = element.text().trim();
                if (text.isEmpty()) {
                    continue;
                }
                // Optionally, skip elements with very long text (could be containers)
                if (text.length() > 150) {
                    continue;
                }
                String aria = element.attr("aria-label");
                String attributes = element.id() + " " + element.className() + " " + (aria != null ? aria : "");
                String normalizedText = text.toLowerCase();
                String normalizedAttributes = attributes.toLowerCase();

                int scoreText = fuzzyScore.fuzzyScore(normalizedExpected, normalizedText);
                int scoreAttr = fuzzyScore.fuzzyScore(normalizedExpected, normalizedAttributes);
                int finalScore = Math.max(scoreText, scoreAttr);

                if (loggedCandidates < MAX_LOGGED_CANDIDATES) {
                    System.out.println("Candidate - Tag: " + tagName
                            + " | Text: \"" + text + "\" | Score: " + finalScore);
                    loggedCandidates++;
                }

                if (finalScore > bestScore) {
                    bestScore = finalScore;
                    bestMatchText = text;
                    bestMatchXPath = generateXPath(element);
                }
            }
            System.out.println("Best match: \"" + bestMatchText + "\" with score " + bestScore);

            if (bestMatchXPath != null && bestScore > SCORE_THRESHOLD) {
                System.out.println("Recovered element via fuzzy matching.");
                WebElement recovered = driver.findElement(By.xpath(bestMatchXPath));
                if (!recovered.isDisplayed()) {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", recovered);
                }
                return recovered;
            }
            System.out.println("Fuzzy matching did not find a strong candidate. Trying hierarchy-based recovery...");
            return recoverFromHierarchy(expectedText);
        } catch (Exception e) {
            System.out.println("AI Recovery failed: " + e.getMessage());
        }
        return null;
    }

    private WebElement recoverFromHierarchy(String expectedText) {
        try {
            WebElement body = driver.findElement(By.tagName("body"));
            return searchInElementTree(body, expectedText);
        } catch (Exception e) {
            System.out.println("Hierarchy recovery failed: " + e.getMessage());
        }
        return null;
    }

    private WebElement searchInElementTree(WebElement parent, String expectedText) {
        String normalizedExpected = expectedText.toLowerCase().trim();
        for (WebElement child : parent.findElements(By.xpath(".//*"))) {
            String childText = child.getText();
            String ariaLabel = child.getAttribute("aria-label");
            String placeholder = child.getAttribute("placeholder");
            String value = child.getAttribute("value");

            String normalizedChildText = childText != null ? childText.toLowerCase() : "";
            String normalizedAria = ariaLabel != null ? ariaLabel.toLowerCase() : "";
            String normalizedPlaceholder = placeholder != null ? placeholder.toLowerCase() : "";
            String normalizedValue = value != null ? value.toLowerCase() : "";

            System.out.println("Checking child: \"" + childText + "\"");
            if (normalizedChildText.contains(normalizedExpected) ||
                    normalizedAria.contains(normalizedExpected) ||
                    normalizedPlaceholder.contains(normalizedExpected) ||
                    normalizedValue.contains(normalizedExpected)) {
                System.out.println("Found matching child in hierarchy: \"" + childText + "\"");
                return child;
            }
        }
        return null;
    }

    private String escapeXPathString(String input) {
        if (input.contains("'")) {
            return "concat('" + input.replace("'", "',\"'\",'") + "')";
        }
        return "'" + input + "'";
    }

    private String generateXPath(Element element) {
        String textContent = element.text().trim();
        String escapedText = escapeXPathString(textContent);
        return "//*[contains(text(), " + escapedText + ") or contains(@id, '"
                + element.id() + "') or contains(@class, '" + element.className() + "')]";
    }

    public void get(String url) {
        driver.get(url);
    }

    public void quit() {
        driver.quit();
    }
}