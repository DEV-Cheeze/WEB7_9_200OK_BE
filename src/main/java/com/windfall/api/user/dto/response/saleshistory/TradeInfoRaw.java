package com.windfall.api.user.dto.response.saleshistory;

import com.windfall.domain.trade.enums.TradeStatus;

public record TradeInfoRaw(
    Long auctionId,
    Long tradeId,
    Long endPrice,
    TradeStatus status
) {

}
