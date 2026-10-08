package com.windfall.api.user.dto.response.saleshistory;

public record ProcessFailedDetail(

    int currentPrice,
    int discountPercent

) implements SalesStatusDetail {

  public static ProcessFailedDetail of(int currentPrice, int discountPercent){
    return new ProcessFailedDetail(currentPrice, discountPercent);
  }

}
