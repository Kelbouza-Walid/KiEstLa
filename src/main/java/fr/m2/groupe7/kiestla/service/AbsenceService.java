package fr.m2.groupe7.kiestla.service;

import fr.m2.groupe7.kiestla.entity.Absence;
import fr.m2.groupe7.kiestla.entity.AbsenceStatus;
import fr.m2.groupe7.kiestla.repository.AbsenceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AbsenceService {

    @Autowired
    private AbsenceRepository absenceRepository;

    /**
     * Retrieves all absences sorted by date in descending order.
     */
    public List<Absence> getAllAbsencesSortedByDateDesc() {
        return absenceRepository.findAll(Sort.by(Sort.Direction.DESC, "date"));
    }

    /**
     * Adds a justification to an absence and marks its status as JUSTIFIED.
     */
    public void justifyAbsence(Long id, String justificationText) {
        Optional<Absence> absenceOpt = absenceRepository.findById(id);
        if (absenceOpt.isPresent()) {
            Absence absence = absenceOpt.get();
            absence.setJustification(justificationText);
            absence.setStatus(AbsenceStatus.JUSTIFIED);
            absenceRepository.save(absence);
        }
    }

    /**
     * Removes the justification from an absence and reverts its status to
     * UNJUSTIFIED.
     */
    public void unjustifyAbsence(Long id) {
        Optional<Absence> absenceOpt = absenceRepository.findById(id);
        if (absenceOpt.isPresent()) {
            Absence absence = absenceOpt.get();
            absence.setJustification(null);
            absence.setStatus(AbsenceStatus.UNJUSTIFIED);
            absenceRepository.save(absence);
        }
    }
}
