package com.windfall.api.mypage.dto.purchasehistory;

import com.windfall.api.user.dto.response.saleshistory.ProcessingSalesHistoryResponse;
import com.windfall.domain.trade.enums.TradeStatus;
import jakarta.persistence.Tuple;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
public class PurchaseHistoryResponse extends BasePurchaseHistory{

  @Builder
  public PurchaseHistoryResponse(TradeStatus status, Long auctionId, Long tradeId, Long sellerId,
      String sellername, String sellerProfileImage, String title, String auctionImageUrl,
      int startPrice, int endPrice, int discountPercent, LocalDate purchasedDate, Long roomId,
      int unreadCount) {
    super(status, auctionId, tradeId, sellerId, sellername, sellerProfileImage, title,
        auctionImageUrl, startPrice, endPrice, discountPercent, purchasedDate, roomId, unreadCount);
  }
  public static PurchaseHistoryResponse from(PurchaseHistoryInfo pinfo, ThumbnailImageInfo image, ChatInfo chat){
    return PurchaseHistoryResponse
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
        .build();
  }

//  public static PurchaseHistoryResponse from(Tuple tuple){
//    return PurchaseHistoryResponse
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
//        .build();
//  }
}
