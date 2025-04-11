import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class gemi {

    // Your Gemini API key
    private static final String GEMINI_API_KEY = "AIzaSyBMRsS1waZAPtdTxjtfO_T9xn0bpXAirmA";

    public static void main(String[] args) {
        // Set up Selenium WebDriver
        System.setProperty("webdriver.chrome.driver", "C:\\Users\\Kishore\\Downloads\\chromedriver\\chromedriver.exe");
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");

        WebDriver driver = new ChromeDriver(options);
        driver.get("https://www.saucedemo.com/");  // Open the target website

        // Description of the element we are looking for
        String elementDescription = "Trying to locate the 'Sign In' button on the homepage.";

        // Try to find the element with fallback
        WebElement signInButton = findElementWithFallback(driver, By.xpath("//*[@name='login-button7']"), elementDescription);

        // Click the button if found
        if (signInButton != null) {
            signInButton.click();
            System.out.println("Sign In button clicked successfully.");
        } else {
            System.out.println("Failed to find the element even with Gemini’s help.");
        }

        // Close the browser
        driver.quit();
    }

    /**
     * Attempts to locate a WebElement using the provided locator.
     * If not found, it calls the Gemini API for an alternative locator.
     */
    public static WebElement findElementWithFallback(WebDriver driver, By by, String description) {
        try {
            return driver.findElement(by);
        } catch (NoSuchElementException e) {
            System.out.println("Element not found: " + by.toString() + ". Fetching alternative from Gemini...");
            String alternativeLocator = getFallbackSelector(description);
            System.out.println("Gemini Suggests: " + alternativeLocator);
            if (alternativeLocator == null || alternativeLocator.trim().isEmpty()) {
                System.out.println("No valid locator suggestion received from Gemini.");
                return null;
            }
            try {
                return driver.findElement(By.xpath(alternativeLocator));
            } catch (NoSuchElementException ex) {
                System.out.println("Alternative locator also failed.");
                return null;
            }
        }
    }

    /**
     * Calls the Gemini API to get alternative selectors for missing elements.
     * It logs the raw response and performs error checking.
     */
    public static String getFallbackSelector(String description) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            String url = "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-pro-002:generateContent?key=" + GEMINI_API_KEY;
            HttpPost request = new HttpPost(url);

            // Build JSON request body
            JsonObject prompt = new JsonObject();
            prompt.addProperty("text", "I'm trying to find an element on a webpage but it's missing. Here are the details: "
                    + description + ". Suggest alternative XPath, CSS selectors, or IDs.");

            JsonObject requestBody = new JsonObject();
            requestBody.add("prompt", prompt);

            StringEntity entity = new StringEntity(requestBody.toString(), StandardCharsets.UTF_8);
            request.setEntity(entity);
            request.addHeader("Content-Type", "application/json");

            CloseableHttpResponse response = httpClient.execute(request);
            try {
                int statusCode = response.getCode();
                if (statusCode != 200) {
                    System.out.println("Non-OK response from Gemini API: " + statusCode);
                    return null;
                }
                if (response.getEntity() == null) {
                    System.out.println("Response entity is null.");
                    return null;
                }
                // Read and log the raw response for debugging
                InputStreamReader reader = new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8);
                String rawResponse = new BufferedReader(reader)
                        .lines()
                        .collect(Collectors.joining("\n"));
                System.out.println("Raw Gemini API Response: " + rawResponse);

                JsonElement jsonElement = JsonParser.parseString(rawResponse);
                if (jsonElement == null || !jsonElement.isJsonObject()) {
                    System.out.println("Response is not a valid JSON object: " + jsonElement);
                    return null;
                }
                JsonObject jsonResponse = jsonElement.getAsJsonObject();
                if (!jsonResponse.has("text")) {
                    System.out.println("JSON response does not have 'text' property: " + jsonResponse);
                    return null;
                }
                String suggestion = jsonResponse.get("text").getAsString().split("\n")[0];
                return suggestion.trim().isEmpty() ? null : suggestion;
            } finally {
                response.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                httpClient.close();
            } catch (IOException ioEx) {
                ioEx.printStackTrace();
            }
        }
    }
}