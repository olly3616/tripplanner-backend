package com.voyage.share.service;

import com.voyage.expense.domain.Expense;
import com.voyage.expense.repository.ExpenseRepository;
import com.voyage.global.exception.BusinessException;
import com.voyage.global.exception.ErrorCode;
import com.voyage.global.util.SecureTokens;
import com.voyage.itinerary.domain.ItineraryItem;
import com.voyage.itinerary.repository.ItineraryItemRepository;
import com.voyage.place.domain.SavedPlace;
import com.voyage.place.repository.SavedPlaceRepository;
import com.voyage.share.domain.ShareLink;
import com.voyage.share.dto.CreateShareLinkRequest;
import com.voyage.share.dto.PublicTripSummary;
import com.voyage.share.dto.ShareLinkResponse;
import com.voyage.share.repository.ShareLinkRepository;
import com.voyage.trip.domain.Trip;
import com.voyage.trip.repository.TripRepository;
import com.voyage.trip.service.TripAccessGuard;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ShareService {

    private static final String UNCATEGORIZED = "기타";

    private final ShareLinkRepository shareLinkRepository;
    private final TripAccessGuard tripAccessGuard;
    private final PasswordEncoder passwordEncoder;
    private final TripRepository tripRepository;
    private final ItineraryItemRepository itineraryItemRepository;
    private final SavedPlaceRepository savedPlaceRepository;
    private final ExpenseRepository expenseRepository;

    @Transactional
    public ShareLinkResponse create(Long userId, Long tripId, CreateShareLinkRequest request) {
        tripAccessGuard.requireOwner(tripId, userId);
        String rawToken = SecureTokens.newToken();
        String passwordHash = StringUtils.hasText(request.password())
                ? passwordEncoder.encode(request.password()) : null;
        ShareLink link = shareLinkRepository.save(ShareLink.create(
                tripId, SecureTokens.sha256Hex(rawToken), passwordHash,
                request.expiresAt(), request.includeExpensesOrDefault()));
        return ShareLinkResponse.created(link, rawToken);
    }

    @Transactional(readOnly = true)
    public List<ShareLinkResponse> list(Long userId, Long tripId) {
        tripAccessGuard.requireOwner(tripId, userId);
        return shareLinkRepository.findByTripIdOrderByCreatedAtDesc(tripId).stream()
                .map(ShareLinkResponse::summary)
                .toList();
    }

    @Transactional
    public void revoke(Long userId, Long tripId, Long linkId) {
        tripAccessGuard.requireOwner(tripId, userId);
        ShareLink link = shareLinkRepository.findByIdAndTripId(linkId, tripId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        shareLinkRepository.delete(link);
    }

    @Transactional(readOnly = true)
    public PublicTripSummary getPublicSummary(String rawToken, String password) {
        ShareLink link = shareLinkRepository.findByTokenHash(SecureTokens.sha256Hex(rawToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.SHARE_LINK_INVALID));
        if (link.isExpired(Instant.now())) {
            throw new BusinessException(ErrorCode.SHARE_LINK_INVALID);
        }
        if (link.hasPassword()
                && (!StringUtils.hasText(password) || !passwordEncoder.matches(password, link.getPasswordHash()))) {
            throw new BusinessException(ErrorCode.SHARE_PASSWORD_INVALID);
        }
        return buildSummary(link);
    }

    private PublicTripSummary buildSummary(ShareLink link) {
        Long tripId = link.getTripId();
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHARE_LINK_INVALID));

        List<SavedPlace> places = savedPlaceRepository.findByTripIdOrderByCreatedAtDesc(tripId);
        Map<Long, String> placeNames = places.stream()
                .collect(Collectors.toMap(SavedPlace::getId, SavedPlace::getName));

        List<PublicTripSummary.ItineraryEntry> itinerary =
                itineraryItemRepository.findByTripIdOrderByDateAscSortOrderAsc(tripId).stream()
                        .map(item -> toItineraryEntry(item, placeNames))
                        .toList();

        List<PublicTripSummary.PlaceEntry> placeEntries = places.stream()
                .map(p -> new PublicTripSummary.PlaceEntry(
                        p.getName(), p.getAddress(), p.getCategory(), p.getStatus().name()))
                .toList();

        PublicTripSummary.Budget budget = link.isIncludeExpenses() ? buildBudget(trip) : null;

        return new PublicTripSummary(trip.getTitle(), trip.getDestination(), trip.getStartsOn(),
                trip.getEndsOn(), trip.getTimezone(), trip.getStatus(), itinerary, placeEntries, budget);
    }

    private PublicTripSummary.ItineraryEntry toItineraryEntry(ItineraryItem item, Map<Long, String> placeNames) {
        String placeName = item.getPlaceId() != null ? placeNames.get(item.getPlaceId()) : null;
        return new PublicTripSummary.ItineraryEntry(
                item.getDate(), item.getStartsAt(), item.getEndsAt(), placeName, item.getNote());
    }

    private PublicTripSummary.Budget buildBudget(Trip trip) {
        Map<String, Long> categoryTotals = new TreeMap<>();
        long total = 0;
        for (Expense expense : expenseRepository.findByTripIdWithSplits(trip.getId())) {
            total += expense.getBaseAmountMinor();
            String category = expense.getCategory() != null ? expense.getCategory() : UNCATEGORIZED;
            categoryTotals.merge(category, expense.getBaseAmountMinor(), Long::sum);
        }
        List<PublicTripSummary.CategoryTotal> categories = categoryTotals.entrySet().stream()
                .map(e -> new PublicTripSummary.CategoryTotal(e.getKey(), e.getValue()))
                .toList();
        return new PublicTripSummary.Budget(trip.getBaseCurrency(), total, categories);
    }
}
