package com.windfall.api.user.dto.response.saleshistory.projections;

public interface SalesStatusProjection {

  Long getTradeId();

  Long getAuctionId();

  Number getCurrentPrice();

  Number getFinalPrice();

  String getTradeStatus();

}
