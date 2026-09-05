package com.voyage.activity;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.voyage.activity.domain.ActivityLog;
import com.voyage.activity.domain.Notification;
import com.voyage.activity.event.ActivityEventListener;
import com.voyage.activity.event.TripActivityEvent;
import com.voyage.activity.repository.ActivityLogRepository;
import com.voyage.activity.repository.NotificationRepository;
import com.voyage.trip.domain.MemberStatus;
import com.voyage.trip.domain.TripMember;
import com.voyage.trip.domain.TripRole;
import com.voyage.trip.repository.TripMemberRepository;
import com.voyage.user.domain.User;
import com.voyage.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActivityEventListenerTest {

    @Mock
    private ActivityLogRepository activityLogRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private TripMemberRepository tripMemberRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private ActivityEventListener listener;

    @Test
    void notifiesEveryMemberExceptActor_andLogsOnce() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(
                User.createEmailUser("minji@voyage.com", "h", "민지", null, null)));
        when(tripMemberRepository.findByTripIdAndStatus(10L, MemberStatus.ACTIVE)).thenReturn(List.of(
                TripMember.owner(10L, 1L),
                TripMember.member(10L, 2L, TripRole.EDITOR),
                TripMember.member(10L, 3L, TripRole.VIEWER)));

        listener.on(new TripActivityEvent(10L, 1L, TripActivityEvent.EXPENSE_CREATED, "EXPENSE", 5L, "숙소비"));

        verify(activityLogRepository, times(1)).save(any(ActivityLog.class));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(captor.capture());
        // actor (1) is excluded; 2 and 3 are notified
        org.junit.jupiter.api.Assertions.assertEquals(
                List.of(2L, 3L),
                captor.getAllValues().stream().map(Notification::getUserId).sorted().toList());
        org.junit.jupiter.api.Assertions.assertTrue(
                captor.getAllValues().get(0).getMessage().contains("민지"));
    }

    @Test
    void soloTrip_logsButNotifiesNobody() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(
                User.createEmailUser("solo@voyage.com", "h", "혼자", null, null)));
        when(tripMemberRepository.findByTripIdAndStatus(10L, MemberStatus.ACTIVE))
                .thenReturn(List.of(TripMember.owner(10L, 1L)));

        listener.on(new TripActivityEvent(10L, 1L, TripActivityEvent.POLL_CREATED, "POLL", 7L, "저녁 투표"));

        verify(activityLogRepository).save(any());
        verify(notificationRepository, never()).save(any());
    }
}
