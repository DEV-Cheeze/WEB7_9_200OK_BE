package com.windfall.api.user.service;

import com.windfall.api.chat.dto.response.info.ChatInfo;
import com.windfall.api.user.dto.response.saleshistory.CompletedDetail;
import com.windfall.api.user.dto.response.saleshistory.ProcessFailedDetail;
import com.windfall.api.user.dto.response.saleshistory.SalesHistoryResponse;
import com.windfall.api.user.dto.response.saleshistory.SalesStatusDetail;
import com.windfall.api.user.dto.response.saleshistory.ChatInfoRaw;
import com.windfall.api.user.dto.response.saleshistory.SalesHistoryBaseRaw;
import com.windfall.api.user.dto.response.saleshistory.projections.SalesHistoryQueryResult;
import com.windfall.api.user.dto.response.saleshistory.SalesHistoryRaw;
import com.windfall.api.user.dto.response.saleshistory.SalesStatusRaw;
import com.windfall.domain.trade.enums.TradeStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Component;

@Component
public class SalesHistoryAssembler {

  public Slice<SalesHistoryResponse> assemble(
      SalesHistoryQueryResult queryResult
  ) {
    List<Long> orderedAuctionIds =
        queryResult.orderedAuctionIds();

    List<SalesHistoryResponse> content =
        new ArrayList<>(orderedAuctionIds.size());

    /*
     * 이미지/상태 쿼리 결과 순서는 보장되지 않으므로
     * 최초 ID Slice 순서대로 응답을 조립한다.
     */
    for (Long auctionId : orderedAuctionIds) {
      SalesHistoryBaseRaw baseRaw =
          requireBaseRaw(
              auctionId,
              queryResult.baseByAuctionId()
          );

      SalesHistoryRaw salesRaw =
          baseRaw.salesHistory();

      SalesStatusDetail statusDetail =
          createStatusDetail(
              salesRaw,
              queryResult.statusByAuctionId()
                  .get(auctionId),
              queryResult.chatByTradeId()
          );

      content.add(
          new SalesHistoryResponse(
              salesRaw.status(),
              salesRaw.auctionId(),
              salesRaw.title(),
              baseRaw.auctionImageUrl(),
              Math.toIntExact(salesRaw.startPrice()),
              salesRaw.startedAt(),
              statusDetail
          )
      );
    }

    return new SliceImpl<>(
        content,
        queryResult.pageable(),
        queryResult.hasNext()
    );
  }

  private SalesStatusDetail createStatusDetail(
      SalesHistoryRaw salesRaw,
      SalesStatusRaw statusRaw,
      Map<Long, ChatInfoRaw> chatByTradeId
  ) {
    return switch (salesRaw.status()) {
      case PROCESS, FAILED ->
          createProcessFailedDetail(
              salesRaw,
              requireStatusRaw(
                  salesRaw.auctionId(),
                  statusRaw
              )
          );

      case COMPLETED ->
          createCompletedDetail(
              salesRaw,
              requireStatusRaw(
                  salesRaw.auctionId(),
                  statusRaw
              ),
              chatByTradeId
          );

      /*
       * 준비, 취소 등 별도 상세 데이터가 필요 없는 상태
       */
      default -> null;
    };
  }

  private ProcessFailedDetail createProcessFailedDetail(
      SalesHistoryRaw salesRaw,
      SalesStatusRaw statusRaw
  ) {
    Long currentPrice =
        Objects.requireNonNull(
            statusRaw.currentPrice(),
            "PROCESS/FAILED 상태의 currentPrice가 없습니다. auctionId="
                + salesRaw.auctionId()
        );

    int discountPercent =
        calculateDiscountPercent(
            salesRaw.startPrice(),
            currentPrice
        );

    return ProcessFailedDetail.of(
        Math.toIntExact(currentPrice),
        discountPercent
    );
  }

  private CompletedDetail createCompletedDetail(
      SalesHistoryRaw salesRaw,
      SalesStatusRaw statusRaw,
      Map<Long, ChatInfoRaw> chatByTradeId
  ) {
    Long tradeId =
        Objects.requireNonNull(
            statusRaw.tradeId(),
            "COMPLETED 상태의 tradeId가 없습니다. auctionId="
                + salesRaw.auctionId()
        );

    Long finalPrice =
        Objects.requireNonNull(
            statusRaw.finalPrice(),
            "COMPLETED 상태의 finalPrice가 없습니다. auctionId="
                + salesRaw.auctionId()
        );

    TradeStatus tradeStatus =
        Objects.requireNonNull(
            statusRaw.tradeStatus(),
            "COMPLETED 상태의 tradeStatus가 없습니다. auctionId="
                + salesRaw.auctionId()
        );

    /*
     * 타인 조회라면 chatByTradeId가 빈 Map이므로 null.
     * 본인 조회라도 기존 채팅 쿼리 결과가 없다면 null.
     */
    ChatInfoRaw chatRaw =
        chatByTradeId.get(tradeId);

    ChatInfo chatInfo =
        chatRaw == null
            ? null
            : new ChatInfo(
                chatRaw.chatRoomId(),
                Math.toIntExact(chatRaw.unreadCount())
            );

    int discountPercent =
        calculateDiscountPercent(
            salesRaw.startPrice(),
            finalPrice
        );

    return new CompletedDetail(
        Math.toIntExact(finalPrice),
        discountPercent,
        tradeStatus,
        chatInfo
    );
  }

  private SalesHistoryBaseRaw requireBaseRaw(
      Long auctionId,
      Map<Long, SalesHistoryBaseRaw> baseByAuctionId
  ) {
    SalesHistoryBaseRaw baseRaw =
        baseByAuctionId.get(auctionId);

    if (baseRaw == null) {
      throw new IllegalStateException(
          "경매 기본정보 또는 이미지가 없습니다. auctionId="
              + auctionId
      );
    }

    return baseRaw;
  }

  private SalesStatusRaw requireStatusRaw(
      Long auctionId,
      SalesStatusRaw statusRaw
  ) {
    if (statusRaw == null) {
      throw new IllegalStateException(
          "경매 상태 상세정보가 없습니다. auctionId="
              + auctionId
      );
    }

    return statusRaw;
  }

  private int calculateDiscountPercent(
      long startPrice,
      long resultPrice
  ) {
    if (startPrice <= 0) {
      return 0;
    }

    long priceDifference =
        startPrice - resultPrice;

    if (priceDifference <= 0) {
      return 0;
    }

    long discountPercent =
        priceDifference * 100L / startPrice;

    return Math.toIntExact(
        Math.min(discountPercent, 100L)
    );
  }
}
