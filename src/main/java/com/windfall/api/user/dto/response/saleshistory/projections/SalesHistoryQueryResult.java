package com.windfall.api.user.dto.response.saleshistory.projections;

import com.windfall.api.user.dto.response.saleshistory.ChatInfoRaw;
import com.windfall.api.user.dto.response.saleshistory.SalesHistoryBaseRaw;
import com.windfall.api.user.dto.response.saleshistory.SalesStatusRaw;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Pageable;

public record SalesHistoryQueryResult(
    List<Long> orderedAuctionIds,
    Map<Long, SalesHistoryBaseRaw> baseByAuctionId,
    Map<Long, SalesStatusRaw> statusByAuctionId,
    Map<Long, ChatInfoRaw> chatByTradeId,
    Pageable pageable,
    boolean hasNext
) {

  public static SalesHistoryQueryResult empty(
      Pageable pageable,
      boolean hasNext
  ) {
    return new SalesHistoryQueryResult(
        List.of(),
        Map.of(),
        Map.of(),
        Map.of(),
        pageable,
        hasNext
    );
  }
}