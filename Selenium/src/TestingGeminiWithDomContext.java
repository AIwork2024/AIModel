import okhttp3.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class TestingGeminiWithDomContext {

    private static final String GEMINI_API_KEY = "AIzaSyCv6Ap6Jfg55ucjtZ2TATmVFFA7VL8LOjo"; // Replace with your actual key

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

        String elementDescription = "Trying to locate the 'Username' textfiled on the homepage.";
By originalLocator = By.xpath("//input[@data-test='username']"); // [AUTO-UPDATE-XPATH]

        WebElement element = null;
        try {
            element = driver.findElement(originalLocator);
            element.sendKeys("kishore");
            System.out.println("✅ Sign In button clicked using original locator.");
        } catch (NoSuchElementException e) {
            System.out.println("❌ Original locator failed. Attempting fallback using Gemini...");

            LocatorResult fallbackResult = findElementWithFallback(driver, elementDescription);

            if (fallbackResult != null && fallbackResult.element != null) {
                fallbackResult.element.sendKeys("kishore");
                System.out.println("✅ Sign In button clicked using fallback locator.");
                System.out.println("✅ Final working XPath used: " + fallbackResult.usedXPath);
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

    public static LocatorResult findElementWithFallback(WebDriver driver, String description) {
        while (true) {
            System.out.println("🔄 Extracting live DOM context...");
            String domContext = driver.findElement(By.tagName("body")).getAttribute("outerHTML");
            System.out.println("printing dome values"+domContext);

            System.out.println("🔄 Requesting fallback locators from Gemini...");
            String alternativeLocators = getFallbackSelectorFromDom(driver, domContext, description);

            if (alternativeLocators == null || alternativeLocators.trim().isEmpty()) {
                System.out.println("⚠️ Gemini did not provide valid alternative locators.");
                return null;
            }

            String[] locatorList = alternativeLocators.split("\n");
            for (String locator : locatorList) {
                locator = locator.trim();
                if (locator.isEmpty()) continue;

                try {
                    By fallbackBy;
                    if (locator.startsWith("//") || locator.startsWith("(//")) {
                        fallbackBy = By.xpath(locator);
                    } else if (locator.startsWith("#") || locator.startsWith(".")) {
                        fallbackBy = By.cssSelector(locator);
                    } else {
                        fallbackBy = By.id(locator); // try ID fallback
                    }

                    WebElement element = driver.findElement(fallbackBy);
                    System.out.println("✅ Successfully found element using: " + locator);
                    return new LocatorResult(element, locator);
                } catch (NoSuchElementException | InvalidSelectorException ignored) {
                    System.out.println("❌ Not found using: " + locator);
                }
            }

            System.out.println("🔁 No valid locator found in this set. Requesting new suggestions...");
        }
    }

    public static String getFallbackSelectorFromDom(WebDriver driver, String domHtml, String description) {
        String prompt = "You're a web automation expert. I need possible valid selectors to locate an element. " +
                "Below is the HTML content of the page currently loaded:\n\n" + domHtml + "\n\n" +
                "I'm trying to locate: " + description + "\n" +
                "Please return ONLY valid multiple XPath, ID, name, or CSS selectors, one per line, no explanation.";

        String url = "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-pro-002:generateContent?key=" + GEMINI_API_KEY;
        String jsonBody = "{ \"contents\": [{ \"parts\": [{ \"text\": " + quoteJson(prompt) + " }] }] }";

        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json"));
        Request request = new Request.Builder().url(url).post(body).header("Content-Type", "application/json").build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                System.out.println("❌ Gemini API response failed: " + response.code());
                return null;
            }

            String rawResponse = response.body().string();
            JsonObject jsonResponse = JsonParser.parseString(rawResponse).getAsJsonObject();

            JsonArray candidates = jsonResponse.getAsJsonArray("candidates");
            if (candidates.size() > 0) {
                JsonObject content = candidates.get(0).getAsJsonObject().getAsJsonObject("content");
                JsonArray parts = content.getAsJsonArray("parts");
                if (parts.size() > 0) {
                    String extractedText = parts.get(0).getAsJsonObject().get("text").getAsString();
                    System.out.println("🧠 Gemini Response:\n" + extractedText);
                    return extractedText;
                }
            }

            return null;
        } catch (IOException e) {
            System.out.println("❌ API call failed: " + e.getMessage());
            return null;
        }
    }

    private static String quoteJson(String text) {
        return "\"" + text.replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }
}
