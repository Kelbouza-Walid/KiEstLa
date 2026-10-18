package fr.m2.groupe7.kiestla.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import fr.m2.groupe7.kiestla.dto.ModuleDetails;
import fr.m2.groupe7.kiestla.dto.UserSummary;
import fr.m2.groupe7.kiestla.exception.BusinessException;
import fr.m2.groupe7.kiestla.repository.ModuleRepository;
import fr.m2.groupe7.kiestla.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.m2.groupe7.kiestla.dto.AbsenceSummary;
import fr.m2.groupe7.kiestla.dto.JustifyAbsenceRequest;
import fr.m2.groupe7.kiestla.entity.Absence;
import fr.m2.groupe7.kiestla.entity.AbsenceStatus;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.entity.Module;
import fr.m2.groupe7.kiestla.exception.ResourceNotFoundException;
import fr.m2.groupe7.kiestla.repository.AbsenceRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AbsenceService.
 */
@ExtendWith(MockitoExtension.class)
class AbsenceServiceTest {

    @Mock
    private AbsenceRepository absenceRepository;

    @Mock
    private ModuleRepository moduleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ModuleService moduleService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AbsenceService absenceService;

    @Test
    void getAbsences_basic() {
        Absence absence = createAbsence(1L, "Noam", "Faivre", LocalDate.of(2026, 10, 1), AbsenceStatus.UNJUSTIFIED,
                null, null);
        when(absenceRepository.findAllByOrderByDateDesc()).thenReturn(List.of(absence));

        List<AbsenceSummary> summaries = absenceService.getAbsences();

        assertEquals(1, summaries.size());
        assertEquals(1L, summaries.get(0).id());
        assertEquals("Noam Faivre", summaries.get(0).studentName());
        assertEquals(LocalDate.of(2026, 10, 1), summaries.get(0).date());
        assertEquals(AbsenceStatus.UNJUSTIFIED, summaries.get(0).status());
        assertNull(summaries.get(0).motiveTitle());
        assertNull(summaries.get(0).motiveDescription());
        verify(absenceRepository).findAllByOrderByDateDesc();
    }

    @Test
    void getAbsencesForModule_basic() {
        Absence absence = createAbsence(2L, "Suleyman", "Kacmaz", LocalDate.of(2026, 10, 2), AbsenceStatus.JUSTIFIED,
                "Malade", "Certificat médical");
        when(absenceRepository.findByModuleId(10L)).thenReturn(List.of(absence));

        List<AbsenceSummary> summaries = absenceService.getAbsencesForModule(10L);

        assertEquals(1, summaries.size());
        assertEquals(2L, summaries.get(0).id());
        assertEquals("Suleyman Kacmaz", summaries.get(0).studentName());
        assertEquals(AbsenceStatus.JUSTIFIED, summaries.get(0).status());
        assertEquals("Malade", summaries.get(0).motiveTitle());
        assertEquals("Certificat médical", summaries.get(0).motiveDescription());
        verify(absenceRepository).findByModuleId(10L);
    }

    @Test
    void getAbsenceForStudent_returnsOnlyRequestedStudentsAbsence() {
        Absence absence = createAbsence(3L, "Alice", "Durand", LocalDate.of(2026, 10, 3),
                AbsenceStatus.JUSTIFIED, "Malade", "Certificat médical");
        Module module = new Module();
        module.setName("Programmation");
        absence.setModule(module);
        when(absenceRepository.findByIdAndStudentLogin(3L, "student1")).thenReturn(Optional.of(absence));

        var summary = absenceService.getAbsenceForStudent(3L, "student1");

        assertEquals(3L, summary.id());
        assertEquals("Programmation", summary.moduleName());
        assertEquals(AbsenceStatus.JUSTIFIED, summary.status());
        verify(absenceRepository).findByIdAndStudentLogin(3L, "student1");
    }

    @Test
    void getAbsenceForStudent_rejectsAbsenceNotOwnedByStudent() {
        when(absenceRepository.findByIdAndStudentLogin(3L, "student1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> absenceService.getAbsenceForStudent(3L, "student1"));
    }

    @Test
    void justifyAbsence_basic() {
        Absence absence = createAbsence(1L, "Mathis", "Regazzoni", LocalDate.of(2026, 10, 1), AbsenceStatus.UNJUSTIFIED,
                null, null);
        when(absenceRepository.findById(1L)).thenReturn(Optional.of(absence));

        JustifyAbsenceRequest request = new JustifyAbsenceRequest("Sick", "Caught a cold");
        absenceService.justifyAbsence(1L, request);

        assertEquals(AbsenceStatus.JUSTIFIED, absence.getStatus());
        assertEquals("Sick", absence.getMotiveTitle());
        assertEquals("Caught a cold", absence.getMotiveDescription());
        verify(absenceRepository).findById(1L);
    }

    @Test
    void justifyAbsence_whenCountDropsBelow3_removesNotification() {
        User student = new User();
        student.setId(10L);
        Module module = new Module();
        module.setId(20L);

        Absence absence = new Absence();
        absence.setId(1L);
        absence.setStudent(student);
        absence.setModule(module);
        absence.setStatus(AbsenceStatus.UNJUSTIFIED);

        when(absenceRepository.findById(1L)).thenReturn(Optional.of(absence));
        when(absenceRepository.countByStudentIdAndModuleIdAndStatus(10L, 20L, AbsenceStatus.UNJUSTIFIED)).thenReturn(2L);

        JustifyAbsenceRequest request = new JustifyAbsenceRequest("Sick", "Medical certificate");
        absenceService.justifyAbsence(1L, request);

        assertEquals(AbsenceStatus.JUSTIFIED, absence.getStatus());
        verify(notificationService).removeNotificationsForStudentAndModule(10L, 20L);
    }

    @Test
    void justifyAbsence_absenceNotFound() {
        when(absenceRepository.findById(99L)).thenReturn(Optional.empty());

        JustifyAbsenceRequest request = new JustifyAbsenceRequest("Raison", "Description");

        assertThrows(ResourceNotFoundException.class, () -> absenceService.justifyAbsence(99L, request));
    }

    @Test
    void unjustifyAbsence_basic() {
        Absence absence = createAbsence(1L, "Gaspard", "Quentin", LocalDate.of(2026, 10, 1), AbsenceStatus.JUSTIFIED,
                "Raison passée", "Description passée");
        when(absenceRepository.findById(1L)).thenReturn(Optional.of(absence));

        absenceService.unjustifyAbsence(1L);

        assertEquals(AbsenceStatus.UNJUSTIFIED, absence.getStatus());
        assertNull(absence.getMotiveTitle());
        assertNull(absence.getMotiveDescription());
        verify(absenceRepository).findById(1L);
    }

    @Test
    void unjustifyAbsence_absenceNotFound() {
        when(absenceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> absenceService.unjustifyAbsence(99L));
    }

    // ---------- addAbsence (ticket TFKIES26-87 ) ----------

    @Test
    void addAbsence_basic() {
        when(moduleService.getModuleDetailsForTeacher(10L, "teacher"))
                .thenReturn(moduleWithStudent(10L, 5L));

        User student = new User();
        student.setId(5L);
        Module module = new Module();
        module.setId(10L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(student));
        when(moduleRepository.findById(10L)).thenReturn(Optional.of(module));

        LocalDate date = LocalDate.of(2026, 10, 12);
        absenceService.addAbsence(10L, 5L, date, "teacher");

        ArgumentCaptor<Absence> captor = ArgumentCaptor.forClass(Absence.class);
        verify(absenceRepository).save(captor.capture());
        Absence saved = captor.getValue();
        assertSame(student, saved.getStudent());
        assertSame(module, saved.getModule());
        assertEquals(date, saved.getDate());
        assertEquals(AbsenceStatus.UNJUSTIFIED, saved.getStatus());
    }

    @Test
    void addAbsence_studentNotEnrolled() {
        // The module has no student at all
        when(moduleService.getModuleDetailsForTeacher(10L, "teacher"))
                .thenReturn(new ModuleDetails(10L, "Test fonctionnel", null, List.of(), List.of()));

        assertThrows(BusinessException.class,
                () -> absenceService.addAbsence(10L, 5L, LocalDate.of(2026, 10, 12), "teacher"));

        verify(absenceRepository, never()).save(any());
    }

    @Test
    void addAbsence_teacherHasNoAccess() {
        when(moduleService.getModuleDetailsForTeacher(10L, "intruder"))
                .thenThrow(new BusinessException("Vous n'êtes pas autorisé à accéder à ce module"));

        assertThrows(BusinessException.class,
                () -> absenceService.addAbsence(10L, 5L, LocalDate.of(2026, 10, 12), "intruder"));

        verify(absenceRepository, never()).save(any());
    }

    @Test
    void addAbsence_studentNotFound() {
        when(moduleService.getModuleDetailsForTeacher(10L, "teacher"))
                .thenReturn(moduleWithStudent(10L, 5L));
        when(userRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> absenceService.addAbsence(10L, 5L, LocalDate.of(2026, 10, 12), "teacher"));

        verify(absenceRepository, never()).save(any());
    }

    // ---------- helpers ----------

    private ModuleDetails moduleWithStudent(Long moduleId, Long studentId) {
        return new ModuleDetails(moduleId, "Test fonctionnel", null,
                List.of(new UserSummary(studentId, "Noam", "Faivre", "nfaivre")),
                List.of());
    }

    private Absence createAbsence(Long id, String firstName, String lastName, LocalDate date, AbsenceStatus status,
                                  String motiveTitle, String motiveDescription) {
        User student = new User();
        student.setFirstName(firstName);
        student.setLastName(lastName);

        Absence absence = new Absence();
        absence.setId(id);
        absence.setStudent(student);
        absence.setDate(date);
        absence.setStatus(status);
        absence.setMotiveTitle(motiveTitle);
        absence.setMotiveDescription(motiveDescription);
        return absence;
    }
}
