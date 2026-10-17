package fr.m2.groupe7.kiestla.service;

import fr.m2.groupe7.kiestla.entity.*;
import fr.m2.groupe7.kiestla.entity.Module;
import fr.m2.groupe7.kiestla.repository.ModuleRepository;
import fr.m2.groupe7.kiestla.repository.NotifRepository;
import fr.m2.groupe7.kiestla.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotifRepository notifRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ModuleRepository moduleRepository;

    @InjectMocks
    private NotificationService notificationService;

    private User student;
    private User teacher1;
    private User teacher2;
    private User secretary;
    private Module module;

    @BeforeEach
    void setUp() {
        student = createUser(1L, "John", "Doe", "jdoe", Role.STUDENT);
        teacher1 = createUser(2L, "Alan", "Turing", "aturing", Role.TEACHER);
        teacher2 = createUser(3L, "Ada", "Lovelace", "alovelace", Role.TEACHER);
        secretary = createUser(4L, "Sarah", "Connor", "sconnor", Role.SECRETARY);

        module = new Module();
        module.setId(10L);
        module.setName("Mathematics");
        module.setReferenceTeacher(teacher1);
        module.setEnrolledUsers(Set.of(teacher2, student));
    }

    @Test
    void notifyForUnjustifiedAbsences_createsNotificationsForTeachersAndSecretaries() {
        when(moduleRepository.findWithStudentsById(10L)).thenReturn(Optional.of(module));
        when(userRepository.findByRole(Role.SECRETARY)).thenReturn(List.of(secretary));

        notificationService.notifyForUnjustifiedAbsences(student, module, 3);

        ArgumentCaptor<Notif> captor = ArgumentCaptor.forClass(Notif.class);
        verify(notifRepository, times(3)).save(captor.capture());

        List<Notif> savedNotifs = captor.getAllValues();
        assertEquals(3, savedNotifs.size());

        for (Notif notif : savedNotifs) {
            assertEquals("John Doe a plus de 3 absence injustifié dans le module Mathematics", notif.getMessage());
            assertEquals(NotifStatus.UNREAD, notif.getStatus());
        }

        List<Long> recipientIds = savedNotifs.stream().map(n -> n.getUser().getId()).toList();
        assertTrue(recipientIds.contains(teacher1.getId()));
        assertTrue(recipientIds.contains(teacher2.getId()));
        assertTrue(recipientIds.contains(secretary.getId()));
    }

    @Test
    void getNotificationsForUser_returnsList() {
        Notif notif = new Notif(teacher1, "Message");
        when(userRepository.findByLogin("aturing")).thenReturn(Optional.of(teacher1));
        when(notifRepository.findByUserIdOrderByCreatedAtDesc(teacher1.getId())).thenReturn(List.of(notif));

        List<Notif> result = notificationService.getNotificationsForUser("aturing");
        assertEquals(1, result.size());
        assertEquals("Message", result.get(0).getMessage());
    }

    @Test
    void getUnreadCountForUser_returnsCount() {
        when(userRepository.findByLogin("sconnor")).thenReturn(Optional.of(secretary));
        when(notifRepository.countByUserIdAndStatus(secretary.getId(), NotifStatus.UNREAD)).thenReturn(5L);

        long count = notificationService.getUnreadCountForUser("sconnor");
        assertEquals(5L, count);
    }

    @Test
    void markAllAsRead_marksUnreadAsRead() {
        Notif notif1 = new Notif(teacher1, "Msg 1");
        notif1.setStatus(NotifStatus.UNREAD);

        when(userRepository.findByLogin("aturing")).thenReturn(Optional.of(teacher1));
        when(notifRepository.findByUserIdAndStatus(teacher1.getId(), NotifStatus.UNREAD)).thenReturn(List.of(notif1));

        notificationService.markAllAsRead("aturing");

        assertEquals(NotifStatus.READ, notif1.getStatus());
        verify(notifRepository).save(notif1);
    }

    private User createUser(Long id, String firstName, String lastName, String login, Role role) {
        User user = new User();
        user.setId(id);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setLogin(login);
        user.setRole(role);
        return user;
    }
}
