package com.windfall.api.auction.dto;

import com.windfall.api.auction.dto.response.AuctionSellerInfoResponse;
import com.windfall.global.redis.enums.CacheDataStatus;

public record AuctionSellerInfoCachedData(

    CacheDataStatus status,
    AuctionSellerInfoResponse response

) {

}
