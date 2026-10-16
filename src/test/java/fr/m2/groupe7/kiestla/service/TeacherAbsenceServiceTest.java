package fr.m2.groupe7.kiestla.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.m2.groupe7.kiestla.dto.ModuleSummary;
import fr.m2.groupe7.kiestla.dto.TeacherAbsenceRow;
import fr.m2.groupe7.kiestla.entity.Absence;
import fr.m2.groupe7.kiestla.entity.AbsenceStatus;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.repository.AbsenceRepository;
import fr.m2.groupe7.kiestla.service.AbsenteeService.SortBy;

@ExtendWith(MockitoExtension.class)
class TeacherAbsenceServiceTest {

    @Mock
    private AbsenceRepository absenceRepository;
    @Mock
    private ModuleService moduleService;
    @InjectMocks
    private TeacherAbsenceService service;

    private void givenData() {
        when(moduleService.getModulesForTeacher("teacher")).thenReturn(List.of(
                new ModuleSummary(10L, "Maths", "Teacher"),
                new ModuleSummary(20L, "Algo", "Teacher")));
        User zoe = user("Zoe", "Adams");
        User bob = user("Bob", "Zimmer");
        when(absenceRepository.findByModuleId(10L)).thenReturn(List.of(absence(zoe, 3)));
        when(absenceRepository.findByModuleId(20L)).thenReturn(List.of(absence(bob, 1), absence(zoe, 2)));
    }

    @Test
    void sortByModule() {
        givenData();

        List<TeacherAbsenceRow> rows = service.getAbsencesForTeacher("teacher", SortBy.MODULE);

        assertEquals(List.of("Algo", "Algo", "Maths"), rows.stream().map(TeacherAbsenceRow::moduleName).toList());
        assertEquals(List.of("Adams", "Zimmer", "Adams"), rows.stream().map(TeacherAbsenceRow::lastName).toList());
    }

    @Test
    void sortByStudent() {
        givenData();

        List<TeacherAbsenceRow> rows = service.getAbsencesForTeacher("teacher", SortBy.STUDENT);

        assertEquals(List.of("Adams", "Adams", "Zimmer"), rows.stream().map(TeacherAbsenceRow::lastName).toList());
        assertEquals(List.of("Algo", "Maths", "Algo"), rows.stream().map(TeacherAbsenceRow::moduleName).toList());
    }

    @Test
    void onlyModulesOfTheTeacherAreQueried() {
        when(moduleService.getModulesForTeacher("teacher"))
                .thenReturn(List.of(new ModuleSummary(10L, "Maths", "Teacher")));
        when(absenceRepository.findByModuleId(10L)).thenReturn(List.of());

        assertTrue(service.getAbsencesForTeacher("teacher", SortBy.MODULE).isEmpty());
        verify(absenceRepository).findByModuleId(10L);
        verifyNoMoreInteractions(absenceRepository);
    }

    private User user(String firstName, String lastName) {
        User u = new User();
        u.setFirstName(firstName);
        u.setLastName(lastName);
        return u;
    }

    private Absence absence(User student, int day) {
        Absence a = new Absence();
        a.setStudent(student);
        a.setDate(LocalDate.of(2026, 10, day));
        a.setStatus(AbsenceStatus.UNJUSTIFIED);
        return a;
    }
}