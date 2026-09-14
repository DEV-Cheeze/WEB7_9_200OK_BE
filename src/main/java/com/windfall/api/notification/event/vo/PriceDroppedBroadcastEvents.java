package com.windfall.api.notification.event.vo;

import java.util.List;

public record PriceDroppedBroadcastEvents(
    List<PriceDroppedBroadcastEvent> events
) {

}
