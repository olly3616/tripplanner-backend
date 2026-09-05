package com.voyage.realtime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.voyage.activity.event.TripActivityEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class RealtimeBroadcasterTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @InjectMocks
    private RealtimeBroadcaster broadcaster;

    @Test
    void broadcastsEventToTripTopic() {
        broadcaster.on(new TripActivityEvent(10L, 1L, TripActivityEvent.EXPENSE_CREATED, "EXPENSE", 5L, "숙소비"));

        verify(messagingTemplate).convertAndSend(eq("/topic/trips/10"), any(RealtimeEvent.class));
    }
}
