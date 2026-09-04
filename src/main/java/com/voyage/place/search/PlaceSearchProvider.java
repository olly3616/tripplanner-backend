package com.voyage.place.search;

import java.util.List;

/**
 * Port for external place/map search. A stub implementation ships now; a real
 * adapter (Kakao/Google) can be added later without touching the rest of the app.
 */
public interface PlaceSearchProvider {

    List<PlaceSearchResult> search(String query);
}
