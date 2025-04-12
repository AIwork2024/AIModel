import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;

public class SampleSelenium {

    public static void main(String[] args) throws Exception {
        System.out.println("Hello, World!");
        System.setProperty("webdriver.chrome.driver", "C:\\Users\\Kishore\\Downloads\\chromedriver\\chromedriver.exe");
        //System.setProperty(
                //"webdriver.edge.driver",
               // "C:\\Users\\Kishore\\Downloads\\edgedriver_win64 (1)\\msedgedriver.exe");



        //EdgeOptions options = new EdgeOptions();
       //options.setCapability("browserName", "MicrosoftEdge");
        // Instantiate a ChromeDriver class.
        WebDriver orginalwebdriver = new ChromeDriver();
       //WebDriver orginalwebdriver = new EdgeDriver();
        AISelfHealingDriver driver = new AISelfHealingDriver(orginalwebdriver);
        // Launch Website
        driver.get("https://www.saucedemo.com/");
        WebElement button = driver.findElementAI(By.xpath("//*[@name='user-name7']"), "user-name");
        Thread.sleep(2000);
        if (button != null) {
            button.sendKeys("your text");
        } else {
            System.out.println("AI Recovery failed: No matching element found.");
        }

        Thread.sleep(2000);
    }
}
