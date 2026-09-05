package com.voyage.ai.provider;

import com.voyage.ai.dto.ItineraryDraftResponse;

/**
 * Port for AI-generated itinerary drafts. A deterministic stub ships now; a real
 * LLM-backed adapter can replace it later. Results are suggestions only — never
 * auto-committed to the itinerary.
 */
public interface AiItineraryProvider {

    ItineraryDraftResponse suggest(DraftContext context);
}
