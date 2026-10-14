package fr.m2.groupe7.kiestla;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.springframework.beans.factory.annotation.Autowired;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.repository.AbsenceRepository;
import fr.m2.groupe7.kiestla.repository.ModuleRepository;
import fr.m2.groupe7.kiestla.repository.UserRepository;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps for the "update users" scenario of the user management feature (921).
 */
public class AdminUpdateUserSteps {

    private static final String BASE_URL = "http://localhost:8080";

    private static final String OLD_LOGIN = "nfaivre";
    private static final String OLD_PASSWORD = "{noop}studentpassword";

    private static final String NEW_FIRST_NAME = "Noa";
    private static final String NEW_LAST_NAME = "Faivre-Martin";
    private static final String NEW_LOGIN = "nfaivremartin";
    private static final Role NEW_ROLE = Role.SECRETARY;

    @Autowired
    private AbsenceRepository absenceRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private UserRepository userRepository;

    private Page getPage() {
        return PlaywrightHooks.getPage();
    }

    @Given("The user exists")
    public void the_user_exists() {
        absenceRepository.deleteAll();
        moduleRepository.deleteAll();
        userRepository.deleteAll();

        User admin = new User();
        admin.setFirstName("System");
        admin.setLastName("Admin");
        admin.setLogin("admin");
        admin.setPassword("{noop}testpassword");
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        User student = new User();
        student.setFirstName("Noam");
        student.setLastName("Faivre");
        student.setLogin(OLD_LOGIN);
        student.setPassword(OLD_PASSWORD);
        student.setRole(Role.STUDENT);
        userRepository.save(student);

        // Log in as the administrator
        getPage().navigate(BASE_URL + "/login");
        getPage().fill("input[name='login']", "admin");
        getPage().fill("input[name='password']", "testpassword");
        getPage().click("button[type='submit']");
        getPage().waitForURL("**/admin");
    }

    @When("I edit the user's information")
    public void i_edit_the_user_s_information() {
        getPage().navigate(BASE_URL + "/admin");
        getPage().locator("tr").filter(new Locator.FilterOptions().setHasText(OLD_LOGIN))
                .getByTestId("btn-edit-user").click();
        getPage().waitForURL("**/admin/users/*/edit");

        assertThat(getPage().getByTestId("input-login")).hasValue(OLD_LOGIN);
        getPage().getByTestId("input-first-name").fill(NEW_FIRST_NAME);
        getPage().getByTestId("input-last-name").fill(NEW_LAST_NAME);
        getPage().getByTestId("input-login").fill(NEW_LOGIN);
        getPage().getByTestId("select-role").selectOption(NEW_ROLE.name());
    }

    @When("I confirm the update")
    public void i_confirm_the_update() {
        getPage().getByTestId("btn-update-user").click();
    }

    @Then("The user's information is updated in the system")
    public void the_user_s_information_is_updated_in_the_system() {
        getPage().waitForURL("**/admin");
        assertThat(getPage().getByTestId("success-message")).isVisible();
        assertThat(getPage().getByTestId("user-login").filter(
                new Locator.FilterOptions().setHasText(NEW_LOGIN))).hasCount(1);

        assertFalse(userRepository.findByLogin(OLD_LOGIN).isPresent());
        User updated = userRepository.findByLogin(NEW_LOGIN).orElseThrow();
        assertEquals(NEW_FIRST_NAME, updated.getFirstName());
        assertEquals(NEW_LAST_NAME, updated.getLastName());
        assertEquals(NEW_ROLE, updated.getRole());
        // The password is not changed by the update
        assertEquals(OLD_PASSWORD, updated.getPassword());
    }
}
