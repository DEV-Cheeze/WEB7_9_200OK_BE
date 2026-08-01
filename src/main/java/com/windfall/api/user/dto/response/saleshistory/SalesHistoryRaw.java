package com.windfall.api.user.dto.response.saleshistory;

import com.windfall.domain.auction.enums.AuctionStatus;
import java.time.LocalDateTime;

public record SalesHistoryRaw(
    Long auctionId,
    AuctionStatus status,
    String title,
    Long startPrice,
    LocalDateTime startedAt
) {

}
