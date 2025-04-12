import okhttp3.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TestingGeminiWithDomContextpromt {

    private static final String GEMINI_API_KEY = "AIzaSyCY-q0TEdcdoGyP3BtHA1y3jDEtnSZascE"; // Replace with your real key
    private static final String SOURCE_FILE_PATH = "D:\\MyAICode\\AIModel\\Selenium\\src\\TestingGeminiWithDomContextpromt.java";

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

        String elementDescription = "Trying to locate the 'Username' text field on the homepage.";

        // [AUTO-UPDATE-XPATH] -- DO NOT REMOVE THIS COMMENT
By originalLocator = By.xpath("//*[@data-test='username']"); // [AUTO-UPDATE-XPATH]
        WebElement element = null;
        try {
            element = driver.findElement(originalLocator);
            element.sendKeys("kishore");
            System.out.println("✅ Element found using original locator.");
        } catch (NoSuchElementException e) {
            System.out.println("❌ Original locator failed. Trying fallback via Gemini...");

            LocatorResult fallbackResult = findElementWithFallback(driver, elementDescription);

            if (fallbackResult != null && fallbackResult.element != null) {
                fallbackResult.element.sendKeys("kishore");
                System.out.println("✅ Element found with Gemini XPath.");
                System.out.println("✅ Final working XPath: " + fallbackResult.usedXPath);

                updateSourceFileWithWorkingXPath(SOURCE_FILE_PATH, fallbackResult.usedXPath);
            } else {
                System.out.println("❌ Could not find the element even after fallback.");
            }
        }

        // driver.quit(); // Uncomment to close browser
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
            System.out.println("🔄 Extracting live DOM...");
            String domContext = driver.findElement(By.tagName("body")).getAttribute("outerHTML");

            System.out.println("🔄 Calling Gemini for fallback XPath...");
            String alternativeLocators = getFallbackSelectorFromDom(domContext, description);

            if (alternativeLocators == null || alternativeLocators.trim().isEmpty()) {
                System.out.println("⚠️ Gemini returned no suggestions.");
                return null;
            }

            String[] locatorList = alternativeLocators.split("\n");
            for (String locator : locatorList) {
                locator = locator.trim();
                if (locator.isEmpty()) continue;

                try {
                    By fallbackBy = locator.startsWith("//") ? By.xpath(locator) :
                            locator.startsWith("#") || locator.startsWith(".") ? By.cssSelector(locator) :
                                    By.id(locator);

                    WebElement element = driver.findElement(fallbackBy);
                    System.out.println("✅ Found element using: " + locator);
                    return new LocatorResult(element, locator);
                } catch (NoSuchElementException | InvalidSelectorException ignored) {
                    System.out.println("❌ Not found using: " + locator);
                }
            }

            System.out.println("🔁 Trying next Gemini suggestion batch...");
        }
    }

    public static String getFallbackSelectorFromDom(String domHtml, String description) {
        String prompt = "You're a web automation expert. Below is the HTML of the page:\n\n" + domHtml +
                "\n\nI need valid XPath/CSS/ID selectors to locate: " + description +
                "\nReturn one per line, no explanation.";

        String url = "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-pro-002:generateContent?key=" + GEMINI_API_KEY;
        String jsonBody = "{ \"contents\": [{ \"parts\": [{ \"text\": " + quoteJson(prompt) + " }] }] }";

        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json"));
        Request request = new Request.Builder().url(url).post(body).header("Content-Type", "application/json").build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                System.out.println("❌ Gemini API failed: " + response.code());
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
            System.out.println("❌ Gemini API error: " + e.getMessage());
            return null;
        }
    }

    public static String quoteJson(String text) {
        return "\"" + text.replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    public static void updateSourceFileWithWorkingXPath(String filePath, String newXPath) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                System.out.println("❌ File does not exist: " + filePath);
                return;
            }

            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            String marker = "// [AUTO-UPDATE-XPATH]";
            boolean replaced = false;

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.contains("By originalLocator") && line.contains(marker)) {
                    String updatedLine = "By originalLocator = By.xpath(\"" + newXPath + "\"); " + marker;
                    lines.set(i, updatedLine);
                    replaced = true;
                    break;
                }
            }

            if (!replaced) {
                System.out.println("⚠️ Could not find the marker line to update.");
                return;
            }

            Files.write(path, lines, StandardCharsets.UTF_8);
            System.out.println("✅ XPath updated in source file.");

            String verify = Files.readString(path, StandardCharsets.UTF_8);
            System.out.println("🔍 File content after update:");
            System.out.println(verify);
            // Calling the shell script after updating the file
            callShellScript("D:\\MyAICode\\AIModel\\Selenium\\src\\auto_commit_and_pr.sh");

        } catch (IOException e) {
            System.out.println("❌ Failed to update source file: " + e.getMessage());
        }
    }

    public static void callShellScript(String scriptPath) {
        try {
            String bashPath = "C:\\Program Files\\Git\\bin\\bash.exe"; // Git Bash path

            ProcessBuilder processBuilder = new ProcessBuilder(bashPath, scriptPath);
            processBuilder.inheritIO();

            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("✅ Shell script executed successfully.");
            } else {
                System.out.println("❌ Shell script failed with exit code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("❌ Failed to call shell script: " + e.getMessage());
        }
    }

}
