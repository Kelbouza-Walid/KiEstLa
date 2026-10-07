package fr.m2.groupe7.kiestla.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.m2.groupe7.kiestla.dto.AbsenceSummary;
import fr.m2.groupe7.kiestla.dto.JustifyAbsenceRequest;
import fr.m2.groupe7.kiestla.entity.Absence;
import fr.m2.groupe7.kiestla.entity.AbsenceStatus;
import fr.m2.groupe7.kiestla.entity.User;
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

    @InjectMocks
    private AbsenceService absenceService;

    @Test
    void getAbsences_basic() {
        Absence absence = createAbsence(1L, "Noam", "Faivre", LocalDate.of(2026, 10, 1), AbsenceStatus.UNJUSTIFIED,
                null);
        when(absenceRepository.findAllByOrderByDateDesc()).thenReturn(List.of(absence));

        List<AbsenceSummary> summaries = absenceService.getAbsences();

        assertEquals(1, summaries.size());
        assertEquals(1L, summaries.get(0).id());
        assertEquals("Noam Faivre", summaries.get(0).studentName());
        assertEquals(LocalDate.of(2026, 10, 1), summaries.get(0).date());
        assertEquals(AbsenceStatus.UNJUSTIFIED, summaries.get(0).status());
        assertNull(summaries.get(0).justification());
        verify(absenceRepository).findAllByOrderByDateDesc();
    }

    @Test
    void getAbsencesForModule_basic() {
        Absence absence = createAbsence(2L, "Suleyman", "Kacmaz", LocalDate.of(2026, 10, 2), AbsenceStatus.JUSTIFIED,
                "Certificat médical");
        when(absenceRepository.findByModuleId(10L)).thenReturn(List.of(absence));

        List<AbsenceSummary> summaries = absenceService.getAbsencesForModule(10L);

        assertEquals(1, summaries.size());
        assertEquals(2L, summaries.get(0).id());
        assertEquals("Suleyman Kacmaz", summaries.get(0).studentName());
        assertEquals(AbsenceStatus.JUSTIFIED, summaries.get(0).status());
        assertEquals("Certificat médical", summaries.get(0).justification());
        verify(absenceRepository).findByModuleId(10L);
    }

    @Test
    void justifyAbsence_basic() {
        Absence absence = createAbsence(1L, "Mathis", "Regazzoni", LocalDate.of(2026, 10, 1), AbsenceStatus.UNJUSTIFIED,
                null);
        when(absenceRepository.findById(1L)).thenReturn(Optional.of(absence));

        JustifyAbsenceRequest request = new JustifyAbsenceRequest("Rendez-vous médical");
        absenceService.justifyAbsence(1L, request);

        assertEquals(AbsenceStatus.JUSTIFIED, absence.getStatus());
        assertEquals("Rendez-vous médical", absence.getJustification());
        verify(absenceRepository).findById(1L);
    }

    @Test
    void justifyAbsence_absenceNotFound() {
        when(absenceRepository.findById(99L)).thenReturn(Optional.empty());

        JustifyAbsenceRequest request = new JustifyAbsenceRequest("Raison");

        assertThrows(ResourceNotFoundException.class, () -> absenceService.justifyAbsence(99L, request));
    }

    @Test
    void unjustifyAbsence_basic() {
        Absence absence = createAbsence(1L, "Gaspard", "Quentin", LocalDate.of(2026, 10, 1), AbsenceStatus.JUSTIFIED,
                "Raison passée");
        when(absenceRepository.findById(1L)).thenReturn(Optional.of(absence));

        absenceService.unjustifyAbsence(1L);

        assertEquals(AbsenceStatus.UNJUSTIFIED, absence.getStatus());
        assertNull(absence.getJustification());
        verify(absenceRepository).findById(1L);
    }

    @Test
    void unjustifyAbsence_absenceNotFound() {
        when(absenceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> absenceService.unjustifyAbsence(99L));
    }

    private Absence createAbsence(Long id, String firstName, String lastName, LocalDate date, AbsenceStatus status,
            String justification) {
        User student = new User();
        student.setFirstName(firstName);
        student.setLastName(lastName);

        Absence absence = new Absence();
        absence.setId(id);
        absence.setStudent(student);
        absence.setDate(date);
        absence.setStatus(status);
        absence.setJustification(justification);
        return absence;
    }
}
