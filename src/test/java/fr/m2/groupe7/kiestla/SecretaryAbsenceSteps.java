package fr.m2.groupe7.kiestla;

import io.cucumber.java.PendingException;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import org.springframework.beans.factory.annotation.Autowired;


import com.microsoft.playwright.Page;

import fr.m2.groupe7.kiestla.entity.Absence;
import fr.m2.groupe7.kiestla.entity.AbsenceStatus;
import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.entity.Module;
import fr.m2.groupe7.kiestla.repository.AbsenceRepository;
import fr.m2.groupe7.kiestla.repository.ModuleRepository;
import fr.m2.groupe7.kiestla.repository.UserRepository;

public class SecretaryAbsenceSteps {

    @Autowired
    private AbsenceRepository absenceRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private UserRepository userRepository;

    private Long currentAbsenceId;

    // Only set once the justification form has been opened (not the case for an already justified absence)
    private boolean motiveFormOpened = false;

    private Page getPage() {
        return PlaywrightHooks.getPage();
    }

    @Given("I have already logged in")
    public void i_have_already_logged_in() {
        absenceRepository.deleteAll();
        moduleRepository.deleteAll();
        userRepository.deleteAll();
        User secretary = new User();
        secretary.setFirstName("FirstName");
        secretary.setLastName("LastName");
        secretary.setLogin("secretary");
        secretary.setPassword("{noop}testpassword");
        secretary.setRole(Role.SECRETARY);
        userRepository.save(secretary);

        getPage().navigate("http://localhost:8080/login");
        getPage().fill("input[name='login']", "secretary");
        getPage().fill("input[name='password']", "testpassword");
        getPage().click("button[type='submit']");

        getPage().waitForURL("**/secretary");
    }

    // --- Scenario: Adding a valid motive to an absence (1174) ---

    @Given("The student motive is valid")
    public void the_student_motive_is_valid() {
        absenceRepository.deleteAll();

        User student = new User();
        student.setFirstName("Gaspard");
        student.setLastName("Quentin");
        student.setLogin("gquentin");
        student.setPassword("pwd");
        student.setRole(Role.STUDENT);
        student = userRepository.save(student);

        Module module = new Module();
        module.setName("Functionnal testing");
        moduleRepository.save(module);

        Absence absence = new Absence();
        absence.setStudent(student);
        absence.setModule(module);
        absence.setDate(LocalDate.now());
        absence.setStatus(AbsenceStatus.UNJUSTIFIED);
        Absence saved = absenceRepository.save(absence);

        this.currentAbsenceId = saved.getId();
    }

    @When("I enter the motive for the absence")
    public void i_enter_the_motive_for_the_absence() {
        getPage().navigate("http://localhost:8080/secretary");
        getPage().getByTestId("btn-add-justification").click();
        getPage().getByTestId("input-motive-title").fill("Sick");
        motiveFormOpened = true;
    }

    @When("I add a corresponding description")
    public void i_add_a_corresponding_description() {
        if (!motiveFormOpened) {
            return;
        }
        getPage().getByTestId("input-motive-description").fill("Caught a cold");
    }

    @When("I set the absence as justified")
    public void i_set_the_absence_as_justified() {
        getPage().getByTestId("btn-save-justification").click();
    }

    @Then("The absence is registered as justified")
    public void the_absence_is_registered_as_justified() {
        assertThat(getPage().getByTestId("absence-status")).hasText("JUSTIFIED");
        assertThat(getPage().getByTestId("absence-motive-title")).hasText("Sick");
        assertThat(getPage().getByTestId("absence-motive-description")).hasText("Caught a cold");

        Absence updated = absenceRepository.findById(currentAbsenceId).get();
        assertEquals(AbsenceStatus.JUSTIFIED, updated.getStatus());
        assertEquals("Sick", updated.getMotiveTitle());
        assertEquals("Caught a cold", updated.getMotiveDescription());
    }

    // --- Scenario: Changing a justified absence to unjustified (1184) ---

    @Given("The absence is currently justified")
    public void the_absence_is_currently_justified() {
        absenceRepository.deleteAll();

        User student = new User();
        student.setFirstName("Gaspard");
        student.setLastName("Quentin");
        student.setLogin("gquentin");
        student.setPassword("pwd");
        student.setRole(Role.STUDENT);
        student = userRepository.save(student);

        Module module = new Module();
        module.setName("Functionnal testing");
        moduleRepository.save(module);

        Absence absence = new Absence();
        absence.setStudent(student);
        absence.setModule(module);
        absence.setDate(LocalDate.now());
        absence.setStatus(AbsenceStatus.JUSTIFIED);
        absence.setMotiveTitle("Medical");
        absence.setMotiveDescription("Medical thing");
        Absence saved = absenceRepository.save(absence);

        this.currentAbsenceId = saved.getId();
    }

    @When("I enter a new invalid motive for the absence")
    public void i_enter_a_new_invalid_motive_for_the_absence() {
        getPage().navigate("http://localhost:8080/secretary");
    }

    @When("I set the absence as unjustified")
    public void i_set_the_absence_as_unjustified() {
        getPage().onceDialog(dialog -> dialog.accept());
        getPage().getByTestId("btn-remove-justification").click();
    }

    @Then("The absence is registered as unjustified")
    public void the_absence_is_registered_as_unjustified() {
        assertThat(getPage().getByTestId("absence-status")).hasText("UNJUSTIFIED");
        Absence updated = absenceRepository.findById(currentAbsenceId).get();
        assertEquals(AbsenceStatus.UNJUSTIFIED, updated.getStatus());
        assertNull(updated.getMotiveTitle());
        assertNull(updated.getMotiveDescription());
    }
}
