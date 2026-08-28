package com.windfall.api.user.dto.response.saleshistory.projections;

import java.time.LocalDateTime;

public interface SalesHistoryBaseProjection {

  Long getAuctionId();

  String getStatus();

  String getTitle();

  Number getStartPrice();

  LocalDateTime getStartedAt();

  String getAuctionImageUrl();
}