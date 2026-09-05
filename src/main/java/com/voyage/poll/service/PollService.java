package com.voyage.poll.service;

import com.voyage.global.exception.BusinessException;
import com.voyage.global.exception.ErrorCode;
import com.voyage.poll.domain.Poll;
import com.voyage.poll.domain.PollOption;
import com.voyage.poll.domain.Vote;
import com.voyage.poll.dto.CreatePollRequest;
import com.voyage.poll.dto.PollResponse;
import com.voyage.poll.dto.VoteRequest;
import com.voyage.poll.repository.PollRepository;
import com.voyage.poll.repository.VoteRepository;
import com.voyage.trip.domain.TripRole;
import com.voyage.trip.service.TripAccessGuard;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PollService {

    private final PollRepository pollRepository;
    private final VoteRepository voteRepository;
    private final TripAccessGuard tripAccessGuard;

    @Transactional
    public PollResponse create(Long userId, Long tripId, CreatePollRequest request) {
        tripAccessGuard.requireAnyRole(tripId, userId, TripRole.OWNER, TripRole.EDITOR);
        if (request.options().size() < 2) {
            throw new BusinessException(ErrorCode.INVALID_POLL, "선택지는 2개 이상이어야 합니다.");
        }
        List<PollOption> options = IntStream.range(0, request.options().size())
                .mapToObj(i -> PollOption.of(request.options().get(i), i))
                .toList();
        Poll poll = pollRepository.save(Poll.create(tripId, userId, request.title(),
                request.multipleChoice(), request.anonymous(), request.closesAt(), options));
        return toResponse(poll, userId);
    }

    @Transactional(readOnly = true)
    public List<PollResponse> list(Long userId, Long tripId) {
        tripAccessGuard.requireActiveMember(tripId, userId);
        return pollRepository.findByTripIdOrderByCreatedAtDesc(tripId).stream()
                .map(poll -> toResponse(poll, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public PollResponse get(Long userId, Long pollId) {
        Poll poll = findPoll(pollId);
        tripAccessGuard.requireActiveMember(poll.getTripId(), userId);
        return toResponse(poll, userId);
    }

    @Transactional
    public PollResponse vote(Long userId, Long pollId, VoteRequest request) {
        Poll poll = findPoll(pollId);
        tripAccessGuard.requireActiveMember(poll.getTripId(), userId);
        if (poll.isClosed(Instant.now())) {
            throw new BusinessException(ErrorCode.POLL_CLOSED);
        }
        List<Long> optionIds = request.optionIds().stream().distinct().toList();
        Set<Long> pollOptionIds = poll.getOptions().stream().map(PollOption::getId).collect(java.util.stream.Collectors.toSet());
        if (!pollOptionIds.containsAll(optionIds)) {
            throw new BusinessException(ErrorCode.INVALID_VOTE, "선택지가 이 투표에 속하지 않습니다.");
        }
        if (!poll.isMultipleChoice() && optionIds.size() != 1) {
            throw new BusinessException(ErrorCode.INVALID_VOTE, "단일 선택 투표입니다.");
        }
        // Replace the user's previous votes for this poll.
        voteRepository.deleteAll(voteRepository.findByPollIdAndUserId(pollId, userId));
        voteRepository.flush();
        voteRepository.saveAll(optionIds.stream().map(oid -> Vote.of(pollId, oid, userId)).toList());
        return toResponse(poll, userId);
    }

    @Transactional
    public void delete(Long userId, Long pollId) {
        Poll poll = findPoll(pollId);
        tripAccessGuard.requireAnyRole(poll.getTripId(), userId, TripRole.OWNER, TripRole.EDITOR);
        voteRepository.deleteByPollId(pollId);
        pollRepository.delete(poll);
    }

    private Poll findPoll(Long pollId) {
        return pollRepository.findByIdWithOptions(pollId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    private PollResponse toResponse(Poll poll, Long userId) {
        List<Vote> votes = voteRepository.findByPollId(poll.getId());
        Map<Long, List<Long>> votersByOption = new LinkedHashMap<>();
        Set<Long> voters = new java.util.HashSet<>();
        List<Long> myOptionIds = new ArrayList<>();
        for (Vote vote : votes) {
            votersByOption.computeIfAbsent(vote.getOptionId(), k -> new ArrayList<>()).add(vote.getUserId());
            voters.add(vote.getUserId());
            if (vote.getUserId().equals(userId)) {
                myOptionIds.add(vote.getOptionId());
            }
        }

        List<PollResponse.OptionResult> options = poll.getOptions().stream()
                .sorted(Comparator.comparingInt(PollOption::getSortOrder))
                .map(option -> {
                    List<Long> voterIds = votersByOption.getOrDefault(option.getId(), List.of());
                    return new PollResponse.OptionResult(
                            option.getId(), option.getLabel(), voterIds.size(),
                            poll.isAnonymous() ? null : List.copyOf(voterIds));
                })
                .toList();

        return new PollResponse(
                poll.getId(), poll.getTripId(), poll.getCreatedBy(), poll.getTitle(),
                poll.isMultipleChoice(), poll.isAnonymous(), poll.getClosesAt(),
                poll.isClosed(Instant.now()), options, voters.size(), myOptionIds);
    }
}
