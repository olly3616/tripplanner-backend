package com.voyage.activity.event;

import com.voyage.activity.domain.ActivityLog;
import com.voyage.activity.domain.Notification;
import com.voyage.activity.repository.ActivityLogRepository;
import com.voyage.activity.repository.NotificationRepository;
import com.voyage.trip.domain.MemberStatus;
import com.voyage.trip.domain.TripMember;
import com.voyage.trip.repository.TripMemberRepository;
import com.voyage.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Writes the activity feed entry and fans out notifications to every active trip
 * member except the actor. Runs synchronously inside the publishing transaction,
 * so the feed/notifications commit atomically with the change that triggered them.
 */
@Component
@RequiredArgsConstructor
public class ActivityEventListener {

    private final ActivityLogRepository activityLogRepository;
    private final NotificationRepository notificationRepository;
    private final TripMemberRepository tripMemberRepository;
    private final UserRepository userRepository;

    @EventListener
    public void on(TripActivityEvent event) {
        String actorName = userRepository.findById(event.actorId())
                .map(u -> u.getName()).orElse("멤버");
        String message = buildMessage(event.action(), actorName, event.title());

        activityLogRepository.save(ActivityLog.of(event.tripId(), event.actorId(),
                event.action(), event.entityType(), event.entityId(), message));

        for (TripMember member : tripMemberRepository.findByTripIdAndStatus(event.tripId(), MemberStatus.ACTIVE)) {
            if (!member.getUserId().equals(event.actorId())) {
                notificationRepository.save(Notification.of(
                        member.getUserId(), event.action(), event.tripId(), message));
            }
        }
    }

    private String buildMessage(String action, String actorName, String title) {
        return switch (action) {
            case TripActivityEvent.EXPENSE_CREATED -> "%s님이 '%s' 지출을 추가했어요".formatted(actorName, title);
            case TripActivityEvent.POLL_CREATED -> "%s님이 투표 '%s'을(를) 만들었어요".formatted(actorName, title);
            default -> "%s님이 활동했어요".formatted(actorName);
        };
    }
}
