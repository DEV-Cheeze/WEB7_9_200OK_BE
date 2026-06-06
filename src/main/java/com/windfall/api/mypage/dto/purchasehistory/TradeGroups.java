package com.windfall.api.mypage.dto.purchasehistory;

import com.windfall.domain.trade.enums.TradeStatus;
import java.util.List;
import java.util.Map;

public record TradeGroups(
    Map<TradeStatus, List<Long>> tradeGroups
) {
}
