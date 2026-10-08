package com.windfall.api.user.dto.response.saleshistory;

import com.windfall.domain.auction.enums.AuctionStatus;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record SalesHistoryResponse(
    AuctionStatus status,
    Long auctionId,
    String title,
    String auctionImageUrl,
    int startPrice,
    LocalDateTime startedAt,
    SalesStatusDetail statusDetail
)
{

  public static SalesHistoryResponse from(SalesHistoryRaw salesHistoryRaw, String auctionImageUrl, SalesStatusDetail details){
    return SalesHistoryResponse.builder()
        .status(salesHistoryRaw.status())
        .auctionId(salesHistoryRaw.auctionId())
        .title(salesHistoryRaw.title())
        .auctionImageUrl(auctionImageUrl)
        .startPrice(salesHistoryRaw.startPrice().intValue())
        .startedAt(salesHistoryRaw.startedAt())
        .statusDetail(details)
        .build();
  }


}
