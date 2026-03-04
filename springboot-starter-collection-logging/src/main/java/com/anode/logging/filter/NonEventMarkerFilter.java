package com.anode.logging.filter;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.Marker;

import java.util.List;

/**
 * Logback filter that denies log events marked with EVENT marker.
 * Use this filter on console/file appenders to prevent duplicate logging of events.
 */
public class NonEventMarkerFilter extends Filter<ILoggingEvent> {

    private static final String EVENT_MARKER_NAME = "EVENT";

    @Override
    public FilterReply decide(ILoggingEvent event) {
        List<Marker> markers = event.getMarkerList();
        if (markers == null || markers.isEmpty()) {
            return FilterReply.NEUTRAL;
        }
        for (Marker marker : markers) {
            if (containsEventMarker(marker)) {
                return FilterReply.DENY;
            }
        }
        return FilterReply.NEUTRAL;
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
