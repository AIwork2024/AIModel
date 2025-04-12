import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

public class chromejava {
    public static void main(String[] args) {
        // Set the path to chromedriver executable
        System.setProperty("webdriver.chrome.driver", "C:\\Users\\Kishore\\Downloads\\chromedriver\\chromedriver.exe");

        // Initialize WebDriver
        WebDriver driver = new ChromeDriver();

        try {
            // Open Google.com
            driver.get("https://www.google.com");

            // Maximize browser window
            driver.manage().window().maximize();

            // Find search box element
            WebElement searchBox = driver.findElement(By.name("q"));

            // Enter search query
            searchBox.sendKeys("Selenium WebDriver");

            // Submit the search
            searchBox.submit();

            // Wait for results to load (optional, but good practice)
            Thread.sleep(3000);

            // Print the title of the page
            System.out.println("Page title is: " + driver.getTitle());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Close browser
            driver.quit();
        }
    }
}