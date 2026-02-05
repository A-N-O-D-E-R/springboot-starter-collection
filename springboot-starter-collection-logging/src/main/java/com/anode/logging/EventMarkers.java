package com.anode.logging;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * Markers for structured event logging.
 * Use {@link #EVENT} to tag log statements that should be serialized as JSON events.
 *
 * <pre>{@code
 * log.info(EventMarkers.EVENT, "{}", eventObject);
 * }</pre>
 */
public final class EventMarkers {

    /**
     * Marker for structured event logging.
     * Logs with this marker will be serialized to JSON and written to the events file.
     */
    public static final Marker EVENT = MarkerFactory.getMarker("EVENT");

    private EventMarkers() {
    }
}
