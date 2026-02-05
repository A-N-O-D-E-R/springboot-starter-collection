package com.anode.logging.filter;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import com.anode.logging.EventMarkers;
import org.slf4j.Marker;

/**
 * Logback filter that accepts only log events marked with {@link EventMarkers#EVENT}.
 * All other events are denied.
 */
public class EventMarkerFilter extends Filter<ILoggingEvent> {

    private static final String EVENT_MARKER_NAME = "EVENT";

    @Override
    public FilterReply decide(ILoggingEvent event) {
        Marker marker = event.getMarker();
        if (marker == null) {
            return FilterReply.DENY;
        }
        if (containsEventMarker(marker)) {
            return FilterReply.ACCEPT;
        }
        return FilterReply.DENY;
    }

    private boolean containsEventMarker(Marker marker) {
        if (EVENT_MARKER_NAME.equals(marker.getName())) {
            return true;
        }
        if (marker.hasReferences()) {
            for (var it = marker.iterator(); it.hasNext(); ) {
                if (containsEventMarker(it.next())) {
                    return true;
                }
            }
        }
        return false;
    }
}
