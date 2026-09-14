package com.windfall.api.auction.dto;

import java.util.List;

public record AuctionPriceChangeResult(
    List<PriceChangedAuctionInfo> changes,
    int failed,
    int decreased
) {

}
