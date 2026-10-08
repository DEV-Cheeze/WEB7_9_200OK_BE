package com.windfall.api.user.dto.response.saleshistory;

import com.windfall.domain.trade.enums.TradeStatus;

public record SalesStatusRaw(

    Long auctionId,
    Long tradeId,
    Long currentPrice,
    Long finalPrice,
    TradeStatus tradeStatus

) {

}
