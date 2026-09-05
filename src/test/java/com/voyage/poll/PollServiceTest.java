package com.voyage.poll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.voyage.global.exception.BusinessException;
import com.voyage.global.exception.ErrorCode;
import com.voyage.poll.domain.Poll;
import com.voyage.poll.domain.PollOption;
import com.voyage.poll.dto.CreatePollRequest;
import com.voyage.poll.dto.PollResponse;
import com.voyage.poll.dto.VoteRequest;
import com.voyage.poll.repository.PollRepository;
import com.voyage.poll.repository.VoteRepository;
import com.voyage.poll.service.PollService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PollServiceTest {

    @Mock
    private PollRepository pollRepository;
    @Mock
    private VoteRepository voteRepository;
    @Mock
    private com.voyage.trip.service.TripAccessGuard tripAccessGuard;
    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;
    @InjectMocks
    private PollService pollService;

    @Test
    void create_tooFewOptions_throws() {
        CreatePollRequest request = new CreatePollRequest("첫날 저녁?", false, false,
                Instant.now().plus(1, ChronoUnit.DAYS), List.of("흑돼지"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pollService.create(7L, 1L, request));
        assertEquals(ErrorCode.INVALID_POLL, ex.getErrorCode());
    }

    @Test
    void create_success_returnsOptionsWithNoVotes() {
        when(pollRepository.save(any(Poll.class))).thenAnswer(inv -> inv.getArgument(0));
        when(voteRepository.findByPollId(any())).thenReturn(List.of());
        CreatePollRequest request = new CreatePollRequest("첫날 저녁?", false, false,
                Instant.now().plus(1, ChronoUnit.DAYS), List.of("흑돼지", "해산물"));

        PollResponse response = pollService.create(7L, 1L, request);

        assertEquals(2, response.options().size());
        assertEquals(0, response.totalVoters());
    }

    @Test
    void vote_closedPoll_throws() {
        Poll poll = Poll.create(1L, 7L, "마감된 투표", false, false,
                Instant.now().minus(1, ChronoUnit.HOURS), List.of(PollOption.of("A", 0), PollOption.of("B", 1)));
        when(pollRepository.findByIdWithOptions(5L)).thenReturn(Optional.of(poll));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pollService.vote(7L, 5L, new VoteRequest(List.of(1L))));
        assertEquals(ErrorCode.POLL_CLOSED, ex.getErrorCode());
    }
}
