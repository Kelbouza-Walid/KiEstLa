package fr.m2.groupe7.kiestla;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import io.cucumber.java.After;
import io.cucumber.java.Before;

/**
 * Centralised Playwright lifecycle management.
 * A single browser/page is created per scenario and shared across all step classes.
 */
public class PlaywrightHooks {

    private static Playwright playwright;
    private static Browser browser;
    private static Page page;

    @Before(order = 0)
    public void setupBrowser() {
        playwright = Playwright.create();
        boolean isCI = System.getenv("CI") != null || System.getProperty("CI") != null;
        browser = playwright.chromium().launch(
                new com.microsoft.playwright.BrowserType.LaunchOptions()
                        .setHeadless(isCI)
                        .setSlowMo(isCI ? 0 : 1000));
        page = browser.newPage();
    }

    @After(order = 0)
    public void closeBrowser() {
        if (browser != null) {
            browser.close();
            browser = null;
        }
        if (playwright != null) {
            playwright.close();
            playwright = null;
        }
        page = null;
    }

    public static Page getPage() {
        return page;
    }
}
