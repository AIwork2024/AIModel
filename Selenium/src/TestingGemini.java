import okhttp3.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class TestingGemini {

    private static final String GEMINI_API_KEY = "AIzaSyCv6Ap6Jfg55ucjtZ2TATmVFFA7VL8LOjo"; // Replace with actual API Key

    static OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver", "C:\\Users\\Kishore\\Downloads\\chromedriver\\chromedriver.exe");

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        WebDriver driver = new ChromeDriver(options);
        driver.get("https://www.saucedemo.com/");

        String elementDescription = "Trying to locate the 'login' button on the homepage.";
        By originalLocator = By.xpath("//*[@name='login-button7']");

        WebElement element = null;

        try {
            element = driver.findElement(originalLocator);
            element.click();
            System.out.println("✅ Sign In button clicked using original locator.");
        } catch (NoSuchElementException e) {
            System.out.println("❌ Original locator failed: " + originalLocator);
            System.out.println("🔄 Attempting fallback using Gemini...");

            LocatorResult fallbackResult = findElementWithFallback(driver, originalLocator, elementDescription);

            if (fallbackResult != null && fallbackResult.element != null) {
                fallbackResult.element.click();
                System.out.println("✅ Sign In button clicked using fallback locator.");
                System.out.println("✅ Final working XPath used: " + fallbackResult.usedXPath);

                // Update the originalLocator and print the final updated value
                originalLocator = By.xpath(fallbackResult.usedXPath);
                System.out.println("📌 Updated originalLocator value: " + originalLocator);
            } else {
                System.out.println("❌ Failed to find the element even after fallback.");
            }
        }

        // driver.quit();
    }

    static class LocatorResult {
        WebElement element;
        String usedXPath;

        public LocatorResult(WebElement element, String usedXPath) {
            this.element = element;
            this.usedXPath = usedXPath;
        }
    }

    public static LocatorResult findElementWithFallback(WebDriver driver, By by, String description) {
        while (true) {
            System.out.println("🔄 Fetching alternative locators from Gemini...");

            String alternativeLocators = getFallbackSelector(driver, description);
            if (alternativeLocators == null || alternativeLocators.trim().isEmpty()) {
                System.out.println("⚠️ Gemini did not provide valid alternative locators.");
                return null;
            }

            String[] locatorList = alternativeLocators.split("\n");
            for (String locator : locatorList) {
                locator = locator.trim();
                if (locator.isEmpty()) continue;

                if (!locator.startsWith("//") && !locator.startsWith(".") && !locator.startsWith("//*")) {
                    System.out.println("⚠️ Ignoring invalid XPath locator: " + locator);
                    continue;
                }

                System.out.println("🔍 Trying alternative locator: " + locator);
                try {
                    WebElement element = driver.findElement(By.xpath(locator));
                    System.out.println("✅ Successfully found element using: " + locator);
                    return new LocatorResult(element, locator);
                } catch (NoSuchElementException ignored) {
                    System.out.println("❌ Element not found using: " + locator);
                } catch (InvalidSelectorException e) {
                    System.out.println("⚠️ Skipping invalid XPath syntax: " + locator);
                }
            }

            System.out.println("🔁 No valid locator found in this set. Requesting new set...");
        }
    }

    public static String getFallbackSelector(WebDriver driver, String description) {
        String url = "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-pro-002:generateContent?key=" + GEMINI_API_KEY;

        String jsonBody = "{" +
                "  \"contents\": [{" +
                "    \"parts\": [{" +
                "      \"text\": \"I need multiple valid XPath, ID, name, and CSS selectors for the login button on this public webpage. " +
                "Here is the webpage URL: https://www.saucedemo.com/. " +
                "Based on your knowledge of the DOM structure or common layouts, provide different tag names (e.g., button, input, div), " +
                "and attribute-based selectors (e.g., @name, @id, contains(@class, ...)). " +
                "IMPORTANT: Return ONLY a list of different valid selectors, separated by new lines, no explanation.\" " +
                "    }]" +
                "  }]" +
                "}";


        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                System.out.println("❌ Non-OK response from Gemini API: " + response.code());
                return null;
            }

            String rawResponse = response.body().string();
            System.out.println("📩 Raw Gemini API Response: " + rawResponse);

            JsonObject jsonResponse = JsonParser.parseString(rawResponse).getAsJsonObject();
            if (!jsonResponse.has("candidates")) {
                System.out.println("⚠️ No 'candidates' field in response.");
                return null;
            }

            JsonArray candidates = jsonResponse.getAsJsonArray("candidates");
            if (candidates.size() > 0) {
                JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
                if (firstCandidate.has("content")) {
                    JsonObject content = firstCandidate.getAsJsonObject("content");
                    JsonArray parts = content.getAsJsonArray("parts");
                    if (parts.size() > 0) {
                        String extractedText = parts.get(0).getAsJsonObject().get("text").getAsString();
                        System.out.println("🧠 Extracted Text from Gemini: \n" + extractedText);
                        return extractedText;
                    }
                }
            }
            System.out.println("⚠️ No valid XPath found in response.");
            return null;
        } catch (IOException e) {
            System.out.println("❌ API request failed: " + e.getMessage());
            return null;
        }
    }
}
