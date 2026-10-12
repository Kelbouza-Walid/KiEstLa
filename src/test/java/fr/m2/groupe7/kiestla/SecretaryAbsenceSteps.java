package fr.m2.groupe7.kiestla;

import io.cucumber.java.PendingException;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import org.springframework.beans.factory.annotation.Autowired;


import com.microsoft.playwright.Page;

import fr.m2.groupe7.kiestla.entity.Absence;
import fr.m2.groupe7.kiestla.entity.AbsenceStatus;
import fr.m2.groupe7.kiestla.entity.Comment;
import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.entity.Module;
import fr.m2.groupe7.kiestla.repository.AbsenceRepository;
import fr.m2.groupe7.kiestla.repository.CommentRepository;
import fr.m2.groupe7.kiestla.repository.ModuleRepository;
import fr.m2.groupe7.kiestla.repository.UserRepository;

public class SecretaryAbsenceSteps {

    @Autowired
    private AbsenceRepository absenceRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommentRepository commentRepository;

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

    // --- Scenario: Adding a comment to an absence (1171) ---

    @Given("The absence has no comment from me")
    public void the_absence_has_no_comment_from_me() {
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

    @When("I write a comment")
    public void i_write_a_comment() {
        getPage().navigate("http://localhost:8080/secretary");
        getPage().getByTestId("btn-see-comments").click();
        getPage().getByTestId("input-comment").fill("Called the parents");
    }

    @When("I save the comment")
    public void i_save_the_comment() {
        getPage().getByTestId("btn-save-comment").click();
    }

    @Then("The comment is displayed on the absence")
    public void the_comment_is_displayed_on_the_absence() {
        assertThat(getPage().getByTestId("comment-author")).hasText("FirstName LastName");
        assertThat(getPage().getByTestId("comment-text")).hasText("Called the parents");

        List<Comment> comments = commentRepository.findByAbsenceIdOrderByDateAsc(currentAbsenceId);
        assertEquals(1, comments.size());
        assertEquals("Called the parents", comments.get(0).getComment());
        assertEquals("secretary", comments.get(0).getCommenter().getLogin());
    }

    // --- Scenario: Attempting to save an empty comment (1186) ---

    @When("I try to save an empty comment")
    public void i_try_to_save_an_empty_comment() {
        getPage().navigate("http://localhost:8080/secretary");
        getPage().getByTestId("btn-see-comments").click();
        getPage().getByTestId("input-comment").fill("   ");
        getPage().getByTestId("btn-save-comment").click();
    }

    @Then("The comment is not saved")
    public void the_comment_is_not_saved() {
        assertThat(getPage().getByTestId("no-comments")).isVisible();
        assertEquals(0, commentRepository.findByAbsenceIdOrderByDateAsc(currentAbsenceId).size());
    }

    @Then("I see an error message indicating the comment cannot be empty")
    public void i_see_an_error_message_indicating_the_comment_cannot_be_empty() {
        assertThat(getPage().getByTestId("comment-error"))
                .hasText("Le commentaire ne peut pas être vide (2000 caractères maximum).");
    }
}
