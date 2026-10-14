package fr.m2.groupe7.kiestla;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.microsoft.playwright.Page;

import fr.m2.groupe7.kiestla.entity.Module;
import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.repository.ModuleRepository;
import fr.m2.groupe7.kiestla.repository.UserRepository;

import io.cucumber.java.After;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps for the feature "add/remove teachers and students to/from modules" (926, ticket TFKIES26-108).
 * "The module exists" and "I confirm the update" are shared steps defined in
 * AdminModuleSteps and AdminUpdateUserSteps.
 */
public class AdminModuleMembersSteps {

    private static final String BASE_URL = "http://localhost:8080";
    // Created by "The module exists"
    private static final String MODULE_NAME = "Mathématiques";

    private static final List<String> TEACHERS = List.of("mcurie", "aeinstein");
    private static final List<String> STUDENTS = List.of("nfaivre", "skacmaz");
    // Member that is not selected in the remove scenario: must stay enrolled
    private static final String REMAINING_STUDENT = "mregazzoni";

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private UserRepository userRepository;

    private Page getPage() {
        return PlaywrightHooks.getPage();
    }

    // ---------- add teachers and students ----------

    @Given("The teachers and students to add exist")
    public void the_teachers_and_students_to_add_exist() {
        TEACHERS.forEach(login -> createUser(login, Role.TEACHER));
        STUDENTS.forEach(login -> createUser(login, Role.STUDENT));
    }

    @When("I select the teachers to add to the module")
    public void i_select_the_teachers_to_add_to_the_module() {
        openMembersPage();
        TEACHERS.forEach(login -> getPage().getByTestId("checkbox-teacher-" + login).check());
    }

    @When("I select the students to add to the module")
    public void i_select_the_students_to_add_to_the_module() {
        STUDENTS.forEach(login -> getPage().getByTestId("checkbox-student-" + login).check());
    }

    @Then("The teachers and students are registered as members of the module")
    public void the_teachers_and_students_are_registered_as_members_of_the_module() {
        assertThat(getPage().getByTestId("success-message")).isVisible();
        for (String login : allMembers()) {
            assertTrue(isMember(login), login + " should be a member of the module");
        }
    }

    // ---------- remove teachers and students ----------

    @Given("The teachers and students to remove are currently members of the module")
    public void the_teachers_and_students_to_remove_are_currently_members_of_the_module() {
        Module module = moduleRepository.findByName(MODULE_NAME).orElseThrow();
        TEACHERS.forEach(login -> enroll(createUser(login, Role.TEACHER), module));
        STUDENTS.forEach(login -> enroll(createUser(login, Role.STUDENT), module));
        enroll(createUser(REMAINING_STUDENT, Role.STUDENT), module);
    }

    @When("I select the teachers to remove from the module")
    public void i_select_the_teachers_to_remove_from_the_module() {
        openMembersPage();
        TEACHERS.forEach(login -> getPage().getByTestId("checkbox-teacher-" + login).uncheck());
    }

    @When("I select the students to remove from the module")
    public void i_select_the_students_to_remove_from_the_module() {
        STUDENTS.forEach(login -> getPage().getByTestId("checkbox-student-" + login).uncheck());
    }

    @Then("The teachers and students are no longer registered as members of the module")
    public void the_teachers_and_students_are_no_longer_registered_as_members_of_the_module() {
        assertThat(getPage().getByTestId("success-message")).isVisible();
        for (String login : allMembers()) {
            assertFalse(isMember(login), login + " should not be a member of the module anymore");
        }
        assertTrue(isMember(REMAINING_STUDENT));
        assertThat(getPage().getByTestId("checkbox-student-" + REMAINING_STUDENT)).isChecked();
    }

    /**
     * The other features clean the database by deleting the modules before the users:
     * the enrollments created here must be removed first, or the deletion fails.
     */
    @After
    public void removeEnrollments(Scenario scenario) {
        if (!scenario.getUri().toString().contains("926_")) {
            return;
        }
        userRepository.findAll().forEach(user -> {
            User withModules = userRepository.findWithModulesById(user.getId()).orElseThrow();
            if (!withModules.getModules().isEmpty()) {
                withModules.getModules().clear();
                userRepository.save(withModules);
            }
        });
    }

    // ---------- Helpers ----------

    private void openMembersPage() {
        getPage().navigate(BASE_URL + "/admin/modules");
        getPage().getByTestId("input-module-search").fill(MODULE_NAME);
        getPage().getByTestId("btn-search-module").click();
        getPage().getByTestId("btn-module-members").click();
        assertThat(getPage().getByTestId("members-module-name")).hasText(MODULE_NAME);
    }

    private List<String> allMembers() {
        return java.util.stream.Stream.concat(TEACHERS.stream(), STUDENTS.stream()).toList();
    }

    private boolean isMember(String login) {
        return userRepository.findModulesByLogin(login).orElseThrow().getModules().stream()
                .anyMatch(m -> m.getName().equals(MODULE_NAME));
    }

    private void enroll(User user, Module module) {
        user.getModules().add(module);
        userRepository.save(user);
    }

    private User createUser(String login, Role role) {
        User user = new User();
        user.setFirstName("First");
        user.setLastName(login);
        user.setLogin(login);
        user.setPassword("{noop}testpassword");
        user.setRole(role);
        return userRepository.save(user);
    }
}
