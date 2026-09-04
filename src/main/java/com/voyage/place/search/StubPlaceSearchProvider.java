package com.voyage.place.search;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Placeholder search provider returning deterministic results so the rest of
 * the places feature (save/filter/dedup/link) can be built and tested without
 * external API keys. Replace with a Kakao/Google adapter later.
 */
@Component
public class StubPlaceSearchProvider implements PlaceSearchProvider {

    private static final String PROVIDER = "STUB";

    @Override
    public List<PlaceSearchResult> search(String query) {
        return List.of(
                new PlaceSearchResult(PROVIDER, "stub-" + query + "-1",
                        query + " 명소", "제주특별자치도 제주시 1", 33.4500, 126.5600, "관광명소"),
                new PlaceSearchResult(PROVIDER, "stub-" + query + "-2",
                        query + " 맛집", "제주특별자치도 제주시 2", 33.4600, 126.5700, "음식점"));
    }
}
