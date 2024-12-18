package testBase;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.github.bonigarcia.wdm.WebDriverManager;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

public class Comorbidity extends BaseClass {

    public static void main(String[] args) throws InterruptedException {
        long startTime = System.nanoTime(); // Start time

        List<Recipe> recipes = Collections.synchronizedList(new ArrayList<>()); // Thread-safe list

        List<String> pageBeginsWithList = Arrays.asList("0-9", "A", "B");

        try {
            initializeDriver(); // Initialize WebDriver
            ExecutorService executor = Executors.newFixedThreadPool(10); // Thread pool for parallel tasks

            // Loop through the list of letters/numbers
            for (int k = 0; k < pageBeginsWithList.size(); k++) {
                driver.navigate().to("https://www.tarladalal.com/RecipeAtoZ.aspx?beginswith=" + pageBeginsWithList.get(k));

                int lastPage = 0;
                try {
                    // Find the last page number in pagination
                    String lastPageText = driver.findElement(By.xpath("(//a[@class='respglink'])[last()]")).getText();
                    lastPage = Integer.parseInt(lastPageText);
                } catch (Exception e) {
                    // Handle exception if pagination doesn't exist (single page)
                }

                if (lastPage != 0) {
                    // Loop through all pages for each letter/number
                    for (int j = 1; j <= 2; j++) {
                        driver.navigate().to("https://www.tarladalal.com/RecipeAtoZ.aspx?beginswith=" + pageBeginsWithList.get(k) + "&pageindex=" + j);

                        // Wait for recipe links to load on the page
                        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
                        List<WebElement> recipeLinks = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                                By.xpath("//span[@class='rcc_recipename']/a")));

                        // Collect all recipe URLs from the current page
                        List<String> recipeUrls = new ArrayList<>();
                        for (WebElement link : recipeLinks) {
                            recipeUrls.add(link.getAttribute("href"));
                        }

                        // Parallel Execution with ExecutorService
                        for (String url : recipeUrls) {
                            executor.submit(() -> {
                                WebDriver threadDriver = initializeThreadDriver();
                                try {
                                    Recipe recipe = scrapeRecipe(url, threadDriver);
                                    if (recipe != null) {
                                        synchronized (recipes) { // Add recipe details to the thread-safe list
                                            recipes.add(recipe); // Add to the shared list
                                        }
                                    }
                                } catch (Exception e) {
                                    System.err.println("Failed to scrape: " + url + " - " + e.getMessage());
                                } finally {
                                    threadDriver.quit();  // Ensure proper cleanup
                                }
                            });
                        }
                    }
                }
            }

            // Shutdown executor after all tasks are submitted
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.MINUTES);

            // Print the results after scraping all pages
            System.out.println("====== Recipe Details ======");
            for (Recipe recipe : recipes) {
                System.out.println("Recipe Name: " + recipe.name);
                System.out.println("Recipe URL: " + recipe.url);
                System.out.println("Ingredients: " + recipe.ingredients);
                System.out.println("Preparation Method: " + recipe.method);
                System.out.println();
            }

            System.out.println("Total Recipes Scraped: " + recipes.size());

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            quitDriver();
        }

        long endTime = System.nanoTime();
        double durationInSeconds = (endTime - startTime) / 1_000_000_000.0;
        System.out.println("Execution Time: " + durationInSeconds + " seconds");
    }

    public static WebDriver initializeThreadDriver() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = getChromeOptions();
        return new ChromeDriver(options);
    }

    // Scrape a single recipe using the WebDriver from BaseClass
    public static Recipe scrapeRecipe(String url, WebDriver driver) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
            driver.get(url);

            // Wait for recipe name to load
            String recipeName = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//span[@id='ctl00_cntrightpanel_lblRecipeName']"))).getText();
            String ingredients = driver.findElement(By.id("rcpinglist")).getText();
            String preparationMethod = driver.findElement(By.id("recipe_small_steps")).getText();

            return new Recipe(recipeName, url, ingredients, preparationMethod);
        } catch (Exception e) {
            System.err.println("Error scraping recipe at URL: " + url + " - " + e.getMessage());
            return null;
        }
    }

    // Recipe class to store recipe details
    static class Recipe {
        String name;
        String url;
        String ingredients;
        String method;

        public Recipe(String name, String url, String ingredients, String method) {
            this.name = name;
            this.url = url;
            this.ingredients = ingredients;
            this.method = method;
        }
    }
}
