package fr.m2.groupe7.kiestla;

import io.cucumber.java.PendingException;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

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

import java.time.LocalDate;
import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SecretaryModuleDetailsSteps {

    @Autowired
    private AbsenceRepository absenceRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private UserRepository userRepository;

    private Page getPage() {
        return PlaywrightHooks.getPage();
    }

    // =====================================================================
    // Feature 949: Viewing the absences of a student in a module
    // =====================================================================

    private Module studentModule;

    @Given("The student has multiple absences registered in the module")
    public void the_student_has_multiple_absences_registered_in_the_module() {
        User student = saveStudent("Gaspard", "Quentin", "gquentin");
        User other = saveStudent("Noam", "Faivre", "nfaivre");

        studentModule = saveModule("Test fonctionnel");
        Module otherModule = saveModule("Informatique Responsable");

        saveAbsence(student, studentModule, LocalDate.of(2026, 10, 1));
        saveAbsence(student, studentModule, LocalDate.of(2026, 10, 2));
        // must be hidden by the filter: other student / other module
        saveAbsence(other, studentModule, LocalDate.of(2026, 10, 3));
        saveAbsence(student, otherModule, LocalDate.of(2026, 10, 4));
    }

    @When("I open the absence list for that student in that module")
    public void i_open_the_absence_list_for_that_student_in_that_module() {
        getPage().navigate("http://localhost:8080/secretary");
        getPage().getByTestId("select-absence-module").selectOption(String.valueOf(studentModule.getId()));
        getPage().getByTestId("input-absence-student").fill("Gaspard Quentin");
        getPage().getByTestId("btn-filter-absences").click();
        getPage().waitForURL(url -> url.contains("absenceModuleId="));
    }

    @Then("I see all of his absences listed individually")
    public void i_see_all_of_his_absences_listed_individually() {
        assertThat(getPage().getByTestId("absence-student")).hasCount(2);
        assertEquals(List.of("Gaspard Quentin", "Gaspard Quentin"),
                getPage().getByTestId("absence-student").allTextContents());
    }

    private User saveStudent(String firstName, String lastName, String login) {
        User student = new User();
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setLogin(login);
        student.setPassword("pwd");
        student.setRole(Role.STUDENT);
        return userRepository.save(student);
    }

    private Module saveModule(String name) {
        Module module = new Module();
        module.setName(name);
        return moduleRepository.save(module);
    }

    private void saveAbsence(User student, Module module, LocalDate date) {
        Absence absence = new Absence();
        absence.setStudent(student);
        absence.setModule(module);
        absence.setDate(date);
        absence.setStatus(AbsenceStatus.UNJUSTIFIED);
        absenceRepository.save(absence);
    }

    // =====================================================================
    // Feature 1183: Absence list is empty for a module with no absences
    // =====================================================================

    @Given("The module has no absences registered")
    public void the_module_has_no_absences_registered() {
        Module module = new Module();
        module.setName("Functionnal testing");
        moduleRepository.save(module);
    }

    @When("I open the absence list for that module")
    public void i_open_the_absence_list_for_that_module() {
        getPage().navigate("http://localhost:8080/secretary");
        getPage().getByTestId("btn-module-details").click();
    }

    @Then("The list is empty")
    public void the_list_is_empty() {
        if (getPage().url().contains("/module-details/")) {
            // Feature 1183: absences of a module
            assertThat(getPage().getByTestId("module-status")).hasText("No absences found for this module.");
        } else {
            // Feature 1182: follow-up list of the secretary page
            assertThat(getPage().getByTestId("absentee-row")).hasCount(0);
            assertThat(getPage().getByTestId("no-absentees")).isVisible();
        }
    }
    // =====================================================================
    // Feature 959: Viewing module absences sorted by student
    // =====================================================================

    @Given("The module has multiple students with registered absences")
    public void the_module_has_multiple_students_with_registered_absences() {
        // TODO: Créer un module avec plusieurs étudiants ayant des absences
        throw new PendingException();
    }

    // Note: "I open the absence list for that module" est réutilisé depuis le bloc 1183

    @When("I sort the list by student")
    public void i_sort_the_list_by_student() {
        getPage().getByTestId("btn-sort-student").click();
        getPage().waitForURL(url -> url.contains("sort=STUDENT"));
    }

    @Then("I see all absences of the module ordered by student")
    public void i_see_all_absences_of_the_module_ordered_by_student() {
        // TODO: Vérifier que les absences sont triées par étudiant
        // Not implemented yet
        throw new PendingException();
    }

    // =====================================================================
    // Feature 1168: Viewing absences sorted by module
    // =====================================================================

    @Given("Several modules have students with registered absences")
    public void several_modules_have_students_with_registered_absences() {
        // TODO: Créer plusieurs modules avec des étudiants ayant des absences
        throw new PendingException();
    }

    @When("I open the absence list")
    public void i_open_the_absence_list() {
        // TODO: Naviguer vers la page listant toutes les absences
        throw new PendingException();
    }

    @When("I sort the list by module")
    public void i_sort_the_list_by_module() {
        getPage().getByTestId("btn-sort-module").click();
        getPage().waitForURL(url -> url.contains("sort=MODULE"));
    }

    @Then("I see all absences ordered by module")
    public void i_see_all_absences_ordered_by_module() {
        // TODO: Vérifier que les absences sont triées par module
        throw new PendingException();
    }
}
