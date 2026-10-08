package com.windfall.api.mypage.dto.purchasehistory;

import com.windfall.domain.trade.enums.TradeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
public class ConfirmedPurchaseHistoryResponse extends BasePurchaseHistory{

  @Schema(description = "리뷰 Id (리뷰가 없을경우 0으로 반환)")
  private final Long reviewId;

  @Builder
  public ConfirmedPurchaseHistoryResponse(TradeStatus status, Long auctionId, Long tradeId,
      Long sellerId,
      String sellername, String sellerProfileImage, String title, String auctionImageUrl,
      int startPrice, int endPrice, int discountPercent, LocalDate purchasedDate, Long roomId,
      int unreadCount, Long reviewId) {
    super(status, auctionId, tradeId, sellerId, sellername, sellerProfileImage, title,
        auctionImageUrl, startPrice, endPrice, discountPercent, purchasedDate, roomId, unreadCount);
    this.reviewId = reviewId;
  }

  public static ConfirmedPurchaseHistoryResponse from(PurchaseHistoryInfo pinfo, ThumbnailImageInfo image, ChatInfoRaw chat, ReviewInfo review){
    return ConfirmedPurchaseHistoryResponse
        .builder()
        .status(pinfo.status())
        .auctionId(pinfo.auctionId())
        .tradeId(pinfo.tradeId())
        .sellerId(pinfo.sellerId())
        .sellername(pinfo.sellerNickname())
        .sellerProfileImage(pinfo.sellerProfileImageURL())
        .title(pinfo.auctionTitle())
        .auctionImageUrl(image.thumbnailImageURL())
        .startPrice(pinfo.startPrice().intValue())
        .endPrice(pinfo.finalPrice().intValue())
        .discountPercent(pinfo.discountPercent().intValue())
        .purchasedDate(pinfo.purchasedDate().toLocalDate())
        .roomId(chat.chatRoomId())
        .unreadCount(chat.unreadCount().intValue())
        .reviewId(review.reviewId())
        .build();
  }

//  public static ConfirmedPurchaseHistoryResponse from(Tuple tuple){
//    return ConfirmedPurchaseHistoryResponse
//        .builder()
//        .status(tuple.get("status", String.class))
//        .auctionId(tuple.get("auctionId", Long.class))
//        .tradeId(tuple.get("tradeId", Long.class))
//        .sellerId(tuple.get("sellerId", Long.class))
//        .sellername(tuple.get("sellername", String.class))
//        .sellerProfileImage(tuple.get("sellerProfileImage", String.class))
//        .title(tuple.get("title", String.class))
//        .auctionImageUrl(tuple.get("auctionImageUrl", String.class))
//        .startPrice(tuple.get("startPrice", Long.class).intValue())
//        .endPrice(tuple.get("endPrice", Long.class).intValue())
//        .discountPercent(tuple.get("discountPercent", BigDecimal.class).intValue())
//        .purchasedDate(tuple.get("purchasedDate", Date.class).toLocalDate())
//        .roomId(tuple.get("roomId", Long.class))
//        .unreadCount(tuple.get("unreadCount", BigDecimal.class).intValue())
//        .reviewId(tuple.get("reviewId", Long.class))
//        .build();
//  }

}
