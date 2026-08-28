package com.windfall.api.user.service;

import com.windfall.api.user.dto.response.saleshistory.projections.ChatInfoProjection;
import com.windfall.api.user.dto.response.saleshistory.ChatInfoRaw;
import com.windfall.api.user.dto.response.saleshistory.projections.SalesHistoryBaseProjection;
import com.windfall.api.user.dto.response.saleshistory.SalesHistoryBaseRaw;
import com.windfall.api.user.dto.response.saleshistory.projections.SalesHistoryQueryResult;
import com.windfall.api.user.dto.response.saleshistory.SalesHistoryRaw;
import com.windfall.api.user.dto.response.saleshistory.projections.SalesStatusProjection;
import com.windfall.api.user.dto.response.saleshistory.SalesStatusRaw;
import com.windfall.domain.auction.enums.AuctionStatus;
import com.windfall.domain.trade.enums.TradeStatus;
import com.windfall.domain.user.repository.SalesHistoryQueryRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SalesHistoryQueryServiceV3 {


  private final SalesHistoryQueryRepository repository;
  /**
   * DB 조회와 Repository Projection → 내부 Raw DTO 변환까지만 담당한다.
   *
   * 이 메서드가 반환되면 트랜잭션이 종료되고,
   * 최종 응답 DTO 조립은 SalesHistoryAssemblerV3에서 수행된다.
   */
  @Transactional(readOnly = true)
  public SalesHistoryQueryResult fetch(
      Long sellerId,
      Long loginId,
      AuctionStatus filter,
      Pageable pageable
  ) {
    Slice<Long> auctionIdSlice = findAuctionIds(
        sellerId,
        filter,
        pageable
    );

    List<Long> orderedAuctionIds =
        auctionIdSlice.getContent();

    if (orderedAuctionIds.isEmpty()) {
      return SalesHistoryQueryResult.empty(
          auctionIdSlice.getPageable(),
          auctionIdSlice.hasNext()
      );
    }

    /*
     * 첫 번째 ID 조회에서 페이징이 끝났으므로
     * 이후 쿼리에는 Pageable을 넘기지 않고 해당 ID만 조회한다.
     */
    List<SalesHistoryBaseProjection> baseRows =
        repository.getRawSalesHistoryWithImg(
            orderedAuctionIds
        );

    List<SalesStatusProjection> statusRows =
        repository.getAllStatusInformation(
            orderedAuctionIds
        );

    Map<Long, SalesHistoryBaseRaw> baseByAuctionId =
        createBaseMap(baseRows);

    boolean ownerRequest =
        loginId != null && loginId.equals(sellerId);

    /*
     * 상태 결과를 한 번 순회하면서:
     * 1. auctionId → 상태 상세 Map 생성
     * 2. COMPLETED 상태의 tradeId 수집
     */
    StatusIndexV3 statusIndex =
        createStatusIndex(
            statusRows,
            ownerRequest
        );

    /*
     * 판매자 본인이면서 COMPLETED 거래가 있는 경우에만
     * 실제 채팅 쿼리가 실행된다.
     */
    Map<Long, ChatInfoRaw> chatByTradeId =
        findChatInfo(
            ownerRequest,
            loginId,
            statusIndex.tradeIds()
        );

    /*
     * 이미지는 필수라는 도메인 규칙이 있으므로,
     * ID Slice 결과와 기본정보 조회 결과가 다르면 비정상 상태다.
     */
    validateBaseResults(
        orderedAuctionIds,
        baseByAuctionId
    );

    return new SalesHistoryQueryResult(
        List.copyOf(orderedAuctionIds),
        baseByAuctionId,
        statusIndex.statusByAuctionId(),
        chatByTradeId,
        auctionIdSlice.getPageable(),
        auctionIdSlice.hasNext()
    );
  }

  private Slice<Long> findAuctionIds(
      Long sellerId,
      AuctionStatus filter,
      Pageable pageable
  ) {
    if (filter == null) {
      return repository.getAuctionIdListWithoutFilter(
          sellerId,
          pageable
      );
    }

    return repository.getAuctionIdList(
        sellerId,
        filter,
        pageable
    );
  }

  private Map<Long, SalesHistoryBaseRaw> createBaseMap(
      List<SalesHistoryBaseProjection> rows
  ) {
    Map<Long, SalesHistoryBaseRaw> result =
        new HashMap<>(calculateMapCapacity(rows.size()));

    for (SalesHistoryBaseProjection row : rows) {

      Long auctionId = Objects.requireNonNull(
          row.getAuctionId(),
          "기본 조회 결과에 auctionId가 없습니다."
      );

      String statusValue = Objects.requireNonNull(
          row.getStatus(),
          "기본 조회 결과에 status가 없습니다. auctionId="
              + auctionId
      );

      AuctionStatus status =
          AuctionStatus.valueOf(statusValue);

      Long startPrice = toRequiredLong(
          row.getStartPrice(),
          "startPrice",
          auctionId
      );

      String imageUrl = Objects.requireNonNull(
          row.getAuctionImageUrl(),
          "경매 이미지가 없습니다. auctionId="
              + auctionId
      );

      SalesHistoryRaw salesHistoryRaw =
          new SalesHistoryRaw(
              status,
              auctionId,
              row.getTitle(),
              startPrice,
              row.getStartedAt()
          );

      SalesHistoryBaseRaw baseRaw =
          new SalesHistoryBaseRaw(
              salesHistoryRaw,
              imageUrl
          );

      SalesHistoryBaseRaw previous =
          result.put(auctionId, baseRaw);

      /*
       * 대표 이미지 쿼리는 경매당 한 행만 반환해야 한다.
       * 중복이 발생하면 이미지 조인이나 데이터 제약을 확인해야 한다.
       */
      if (previous != null) {
        throw new IllegalStateException(
            "경매 기본정보가 중복 조회되었습니다. auctionId="
                + auctionId
        );
      }
    }

    return result;
  }

  private StatusIndexV3 createStatusIndex(
      List<SalesStatusProjection> rows,
      boolean collectTradeIds
  ) {
    Map<Long, SalesStatusRaw> statusByAuctionId =
        new HashMap<>(calculateMapCapacity(rows.size()));

    List<Long> tradeIds = collectTradeIds
        ? new ArrayList<>()
        : List.of();

    for (SalesStatusProjection row : rows) {
      Long auctionId = Objects.requireNonNull(
          row.getAuctionId(),
          "상태 조회 결과에 auctionId가 없습니다."
      );

      TradeStatus tradeStatus =
          convertTradeStatus(row.getTradeStatus());

      SalesStatusRaw statusRaw =
          new SalesStatusRaw(
              auctionId,
              row.getTradeId(),
              toNullableLong(row.getCurrentPrice()),
              toNullableLong(row.getFinalPrice()),
              tradeStatus
          );

      SalesStatusRaw previous =
          statusByAuctionId.put(
              auctionId,
              statusRaw
          );

      /*
       * UNION의 각 분기는 서로 배타적이고,
       * COMPLETED 경매당 Trade가 하나라는 전제다.
       */
      if (previous != null) {
        throw new IllegalStateException(
            "경매 상태 상세정보가 중복 조회되었습니다. auctionId="
                + auctionId
        );
      }

      if (
          collectTradeIds
              && row.getTradeId() != null
      ) {
        tradeIds.add(row.getTradeId());
      }
    }

    return new StatusIndexV3(
        statusByAuctionId,
        tradeIds
    );
  }

  private Map<Long, ChatInfoRaw> findChatInfo(
      boolean ownerRequest,
      Long loginId,
      List<Long> tradeIds
  ) {
    /*
     * 타인 조회 또는 COMPLETED 거래가 없는 경우
     * Repository 메서드 자체를 호출하지 않는다.
     */
    if (!ownerRequest || tradeIds.isEmpty()) {
      return Map.of();
    }

    List<ChatInfoProjection> chatRows =
        repository.getChatInfo(
            tradeIds,
            loginId
        );

    Map<Long, ChatInfoRaw> result =
        new HashMap<>(
            calculateMapCapacity(chatRows.size())
        );

    for (ChatInfoProjection row : chatRows) {
      Long tradeId = Objects.requireNonNull(
          row.getTradeId(),
          "채팅 조회 결과에 tradeId가 없습니다."
      );

      Long chatRoomId = Objects.requireNonNull(
          row.getChatRoomId(),
          "채팅 조회 결과에 chatRoomId가 없습니다. tradeId="
              + tradeId
      );

      Long unreadCount = Objects.requireNonNull(
          row.getUnreadCount(),
          "채팅 조회 결과에 unreadCount가 없습니다. tradeId="
              + tradeId
      );

      ChatInfoRaw chatInfoRaw =
          new ChatInfoRaw(
              tradeId,
              chatRoomId,
              unreadCount
          );

      ChatInfoRaw previous =
          result.put(
              tradeId,
              chatInfoRaw
          );

      /*
       * 거래당 채팅방이 하나라는 전제다.
       */
      if (previous != null) {
        throw new IllegalStateException(
            "거래당 채팅 정보가 둘 이상 조회되었습니다. tradeId="
                + tradeId
        );
      }
    }

    return result;
  }

  private TradeStatus convertTradeStatus(
      String tradeStatus
  ) {
    if (tradeStatus == null) {
      return null;
    }

    return TradeStatus.valueOf(tradeStatus);
  }

  private Long toRequiredLong(
      Number value,
      String fieldName,
      Long auctionId
  ) {
    if (value == null) {
      throw new IllegalStateException(
          fieldName + " 값이 없습니다. auctionId="
              + auctionId
      );
    }

    return value.longValue();
  }

  private Long toNullableLong(Number value) {
    return value == null
        ? null
        : value.longValue();
  }

  private void validateBaseResults(
      List<Long> auctionIds,
      Map<Long, SalesHistoryBaseRaw> baseByAuctionId
  ) {
    if (auctionIds.size() == baseByAuctionId.size()) {
      return;
    }

    for (Long auctionId : auctionIds) {
      if (!baseByAuctionId.containsKey(auctionId)) {
        throw new IllegalStateException(
            "기본정보 또는 이미지가 누락된 경매입니다. auctionId="
                + auctionId
        );
      }
    }
  }

  /**
   * HashMap의 기본 load factor 0.75를 고려한 초기 용량.
   * 페이지 크기가 작기 때문에 성능 영향은 미미하지만
   * 불필요한 resize를 피할 수 있다.
   */
  private int calculateMapCapacity(int size) {
    if (size == 0) {
      return 0;
    }

    return (int) Math.ceil(size / 0.75d);
  }

  private record StatusIndexV3(
      Map<Long, SalesStatusRaw> statusByAuctionId,
      List<Long> tradeIds
  ) {
  }

}
