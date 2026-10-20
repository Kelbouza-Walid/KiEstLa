package fr.m2.groupe7.kiestla;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.springframework.beans.factory.annotation.Autowired;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.SelectOption;

import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.repository.ModuleRepository;
import fr.m2.groupe7.kiestla.repository.UserRepository;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps for the feature "define reference teachers for modules" (925).
 * "The module exists" and "I confirm the update" are shared steps defined in
 * AdminModuleSteps and AdminUpdateUserSteps.
 */
public class AdminModuleReferenceSteps {

    private static final String BASE_URL = "http://localhost:8080";
    // Created by "The module exists", with Ada Lovelace as reference teacher
    private static final String MODULE_NAME = "Mathématiques";
    private static final String NEW_REFERENCE_LOGIN = "aturing";

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private UserRepository userRepository;

    private Page getPage() {
        return PlaywrightHooks.getPage();
    }

    @When("I select the teacher as the reference teacher for the module")
    public void i_select_the_teacher_as_the_reference_teacher_for_the_module() {
        User teacher = new User();
        teacher.setFirstName("Alan");
        teacher.setLastName("Turing");
        teacher.setLogin(NEW_REFERENCE_LOGIN);
        teacher.setPassword("{noop}testpassword");
        teacher.setRole(Role.TEACHER);
        userRepository.save(teacher);

        getPage().navigate(BASE_URL + "/admin/modules");
        getPage().getByTestId("input-module-search").fill(MODULE_NAME);
        getPage().getByTestId("btn-search-module").click();
        getPage().getByTestId("btn-module-members").click();
        assertThat(getPage().getByTestId("members-module-name")).hasText(MODULE_NAME);

        getPage().getByTestId("select-reference-teacher")
                .selectOption(new SelectOption().setLabel("Alan Turing (" + NEW_REFERENCE_LOGIN + ")"));
    }

    @Then("The teacher is registered as the reference teacher of the module")
    public void the_teacher_is_registered_as_the_reference_teacher_of_the_module() {
        assertThat(getPage().getByTestId("success-message")).isVisible();
        assertThat(getPage().getByTestId("select-reference-teacher").locator("option:checked"))
                .hasText("Alan Turing (" + NEW_REFERENCE_LOGIN + ")");
        // The old reference teacher can now be checked as a regular teacher
        assertThat(getPage().getByTestId("checkbox-teacher-alovelace")).not().isChecked();

        User reference = moduleRepository.findByName(MODULE_NAME).orElseThrow().getReferenceTeacher();
        assertEquals(NEW_REFERENCE_LOGIN, reference.getLogin());
    }
}
