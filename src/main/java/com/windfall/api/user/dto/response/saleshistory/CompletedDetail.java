package com.windfall.api.user.dto.response.saleshistory;

import com.windfall.api.chat.dto.response.info.ChatInfo;
import com.windfall.api.mypage.dto.purchasehistory.ChatInfoRaw;
import com.windfall.domain.trade.enums.TradeStatus;
import lombok.Builder;

@Builder
public record CompletedDetail(

    int endPrice,
    int discountPercent,
    TradeStatus tradeStatus,
    ChatInfo chatInfo

) implements SalesStatusDetail
{

  public static CompletedDetail from(int endPrice, int discountPercent, TradeStatus tradeStatus, ChatInfoRaw chatInfo){
    ChatInfo info = chatInfo == null ? null : new ChatInfo(chatInfo.chatRoomId(), chatInfo.unreadCount().intValue());

    return CompletedDetail.builder()
        .endPrice(endPrice)
        .discountPercent(discountPercent)
        .tradeStatus(tradeStatus)
        .chatInfo(info)
        .build();


  }


}
