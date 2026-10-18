package fr.m2.groupe7.kiestla;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import fr.m2.groupe7.kiestla.entity.Module;
import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.repository.ModuleRepository;
import fr.m2.groupe7.kiestla.repository.UserRepository;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps for the feature "Student views reference teachers of modules" (931).
 * The student is created and logged in by "I am logged in as a student with username ..." (StudentAbsenceSteps),
 * and the database is cleaned by its @After hook.
 */
public class StudentModulesSteps {

    private static final String BASE_URL = "http://localhost:8080";

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private UserRepository userRepository;

    private Page getPage() {
        return PlaywrightHooks.getPage();
    }

    @Given("I am enrolled in module {string} with reference teacher {string}")
    public void i_am_enrolled_in_module_with_reference_teacher(String moduleName, String teacherName) {
        // The student page displays the last name of the reference teacher
        User teacher = new User();
        teacher.setFirstName("Teacher");
        teacher.setLastName(teacherName);
        teacher.setLogin(teacherName);
        teacher.setPassword("{noop}testpassword");
        teacher.setRole(Role.TEACHER);
        teacher = userRepository.save(teacher);

        Module module = new Module();
        module.setName(moduleName);
        module.setReferenceTeacher(teacher);
        module = moduleRepository.save(module);

        User student = loggedInStudent();
        student.getModules().add(module);
        userRepository.save(student);
    }

    @Given("I am not enrolled in any modules")
    public void i_am_not_enrolled_in_any_modules() {
        User student = loggedInStudent();
        student.getModules().clear();
        userRepository.save(student);
    }

    @When("I navigate to my modules page")
    public void i_navigate_to_my_modules_page() {
        // The modules are listed on the student's home page
        getPage().navigate(BASE_URL + "/student");
    }

    @Then("I should see module {string} with reference teacher {string}")
    public void i_should_see_module_with_reference_teacher(String moduleName, String teacherName) {
        Locator row = getPage().getByTestId("student-module")
                .filter(new Locator.FilterOptions().setHasText(moduleName));
        assertThat(row).hasCount(1);
        assertThat(row.getByTestId("module-name")).hasText(moduleName);
        assertThat(row.getByTestId("module-teacher")).hasText(teacherName);
    }

    private User loggedInStudent() {
        List<User> students = userRepository.findByRole(Role.STUDENT);
        assertEquals(1, students.size(), "The scenario expects a single logged in student");
        return userRepository.findWithModulesById(students.get(0).getId()).orElseThrow();
    }
}
