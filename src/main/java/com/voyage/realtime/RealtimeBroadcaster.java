package com.voyage.realtime;

import com.voyage.activity.event.TripActivityEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Pushes activity to a trip's WebSocket subscribers <b>after</b> the originating
 * transaction commits, so clients never see an event for a change that rolled back.
 */
@Component
@RequiredArgsConstructor
public class RealtimeBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(TripActivityEvent event) {
        messagingTemplate.convertAndSend(topic(event.tripId()), RealtimeEvent.from(event));
    }

    public static String topic(Long tripId) {
        return "/topic/trips/" + tripId;
    }
}
