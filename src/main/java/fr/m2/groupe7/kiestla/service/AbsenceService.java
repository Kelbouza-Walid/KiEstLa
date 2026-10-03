package fr.m2.groupe7.kiestla.service;

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
     * Retrieves all absences sorted by date in descending order.
     */
    public List<Absence> getAllAbsencesSortedByDateDesc() {
        return absenceRepository.findAll(Sort.by(Sort.Direction.DESC, "date"));
    }

    /**
     * Retrieves all absences sorted by date in descending order.
     */
    public List<Absence> getAbsencesWithID(Long id){
        return absenceRepository.findByModuleId(id);
    }

    /**
     * Adds a justification to an absence and marks its status as JUSTIFIED.
     */
    @Transactional
    public void justifyAbsence(Long id, String justificationText) {
        Absence absence = absenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Absence introuvable avec l'ID : " + id));
        
        absence.setJustification(justificationText);
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
