package fr.m2.groupe7.kiestla.service;

import fr.m2.groupe7.kiestla.dto.AbsenceSummary;
import fr.m2.groupe7.kiestla.dto.JustifyAbsenceRequest;
import fr.m2.groupe7.kiestla.entity.Absence;
import fr.m2.groupe7.kiestla.entity.AbsenceStatus;
import fr.m2.groupe7.kiestla.exception.ResourceNotFoundException;
import fr.m2.groupe7.kiestla.repository.AbsenceRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AbsenceService {

    private final AbsenceRepository absenceRepository;

    public AbsenceService(AbsenceRepository absenceRepository) {
        this.absenceRepository = absenceRepository;
    }

    /**
     * Retrieves all absences rows
     */
    public List<AbsenceSummary> getAbsences() {
        return absenceRepository.findAllByOrderByDateDesc()
                .stream()
                .map(AbsenceSummary::from)
                .toList();
    }

    /**
     * Retrieves all absences for a module of id id
     */
    public List<AbsenceSummary> getAbsencesForModule(Long id) {
        return absenceRepository.findByModuleId(id)
                .stream()
                .map(AbsenceSummary::from)
                .toList();
    }

    /**
     * Adds a justification to an absence and marks its status as JUSTIFIED.
     */
    @Transactional
    public void justifyAbsence(Long id, JustifyAbsenceRequest justificationReq) {
        Absence absence = absenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Absence introuvable avec l'ID : " + id));

        absence.setJustification(justificationReq.justification());
        absence.setStatus(AbsenceStatus.JUSTIFIED);
    }

    /**
     * Removes the justification from an absence and reverts its status to
     * UNJUSTIFIED.
     */
    @Transactional
    public void unjustifyAbsence(Long id) {
        Absence absence = absenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Absence introuvable avec l'ID : " + id));

        absence.setJustification(null);
        absence.setStatus(AbsenceStatus.UNJUSTIFIED);
    }
}
