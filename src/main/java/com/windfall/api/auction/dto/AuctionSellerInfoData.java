package com.windfall.api.auction.dto;

import com.windfall.api.auction.dto.response.info.BuyerReviewInfo;
import com.windfall.api.auction.dto.response.raw.SellerAuctionsRaw;
import com.windfall.api.auction.dto.response.stats.SellerReviewStats;
import com.windfall.api.user.dto.response.reviewlist.AuctionImageRaw;
import com.windfall.domain.user.entity.User;
import java.util.List;

public record AuctionSellerInfoData(

    User seller,
    List<SellerAuctionsRaw> sellerAuctionsRaws,
    List<AuctionImageRaw> auctionImages,
    List<BuyerReviewInfo> buyerReviewInfo,
    SellerReviewStats reviewStats

) {

}
