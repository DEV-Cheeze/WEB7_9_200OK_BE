package com.windfall.api.auction.dto;

import com.windfall.domain.auction.enums.AuctionStatus;

public record PriceChangedAuctionInfo(

    Long auctionId,
    Long oldPrice,
    Long currentPrice,
    AuctionStatus status

) {

}
