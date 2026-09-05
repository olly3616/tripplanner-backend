package com.voyage.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.voyage.ai.dto.ItineraryDraftResponse;
import com.voyage.ai.provider.DraftContext;
import com.voyage.ai.provider.DraftContext.PlaceCandidate;
import com.voyage.ai.provider.StubAiItineraryProvider;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class StubAiItineraryProviderTest {

    private final StubAiItineraryProvider provider = new StubAiItineraryProvider();

    @Test
    void spreadsPlacesAcrossDays() {
        List<PlaceCandidate> places = IntStream.range(0, 5)
                .mapToObj(i -> new PlaceCandidate((long) i, "place" + i, "cafe", "WISH"))
                .toList();
        DraftContext ctx = new DraftContext(
                LocalDate.of(2026, 8, 14), LocalDate.of(2026, 8, 16), places, 2, null);

        ItineraryDraftResponse draft = provider.suggest(ctx);

        assertEquals(3, draft.days().size());
        assertEquals(2, draft.days().get(0).items().size());   // p0, p1
        assertEquals(2, draft.days().get(1).items().size());   // p2, p3
        assertEquals(1, draft.days().get(2).items().size());   // p4
        assertEquals(LocalTime.of(10, 0), draft.days().get(0).items().get(0).startsAt());
        assertEquals("place0", draft.days().get(0).items().get(0).placeName());
    }

    @Test
    void filtersByPreferredCategory() {
        List<PlaceCandidate> places = List.of(
                new PlaceCandidate(1L, "cafe A", "cafe", "WISH"),
                new PlaceCandidate(2L, "food B", "food", "WISH"));
        DraftContext ctx = new DraftContext(
                LocalDate.of(2026, 8, 14), LocalDate.of(2026, 8, 14), places, 3, List.of("food"));

        ItineraryDraftResponse draft = provider.suggest(ctx);

        assertEquals(1, draft.days().size());
        assertEquals(1, draft.days().get(0).items().size());
        assertEquals("food B", draft.days().get(0).items().get(0).placeName());
    }
}
