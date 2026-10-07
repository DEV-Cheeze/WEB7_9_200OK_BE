package com.windfall.api.notification.event.vo;

import com.windfall.domain.auction.enums.AuctionStatus;

public record PriceDroppedBroadcastEvent(
    Long auctionId,
    Long currentPrice,
    AuctionStatus status

) {

}
