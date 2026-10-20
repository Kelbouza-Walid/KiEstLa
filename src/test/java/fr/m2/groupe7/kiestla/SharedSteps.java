package fr.m2.groupe7.kiestla;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Then;

/**
 * Steps whose sentence is used by several features with a different page behind it.
 * Cucumber accepts only one definition per sentence, so they are defined once here.
 */
public class SharedSteps {

    private String featureUri;

    private Page getPage() {
        return PlaywrightHooks.getPage();
    }

    @Before
    public void rememberFeature(Scenario scenario) {
        this.featureUri = scenario.getUri().toString();
    }

    /**
     * After a failed action, the button of that action is still usable:
     * logout (929), password change (945) or the login form (893).
     */
    @Then("I get to retry")
    public void i_get_to_retry() {
        Locator retryButton;
        if (featureUri.contains("929_")) {
            retryButton = getPage().getByTestId("btn-logout");
        } else if (featureUri.contains("945_")) {
            retryButton = getPage().getByTestId("btn-change-password");
        } else {
            retryButton = getPage().locator("form button[type='submit']");
        }
        assertThat(retryButton).isVisible();
        assertThat(retryButton).isEnabled();
    }
}
