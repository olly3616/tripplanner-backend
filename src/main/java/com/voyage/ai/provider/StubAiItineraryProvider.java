package com.voyage.ai.provider;

import com.voyage.ai.dto.ItineraryDraftResponse;
import com.voyage.ai.dto.ItineraryDraftResponse.DraftDay;
import com.voyage.ai.dto.ItineraryDraftResponse.DraftItem;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Deterministic placeholder: spreads the saved places across the trip's days,
 * a few per day at sensible time slots, so the feature works end-to-end without
 * an LLM. Replace with a real provider later.
 */
@Component
public class StubAiItineraryProvider implements AiItineraryProvider {

    private static final LocalTime[] SLOTS = {
            LocalTime.of(10, 0), LocalTime.of(12, 30), LocalTime.of(15, 0),
            LocalTime.of(18, 0), LocalTime.of(20, 0)
    };

    @Override
    public ItineraryDraftResponse suggest(DraftContext context) {
        List<DraftContext.PlaceCandidate> pool = context.places().stream()
                .filter(p -> context.preferredCategories() == null
                        || context.preferredCategories().isEmpty()
                        || context.preferredCategories().contains(p.category()))
                .toList();

        List<DraftDay> days = new ArrayList<>();
        int index = 0;
        for (LocalDate date = context.startsOn(); !date.isAfter(context.endsOn()); date = date.plusDays(1)) {
            List<DraftItem> items = new ArrayList<>();
            for (int slot = 0; slot < context.itemsPerDay() && index < pool.size(); slot++, index++) {
                DraftContext.PlaceCandidate place = pool.get(index);
                LocalTime time = SLOTS[Math.min(slot, SLOTS.length - 1)];
                items.add(new DraftItem(place.placeId(), place.name(), time, "AI 추천"));
            }
            days.add(new DraftDay(date, items));
        }
        return new ItineraryDraftResponse(days);
    }
}
