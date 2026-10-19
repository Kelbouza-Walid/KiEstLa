package fr.m2.groupe7.kiestla;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;

import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Page;

import fr.m2.groupe7.kiestla.entity.Absence;
import fr.m2.groupe7.kiestla.entity.Comment;
import fr.m2.groupe7.kiestla.entity.Module;
import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.repository.AbsenceRepository;
import fr.m2.groupe7.kiestla.repository.CommentRepository;
import fr.m2.groupe7.kiestla.repository.ModuleRepository;
import fr.m2.groupe7.kiestla.repository.NotifRepository;
import fr.m2.groupe7.kiestla.repository.UserRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class TeacherCommentsSteps {

    private static final String BASE_URL = "http://localhost:8080";
    private static final String PASSWORD = "testpassword";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private AbsenceRepository absenceRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private NotifRepository notifRepository;

    private User teacher;
    private Module module;
    private Absence absence;
    private APIResponse lastResponse;

    private Page getPage() {
        return PlaywrightHooks.getPage();
    }

    @Given("I am logged in as the teacher {string}")
    public void i_am_logged_in_as_the_teacher(String fullName) {
        cleanDatabase();
        teacher = createUser(fullName, Role.TEACHER);
        login(teacher, "/teacher");
    }

    @Given("{string} is absent in my module {string}")
    public void is_absent_in_my_module(String studentName, String moduleName) {
        module = createModule(moduleName, teacher);
        absence = createAbsence(createUser(studentName, Role.STUDENT), module);
    }

    @Given("{string} is absent in the module {string} I am not in")
    public void is_absent_in_the_module_i_am_not_in(String studentName, String moduleName) {
        Module otherModule = createModule(moduleName, createUser("Nicolas Marilleau", Role.TEACHER));
        absence = createAbsence(createUser(studentName, Role.STUDENT), otherModule);
    }

    @Given("I have already commented {string} on the absence")
    public void i_have_already_commented_on_the_absence(String text) {
        saveComment(teacher, text);
    }

    @Given("The secretary {string} has commented {string} on the absence")
    public void the_secretary_has_commented_on_the_absence(String secretaryName, String text) {
        saveComment(createUser(secretaryName, Role.SECRETARY), text);
    }

    @When("I open the comments of the absence from my list of absences")
    public void i_open_the_comments_of_the_absence_from_my_list_of_absences() {
        getPage().navigate(BASE_URL + "/teacher/absences");
        getPage().getByTestId("btn-see-comments").click();
    }

    @When("I open the comments of the absence from the details of my module")
    public void i_open_the_comments_of_the_absence_from_the_details_of_my_module() {
        getPage().navigate(BASE_URL + "/module-details/" + module.getId());
        getPage().getByTestId("btn-see-comments").click();
    }

    @When("I send the comment {string}")
    public void i_send_the_comment(String text) {
        getPage().getByTestId("input-comment").fill(text);
        getPage().getByTestId("btn-save-comment").click();
    }

    @When("I change my comment to {string}")
    public void i_change_my_comment_to(String text) {
        getPage().getByTestId("btn-edit-comment").click();
        getPage().getByTestId("input-edit-comment").fill(text);
        getPage().getByTestId("btn-save-edit-comment").click();
    }

    @When("The secretary {string} opens the comments of the absence")
    public void the_secretary_opens_the_comments_of_the_absence(String secretaryName) {
        User secretary = createUser(secretaryName, Role.SECRETARY);
        getPage().context().clearCookies();
        login(secretary, "/secretary");
        getPage().getByTestId("btn-see-comments").click();
    }

    @When("I try to load the comments of that absence")
    public void i_try_to_load_the_comments_of_that_absence() {
        lastResponse = getPage().request().get(BASE_URL + "/absences/" + absence.getId() + "/comments");
    }

    @Then("The comment {string} written by {string} is displayed")
    public void the_comment_written_by_is_displayed(String text, String author) {
        assertThat(getPage().getByTestId("comment-author")).hasText(author);
        assertThat(getPage().getByTestId("comment-text")).hasText(text);
    }

    @Then("The absence has {int} comment saved")
    public void the_absence_has_comment_saved(int count) {
        assertEquals(count, commentRepository.findByAbsenceIdOrderByDateAsc(absence.getId()).size());
    }

    @Then("I am told that the comment cannot be empty")
    public void i_am_told_that_the_comment_cannot_be_empty() {
        assertThat(getPage().getByTestId("no-comments")).isVisible();
        assertThat(getPage().getByTestId("comment-error"))
                .hasText("Le commentaire ne peut pas être vide (2000 caractères maximum).");
    }

    @Then("I cannot edit or delete that comment")
    public void i_cannot_edit_or_delete_that_comment() {
        assertThat(getPage().getByTestId("comment-text")).isVisible();
        assertThat(getPage().getByTestId("btn-edit-comment")).hasCount(0);
        assertThat(getPage().getByTestId("btn-delete-comment")).hasCount(0);
    }

    @Then("I am not allowed to read them")
    public void i_am_not_allowed_to_read_them() {
        assertEquals(403, lastResponse.status());
    }

    private void cleanDatabase() {
        commentRepository.deleteAll();
        notifRepository.deleteAll();
        absenceRepository.deleteAll();
        moduleRepository.findAll().forEach(m -> {
            m.setReferenceTeacher(null);
            moduleRepository.save(m);
        });
        userRepository.deleteAll();
        moduleRepository.deleteAll();
    }

    private void login(User user, String expectedRedirect) {
        getPage().navigate(BASE_URL + "/login");
        getPage().fill("input[name='login']", user.getLogin());
        getPage().fill("input[name='password']", PASSWORD);
        getPage().click("button[type='submit']");
        getPage().waitForURL("**" + expectedRedirect);
    }

    /**
     * Creates a user from "FirstName LastName", with a login like "fbouquet".
     */
    private User createUser(String fullName, Role role) {
        String[] names = fullName.split(" ", 2);
        User user = new User();
        user.setFirstName(names[0]);
        user.setLastName(names[1]);
        user.setLogin((names[0].charAt(0) + names[1]).toLowerCase());
        user.setPassword("{noop}" + PASSWORD);
        user.setRole(role);
        return userRepository.save(user);
    }

    private Module createModule(String name, User referenceTeacher) {
        Module newModule = new Module();
        newModule.setName(name);
        newModule.setReferenceTeacher(referenceTeacher);
        newModule = moduleRepository.save(newModule);
        referenceTeacher.getModules().add(newModule);
        userRepository.save(referenceTeacher);
        return newModule;
    }

    private Absence createAbsence(User student, Module absenceModule) {
        student.getModules().add(absenceModule);
        userRepository.save(student);
        Absence newAbsence = new Absence();
        newAbsence.setStudent(student);
        newAbsence.setModule(absenceModule);
        newAbsence.setDate(LocalDate.now());
        return absenceRepository.save(newAbsence);
    }

    private void saveComment(User commenter, String text) {
        Comment comment = new Comment();
        comment.setAbsence(absence);
        comment.setCommenter(commenter);
        comment.setDate(LocalDateTime.now());
        comment.setComment(text);
        commentRepository.save(comment);
    }
}
