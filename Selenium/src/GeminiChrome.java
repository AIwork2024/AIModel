import okhttp3.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class GeminiChrome {

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
        WebElement signInButton = findElementWithFallback(driver, originalLocator, elementDescription);
        System.out.println("print the sign-in button "+signInButton);

        if (signInButton != null) {
            signInButton.click();
            System.out.println("✅ Sign In button clicked successfully.");
        } else {
            System.out.println("Failed to find the element even after multiple retries.");
        }

        //driver.quit();
    }

    public static WebElement findElementWithFallback(WebDriver driver, By by, String description) {
        while (true) { // Loop until a valid element is found
            System.out.println("Element not found: " + by.toString() + ". Fetching alternative locators from Gemini...");

            String alternativeLocators = getFallbackSelector(driver, description);
            if (alternativeLocators == null || alternativeLocators.trim().isEmpty()) {
                System.out.println("Gemini did not provide valid alternative locators.");
                return null;
            }

            String[] locatorList = alternativeLocators.split("\n"); // Split multiple locators
            for (String locator : locatorList) {
                locator = locator.trim();
                if (locator.isEmpty()) continue;

                // Ensure the locator follows XPath syntax
                if (!locator.startsWith("//") && !locator.startsWith(".")) {
                    System.out.println("Ignoring invalid XPath locator: " + locator);
                    continue;
                }

                System.out.println("🔍 Trying alternative locator: " + locator);

                try {
                    WebElement element = driver.findElement(By.xpath(locator));
                    System.out.println("✅ Successfully found element using: " + locator);
                    return element; // Return the first valid element found
                } catch (NoSuchElementException ignored) {
                    System.out.println("Element not found using: " + locator);
                } catch (InvalidSelectorException e) {
                    System.out.println("Skipping invalid XPath syntax: " + locator);
                }
            }

            System.out.println("No valid locator found in this set. Requesting new set...");
        }
    }

    public static String getFallbackSelector(WebDriver driver, String description) {
        String url = "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-pro-002:generateContent?key=" + GEMINI_API_KEY;

        String jsonBody = "{" +

                "  \"contents\": [{" +
                "    \"parts\": [{" +
                "      \"text\": \"I need multiple valid XPath, ID, name, and CSS selectors for the following missing element on a webpage. " +
                "Here are the details: " + description +
                ". Ensure that the response includes different tag names (e.g., button, input, div, span) when applicable. " +
                "Also include attribute-based selectors (e.g., @name, @id, contains(@class, ...)). " +
                "IMPORTANT: Ensure that the response maintains the original case of the label name (e.g., 'Login' should not be returned as 'login'). " +
                "Return ONLY a list of different possible XPath, ID, name, and CSS selectors, separated by new lines, without extra text or explanation.\"" +
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
                System.out.println(" Non-OK response from Gemini API: " + response.code());
                return null;
            }

            String rawResponse = response.body().string();
            System.out.println("Raw Gemini API Response: " + rawResponse);

            JsonObject jsonResponse = JsonParser.parseString(rawResponse).getAsJsonObject();
            if (!jsonResponse.has("candidates")) {
                System.out.println("No 'candidates' field in response.");
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
                        System.out.println("Extracted Text from Gemini: " + extractedText);
                        return extractedText;
                    }
                }
            }
            System.out.println("No valid XPath found in response.");
            return null;
        } catch (IOException e) {
            System.out.println("API request failed: " + e.getMessage());
            return null;
        }
    }
}
