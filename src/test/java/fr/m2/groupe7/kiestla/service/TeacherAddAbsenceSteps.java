package fr.m2.groupe7.kiestla.service;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.List;

import fr.m2.groupe7.kiestla.PlaywrightHooks;
import org.springframework.beans.factory.annotation.Autowired;

import com.microsoft.playwright.Page;

import fr.m2.groupe7.kiestla.entity.Absence;
import fr.m2.groupe7.kiestla.entity.AbsenceStatus;
import fr.m2.groupe7.kiestla.entity.Module;
import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.repository.AbsenceRepository;
import fr.m2.groupe7.kiestla.repository.ModuleRepository;
import fr.m2.groupe7.kiestla.repository.UserRepository;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps for the feature for creating a new absence (ticket TFKIES26-87)
 */
public class TeacherAddAbsenceSteps {

    private static final String BASE_URL = "http://localhost:8080";
    private static final LocalDate ABSENCE_DATE = LocalDate.of(2026, 10, 12);

    @Autowired
    private AbsenceRepository absenceRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private UserRepository userRepository;

    // Shared state between the steps of one scenario
    private User student;
    private Module module;

    private Page getPage() {
        return PlaywrightHooks.getPage();
    }

    @Given("The absent student is registered in the module I teach")
    public void the_absent_student_is_registered_in_the_module_i_teach() {
        // Start from a clean database
        absenceRepository.deleteAll();
        moduleRepository.deleteAll();
        userRepository.deleteAll();

        User teacher = createUser("Ref", "Teacher", "teacher", Role.TEACHER);
        student = createUser("Alice", "Durand", "student1", Role.STUDENT);

        Module newModule = new Module();
        newModule.setName("Programmation");
        newModule.setReferenceTeacher(teacher);
        module = moduleRepository.save(newModule);

        // user is the owning side of the relation
        enroll(teacher, module);
        enroll(student, module);

        // Log in as the teacher
        login("teacher", "testpassword");
    }

    @When("I select the absent student")
    public void i_select_the_absent_student() {
        getPage().navigate(BASE_URL + "/teacher/modules/" + module.getId());
        getPage().getByTestId("select-absence-student").selectOption(String.valueOf(student.getId()));
    }

    @When("I define in which class he was absent")
    public void i_define_in_which_class_he_was_absent() {
        getPage().getByTestId("input-absence-date").fill(ABSENCE_DATE.toString());
        getPage().getByTestId("btn-add-absence").click();
    }

    @Then("A new absence is created and attributed to that student")
    public void a_new_absence_is_created_and_attributed_to_that_student() {
        // What the user sees
        assertThat(getPage().locator("body")).containsText("L'absence a été ajoutée.");

        // What is really stored in the database
        List<Absence> absences = absenceRepository.findByStudentIdAndModuleId(student.getId(), module.getId());
        assertEquals(1, absences.size());
        assertEquals(AbsenceStatus.UNJUSTIFIED, absences.get(0).getStatus());
        assertEquals(ABSENCE_DATE, absences.get(0).getDate());
    }

    // ---------- Helpers ----------

    private User createUser(String firstName, String lastName, String login, Role role) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setLogin(login);
        user.setPassword("{noop}testpassword");
        user.setRole(role);
        return userRepository.save(user);
    }

    private void enroll(User user, Module moduleToJoin) {
        User withModules = userRepository.findWithModulesById(user.getId()).orElseThrow();
        withModules.getModules().add(moduleToJoin);
        userRepository.save(withModules);
    }

    private void login(String login, String password) {
        getPage().navigate(BASE_URL + "/login");
        getPage().fill("input[name='login']", login);
        getPage().fill("input[name='password']", password);
        getPage().click("button[type='submit']");
        getPage().waitForURL("**/teacher");
    }
}