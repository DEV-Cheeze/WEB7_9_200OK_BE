package com.windfall.api.mypage.dto.purchasehistory;

import com.windfall.domain.trade.enums.TradeStatus;
import java.time.LocalDateTime;

public record PurchaseHistoryInfo(
    Long auctionId,
    Long tradeId,
    TradeStatus status,
    Long sellerId,
    String sellerNickname,
    String sellerProfileImageURL,
    String auctionTitle,
    Long startPrice,
    Long finalPrice,
    Long discountPercent,
    LocalDateTime purchasedDate
) {

}
