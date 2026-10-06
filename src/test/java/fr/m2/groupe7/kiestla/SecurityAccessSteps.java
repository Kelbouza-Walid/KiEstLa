package fr.m2.groupe7.kiestla;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.repository.AbsenceRepository;
import fr.m2.groupe7.kiestla.repository.ModuleRepository;
import fr.m2.groupe7.kiestla.repository.UserRepository;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SecurityAccessSteps {

    private static final String BASE_URL = "http://localhost:8080";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AbsenceRepository absenceRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    private Page page;
    private Response lastResponse;

    @Before(order = 1)
    public void setupPage() {
        this.page = PlaywrightHooks.getPage();
    }

    @Given("I am logged in as a secretary")
    public void i_am_logged_in_as_a_secretary() {
        loginAsUser("sec_user", Role.SECRETARY, "/secretary");
    }

    @Given("I am logged in as a teacher")
    public void i_am_logged_in_as_a_teacher() {
        loginAsUser("teacher_user", Role.TEACHER, "/teacher");
    }

    private void loginAsUser(String login, Role role, String expectedRedirect) {
        absenceRepository.deleteAll();
        moduleRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setLogin(login);
        user.setPassword("{noop}testpassword");
        user.setRole(role);
        userRepository.save(user);

        page.navigate(BASE_URL + "/login");
        page.fill("input[name='login']", login);
        page.fill("input[name='password']", "testpassword");
        page.click("button[type='submit']");
        page.waitForURL("**" + expectedRedirect);
    }

    @When("I attempt to navigate to {string}")
    public void i_attempt_to_navigate_to(String path) {
        this.lastResponse = page.navigate(BASE_URL + path);
    }

    @Then("I should see the 403 access denied page")
    public void i_should_see_the_403_access_denied_page() {
        if (lastResponse != null) {
            assertEquals(403, lastResponse.status());
        }
        assertThat(page.getByTestId("error-code")).hasText("403");
        assertThat(page.getByTestId("error-title")).hasText("Access Denied");
        assertThat(page.getByTestId("error-message")).containsText("Sorry, you do not have permission to access this page.");
        assertThat(page.getByTestId("btn-back-home")).isVisible();
    }
}
