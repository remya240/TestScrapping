package testBase;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import io.github.bonigarcia.wdm.WebDriverManager;

public class BaseClass {

    protected static WebDriver driver;

    // Setup WebDriver with ChromeOptions
    public static void initializeDriver() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = getChromeOptions();
        driver = new ChromeDriver(options);
    }

    // Configure ChromeOptions
    protected  static ChromeOptions getChromeOptions() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless", "--disable-gpu", "--no-sandbox", "--disable-dev-shm-usage");
        options.addArguments("--disable-extensions", "--disable-images"); // Optional: Disable images for speed
        options.addArguments("--disable-blink-features=AutomationControlled"); // Avoid detection
        options.addArguments("--window-size=1920x1080"); // Ensure visibility
        return options;
    }

    // Quit the WebDriver instance
    public static void quitDriver() {
        if (driver != null) {
            driver.quit();
        }
    }
}
