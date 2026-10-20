package fr.m2.groupe7.kiestla.service;

import fr.m2.groupe7.kiestla.entity.Module;
import fr.m2.groupe7.kiestla.entity.Notif;
import fr.m2.groupe7.kiestla.entity.NotifStatus;
import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.exception.ResourceNotFoundException;
import fr.m2.groupe7.kiestla.repository.ModuleRepository;
import fr.m2.groupe7.kiestla.repository.NotifRepository;
import fr.m2.groupe7.kiestla.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class NotificationService {

    private final NotifRepository notifRepository;
    private final UserRepository userRepository;
    private final ModuleRepository moduleRepository;

    public NotificationService(NotifRepository notifRepository, UserRepository userRepository, ModuleRepository moduleRepository) {
        this.notifRepository = notifRepository;
        this.userRepository = userRepository;
        this.moduleRepository = moduleRepository;
    }

    @Transactional(readOnly = true)
    public List<Notif> getNotificationsForUser(String login) {
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + login));
        return notifRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    @Transactional(readOnly = true)
    public long getUnreadCountForUser(String login) {
        User user = userRepository.findByLogin(login).orElse(null);
        if (user == null) {
            return 0;
        }
        return notifRepository.countByUserIdAndStatus(user.getId(), NotifStatus.UNREAD);
    }

    @Transactional
    public void markAllAsRead(String login) {
        User user = userRepository.findByLogin(login).orElse(null);
        if (user == null) {
            return;
        }
        List<Notif> notifs = notifRepository.findByUserIdAndStatus(user.getId(), NotifStatus.UNREAD);
        for (Notif notif : notifs) {
            notif.setStatus(NotifStatus.READ);
            notifRepository.save(notif);
        }
    }

    @Transactional
    public void markAsRead(Long notifId, String login) {
        User user = userRepository.findByLogin(login).orElse(null);
        if (user == null) {
            return;
        }
        Notif notif = notifRepository.findById(notifId).orElse(null);
        if (notif != null && notif.getUser().getId().equals(user.getId())) {
            notif.setStatus(NotifStatus.READ);
            notifRepository.save(notif);
        }
    }

    @Transactional
    public void removeNotificationsForStudentAndModule(Long studentId, Long moduleId) {
        List<Notif> notifs = notifRepository.findByStudentIdAndModuleId(studentId, moduleId);
        if (notifs != null && !notifs.isEmpty()) {
            notifRepository.deleteAll(notifs);
        }
    }

    @Transactional
    public void notifyForUnjustifiedAbsences(User student, Module module, long unjustifiedCount) {
        Module fullModule = moduleRepository.findWithStudentsById(module.getId()).orElse(module);

        String studentName = student.getFirstName() + " " + student.getLastName();
        String message = studentName + " has more than 3 unexcused absences in the module " + fullModule.getName();

        Set<User> recipients = new HashSet<>();
        recipients.addAll(fullModule.getAllTeachers());
        recipients.addAll(userRepository.findByRole(Role.SECRETARY));

        for (User recipient : recipients) {
            boolean alreadyNotified = notifRepository.existsByUserIdAndStudentIdAndModuleIdAndStatus(
                    recipient.getId(), student.getId(), fullModule.getId(), NotifStatus.UNREAD);
            if (!alreadyNotified) {
                notifRepository.save(new Notif(recipient, message, student.getId(), fullModule.getId()));
            }
        }
    }
}
