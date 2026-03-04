package com.anode.logging.filter;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.FilterReply;
import com.anode.logging.EventMarkers;
import org.junit.jupiter.api.Test;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NonEventMarkerFilterTest {

    private final NonEventMarkerFilter filter = new NonEventMarkerFilter();

    @Test
    void deniesEventMarker() {
        ILoggingEvent event = eventWithMarker(EventMarkers.EVENT);
        assertThat(filter.decide(event)).isEqualTo(FilterReply.DENY);
    }

    @Test
    void neutralForNullMarker() {
        ILoggingEvent event = eventWithMarker(null);
        assertThat(filter.decide(event)).isEqualTo(FilterReply.NEUTRAL);
    }

    @Test
    void neutralForUnrelatedMarker() {
        ILoggingEvent event = eventWithMarker(MarkerFactory.getMarker("AUDIT"));
        assertThat(filter.decide(event)).isEqualTo(FilterReply.NEUTRAL);
    }

    @Test
    void deniesNestedEventMarker() {
        Marker parent = MarkerFactory.getMarker("PARENT_" + System.nanoTime());
        parent.add(EventMarkers.EVENT);
        ILoggingEvent event = eventWithMarker(parent);
        assertThat(filter.decide(event)).isEqualTo(FilterReply.DENY);
    }

    private ILoggingEvent eventWithMarker(Marker marker) {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getMarkerList()).thenReturn(marker != null ? List.of(marker) : List.of());
        return event;
    }
}
