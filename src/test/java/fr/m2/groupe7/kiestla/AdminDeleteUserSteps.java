package fr.m2.groupe7.kiestla;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.springframework.beans.factory.annotation.Autowired;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import fr.m2.groupe7.kiestla.repository.UserRepository;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps for the "remove users" scenario of the user management feature (921).
 * "The user exists" (AdminUpdateUserSteps) creates the user and logs in as administrator,
 * "I confirm the deletion" (AdminModuleSteps) clicks the delete button.
 */
public class AdminDeleteUserSteps {

    private static final String BASE_URL = "http://localhost:8080";
    // Created by "The user exists"
    private static final String LOGIN = "nfaivre";

    @Autowired
    private UserRepository userRepository;

    private Page getPage() {
        return PlaywrightHooks.getPage();
    }

    @When("I select the user to remove")
    public void i_select_the_user_to_remove() {
        getPage().navigate(BASE_URL + "/admin");
        getPage().getByTestId("input-user-search").fill(LOGIN);
        getPage().getByTestId("btn-search-user").click();
        assertThat(getPage().getByTestId("user-row")).hasCount(1);
        assertThat(getPage().getByTestId("user-login")).hasText(LOGIN);
    }

    @Then("The user is no longer registered in the system")
    public void the_user_is_no_longer_registered_in_the_system() {
        assertThat(getPage().getByTestId("delete-success-message")).isVisible();
        assertThat(getPage().getByTestId("user-login")
                .filter(new Locator.FilterOptions().setHasText(LOGIN))).hasCount(0);
        assertTrue(userRepository.findByLogin(LOGIN).isEmpty());
    }
}
