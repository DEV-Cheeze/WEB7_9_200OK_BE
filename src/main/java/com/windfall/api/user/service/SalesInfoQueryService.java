package com.windfall.api.user.service;

import static com.windfall.domain.auction.enums.AuctionStatusGroup.COMPLETED;
import static com.windfall.domain.auction.enums.AuctionStatusGroup.PROCESS_FAILED;
import static com.windfall.domain.auction.enums.AuctionStatusGroup.READY_CANCEL;

import com.windfall.api.mypage.dto.purchasehistory.ChatInfoRaw;
import com.windfall.api.user.dto.response.reviewlist.AuctionImageRaw;
import com.windfall.api.user.dto.response.saleshistory.ProcessSalesRaw;
import com.windfall.api.user.dto.response.saleshistory.SalesHistoryQueryResult;
import com.windfall.api.user.dto.response.saleshistory.SalesHistoryRaw;
import com.windfall.api.user.dto.response.saleshistory.TradeInfoRaw;
import com.windfall.api.user.dto.response.saleshistory.projections.ChatInfoProjection;
import com.windfall.domain.auction.enums.AuctionStatus;
import com.windfall.domain.auction.enums.AuctionStatusGroup;
import com.windfall.domain.auction.repository.AuctionImageRepository;
import com.windfall.domain.user.repository.SalesHistoryQueryRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SalesInfoQueryService {

  private final AuctionImageRepository auctionImageRepository;
  private final SalesHistoryQueryRepository salesHistoryQueryRepository;


  @Transactional(readOnly = true)
  public SalesHistoryQueryResult getSalesHistoryResults(Long userId, Long loginId, String filter, Pageable pageable){

    AuctionStatus status = filter == null ? null : AuctionStatus.valueOf(filter);

    // 1. 필요한 쿼리 뽑아오기
    Slice<SalesHistoryRaw> sliceSalesHistory = status == null ?
        salesHistoryQueryRepository.getRawSalesHistoryWithoutFilter(userId, pageable) :
        salesHistoryQueryRepository.getRawSalesHistory(userId, status, pageable);

    List<SalesHistoryRaw> baseSalesHistories = sliceSalesHistory.getContent();

    List<Long> auctionIds = baseSalesHistories.stream().map(SalesHistoryRaw::auctionId).toList();

    Map<AuctionStatusGroup, List<Long>> groupingAuctionIds = groupingStatus(baseSalesHistories);

    List<Long> completedIds = groupingAuctionIds.getOrDefault(COMPLETED, List.of());
    List<Long> processFailedIds = groupingAuctionIds.getOrDefault(PROCESS_FAILED, List.of());

    // 3. 이미지 추출
    List<AuctionImageRaw> auctionImages = auctionImageRepository.findFirstImagesProjection(auctionIds);
    // 4. 상태 별 필요한 값 호출

    List<ProcessSalesRaw> processFailedRaws = processFailedIds.isEmpty() ? List.of() : salesHistoryQueryRepository.getProcessSales(processFailedIds);

    List<TradeInfoRaw> tradeInfoRaws = completedIds.isEmpty() ? List.of() : salesHistoryQueryRepository.getTradeInfoRaws(completedIds);


    //로그인 상태 확인하여 chatInfo 쿼리 실행여부 결정
    List<ChatInfoProjection> chatInfos = new ArrayList<>();

    if(userId.equals(loginId)){
      List<Long> tradeIds = tradeInfoRaws.stream().map(TradeInfoRaw::tradeId).toList();
      chatInfos = completedIds.isEmpty() ? List.of() : salesHistoryQueryRepository.getChatInfo(tradeIds, userId);
    }
    
    return new SalesHistoryQueryResult(sliceSalesHistory, baseSalesHistories, auctionImages, tradeInfoRaws, processFailedRaws, chatInfos);
  }

  private Map<AuctionStatusGroup, List<Long>> groupingStatus(List<SalesHistoryRaw> salesHistories){

    Map<AuctionStatusGroup, List<Long>> result = new HashMap<>();

    List<Long> completed = new ArrayList<>();
    List<Long> processFailed = new ArrayList<>();
    List<Long> defaultStatus = new ArrayList<>();

    for(SalesHistoryRaw status : salesHistories){
      switch (status.status()){
        case COMPLETED -> completed.add(status.auctionId());
        case PROCESS, FAILED -> processFailed.add(status.auctionId());
        default -> defaultStatus.add(status.auctionId());
      }
    }

    result.put(COMPLETED, completed);
    result.put(PROCESS_FAILED, processFailed);
    result.put(READY_CANCEL, defaultStatus);

    return result;

  }
}
