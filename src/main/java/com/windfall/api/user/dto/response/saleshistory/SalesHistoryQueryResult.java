package com.windfall.api.user.dto.response.saleshistory;

import com.windfall.api.mypage.dto.purchasehistory.ChatInfoRaw;
import com.windfall.api.user.dto.response.reviewlist.AuctionImageRaw;
import java.util.List;
import org.springframework.data.domain.Slice;

public record SalesHistoryQueryResult(

    Slice<SalesHistoryRaw> slicedSalesHistoryRaws,
    List<SalesHistoryRaw> salesHistoryRaws,
    List<AuctionImageRaw> auctionImageRaws,
    List<TradeInfoRaw> tradeInfoRaws,
    List<ProcessSalesRaw> processSalesRaws,
    List<ChatInfoRaw> chatInfos
) {



}
