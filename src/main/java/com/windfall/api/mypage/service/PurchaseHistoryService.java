package com.windfall.api.mypage.service;

import com.windfall.api.mypage.dto.purchasehistory.BasePurchaseHistory;
import com.windfall.api.mypage.dto.purchasehistory.ChatInfoRaw;
import com.windfall.api.mypage.dto.purchasehistory.ConfirmedPurchaseHistoryResponse;
import com.windfall.api.mypage.dto.purchasehistory.ReviewInfo;
import com.windfall.api.mypage.dto.purchasehistory.ThumbnailImageInfo;
import com.windfall.api.mypage.dto.purchasehistory.TradeGroups;
import com.windfall.api.mypage.dto.purchasehistory.PurchaseHistoryInfo;
import com.windfall.api.mypage.dto.purchasehistory.PurchaseHistoryResponse;
import com.windfall.domain.mypage.repository.PurchaseHistoryQueryRepository;
import com.windfall.domain.trade.enums.TradeStatus;
import com.windfall.global.response.SliceResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PurchaseHistoryService {

  private final PurchaseHistoryQueryRepository purchaseHistoryQueryRepository;

  @Transactional
  public SliceResponse<BasePurchaseHistory> getPurchaseHistories(Long userId, String filter, Pageable pageable){

    //1. rawdata 추출
    Slice<PurchaseHistoryInfo> rawData = purchaseHistoryQueryRepository.getRawPurchaseHistory(userId, filter, pageable);

    //2. id 저장
    List<Long> auctionIds = rawData.stream().map(PurchaseHistoryInfo::auctionId).toList();
    List<Long> tradeIds = rawData.stream().map(PurchaseHistoryInfo::tradeId).toList();

    //3. 채팅 정보
    Map<Long, ChatInfoRaw> chatInfoMap = getChatInfo(tradeIds, userId);

    //4. 썸네일 이미지 정보
    Map<Long, ThumbnailImageInfo> imageInfoMap = getThumbnailImageInfo(auctionIds);

    //5. 리뷰 정보 (상태에 따라 분기되는 정보)
    Map<Long, ReviewInfo> reviewInfoMap = getReviewInfo(tradeIds);

    //6. 순서대로 조립 ㄱㄱ
    List<BasePurchaseHistory> resultContent = rawData.stream().map(data -> {

      ThumbnailImageInfo image = imageInfoMap.getOrDefault(data.auctionId(), new ThumbnailImageInfo(null, null));
      ChatInfoRaw chat = chatInfoMap.getOrDefault(data.tradeId(), new ChatInfoRaw(null, null, 0L));

      if(data.status() == TradeStatus.PAYMENT_COMPLETED){
        ReviewInfo review = reviewInfoMap.getOrDefault(data.tradeId(), new ReviewInfo(null, null));
        return ConfirmedPurchaseHistoryResponse.from(data, image, chat, review);
      }
      return PurchaseHistoryResponse.from(data, image, chat);
    }).toList();

    //7. 다시 slice로 반환
    Slice<BasePurchaseHistory> resultSlice = toSlice(resultContent, rawData);

    return SliceResponse.from(resultSlice);
  }

  private Map<Long, ReviewInfo> getReviewInfo(List<Long> tradeIds){
    return purchaseHistoryQueryRepository.getReviewInfo(tradeIds)
        .stream()
        .collect(Collectors.toMap(
            ReviewInfo::tradeId,
            r -> r
        ));
  }

  private Map<Long, ThumbnailImageInfo> getThumbnailImageInfo(List<Long> auctionIds){
    List<Long> tIds = purchaseHistoryQueryRepository.getThumbnailImageIds(auctionIds);

    return purchaseHistoryQueryRepository.getThumbnailImageInfo(tIds)
        .stream()
        .collect(Collectors.toMap(
            ThumbnailImageInfo::auctionId,
            ti -> ti
        ));
  }

  private Map<Long, ChatInfoRaw> getChatInfo(List<Long> tradeIds, Long userId){
    return purchaseHistoryQueryRepository.getChatInfo(tradeIds, userId)
        .stream()
        .collect(Collectors.toMap(
            ChatInfoRaw::tradeId,
            t -> t
        ));
  }

  private TradeGroups groupingTradeIds(List<PurchaseHistoryInfo> rawData){
    Map<TradeStatus, List<Long>> tradeGroups = new HashMap<>();
    rawData.forEach(raw ->
    {
      tradeGroups.computeIfAbsent(raw.status(), k -> new ArrayList<>()).add(raw.tradeId()); //이거까지 담는 이유: trade 상태별로 쿼리를 실행해야하기 때문에
    });
    return new TradeGroups(tradeGroups);
  }

//  private Map<Long, BasePurchaseHistory> fetchTradeQueries(TradeGroups groups, Long userid){
//    Map<Long, BasePurchaseHistory> resultData = new HashMap<>();
//    Map<TradeStatus, List<Long>> tradeGroups = groups.tradeGroups();
//    Map<TradeStatus, List<Long>> auctionGroups = groups.auctionGroups();
//
//    if(tradeGroups.containsKey(TradeStatus.PAYMENT_COMPLETED)){ //결제 완료
//      purchaseHistoryQueryRepository.getPurchaseHistory(userid, tradeGroups.get(TradeStatus.PAYMENT_COMPLETED), auctionGroups.get(TradeStatus.PAYMENT_COMPLETED)).forEach(
//      data -> resultData.put(data.get("auctionId", Long.class), PurchaseHistoryResponse.from(data)));
//    }
//    if(tradeGroups.containsKey(TradeStatus.PURCHASE_CONFIRMED)){ //구매 확정
//      purchaseHistoryQueryRepository.getConfirmedPurchaseHistory(userid, tradeGroups.get(TradeStatus.PURCHASE_CONFIRMED), auctionGroups.get(TradeStatus.PURCHASE_CONFIRMED)).forEach(
//          data -> resultData.put(data.get("auctionId", Long.class), ConfirmedPurchaseHistoryResponse.from(data)));
//    }
//
//    return resultData;
//  }

  private List<BasePurchaseHistory> orderByResults(List<Long> dataSequence, Map<Long, BasePurchaseHistory> resultData){
    return dataSequence.stream().map(resultData::get).toList();
  }

  private Slice<BasePurchaseHistory> toSlice(List<BasePurchaseHistory> resultContent, Slice<?> rawData){
    return new SliceImpl<>(
        resultContent,
        rawData.getPageable(),
        rawData.hasNext()
    );
  }
}
