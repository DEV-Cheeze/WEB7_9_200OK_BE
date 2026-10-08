package com.windfall.domain.user.repository;

import com.windfall.api.mypage.dto.purchasehistory.ChatInfoRaw;
import com.windfall.api.user.dto.response.saleshistory.ProcessSalesRaw;
import com.windfall.api.user.dto.response.saleshistory.SalesHistoryRaw;
import com.windfall.api.user.dto.response.saleshistory.TradeInfoRaw;
import com.windfall.api.user.dto.response.saleshistory.projections.ChatInfoProjection;
import com.windfall.api.user.dto.response.saleshistory.projections.SalesHistoryBaseProjection;
import com.windfall.api.user.dto.response.saleshistory.projections.SalesStatusProjection;
import com.windfall.domain.auction.entity.Auction;
import com.windfall.domain.auction.enums.AuctionStatus;
import jakarta.persistence.Tuple;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SalesHistoryQueryRepository extends JpaRepository<Auction, Long> {


  @Query(value = """
    SELECT a.id
    FROM Auction a
    WHERE a.activated = true AND a.seller.id = :id
    ORDER BY a.startedAt DESC
  """)
  Slice<Long> getAuctionIdListWithoutFilter(@Param("id") Long sellerId, Pageable pageable);

  @Query(value = """
    SELECT a.id
    FROM Auction a
    WHERE
    a.activated = true AND
    a.seller.id = :id AND
    a.status = :filter
    ORDER BY a.startedAt DESC
  """)
  Slice<Long> getAuctionIdList(@Param("id") Long sellerId, @Param("filter") AuctionStatus filter, Pageable pageable);

  @Query(value = """
    SELECT a.id as auctionId,
        a.status as status,
        a.title as title,
        a.start_price as startPrice,
        a.started_at as startedAt,
        ai.image as auctionImageUrl
    FROM auction a
    JOIN (SELECT aimg.auction_id as aucid, MIN(aimg.id) as first_img
        FROM auction_image aimg
        WHERE auction_id IN (:aucids)
        GROUP BY aimg.auction_id)
       aisub ON aisub.aucid = a.id
    JOIN auction_image ai ON ai.id = aisub.first_img
    WHERE a.activated = true AND a.id IN (:aucids)
  """, nativeQuery = true)
  List<SalesHistoryBaseProjection> getRawSalesHistoryWithImg(@Param("aucids") List<Long> auctionIds);

  @Query(value = """
    SELECT a.id as auctionId,
     a.status as status,
     a.title as title,
     a.start_price as startPrice,
     a.started_at as startedAt,
     (
         SELECT ai.image
         FROM auction_image ai
         WHERE ai.auction_id = a.id
         ORDER BY ai.id
         LIMIT 1
     ) AS auctionImageUrl
    FROM auction a
    WHERE a.id IN (:aucids) AND
        a.activated = true;
  """, nativeQuery = true)
  List<SalesHistoryBaseProjection> getRawSalesHistoryWithImgV3(@Param("aucids") List<Long> auctionIds);

  @Query(value = """
    SELECT NULL as tradeId, a.id as auctionId, a.current_price as currentPrice, NULL as finalPrice, NULL as tradeStatus FROM auction a -- process
    WHERE a.id IN (:auctionIds) AND a.status IN ('PROCESS', 'FAILED')
    UNION ALL
    SELECT t.id as tradeId, a.id as auctionId, NULL as currentPrice, t.final_price as finalPrice, t.status as tradeStatus FROM auction a -- completed
    JOIN trade t ON a.id = t.auction_id WHERE a.id IN (:auctionIds) AND a.status = 'COMPLETED';
  """, nativeQuery = true)
  List<SalesStatusProjection> getAllStatusInformation(@Param("auctionIds") List<Long> auctionIds);

  @Query("""
    SELECT
    a.id,
    a.status,
    a.title,
    a.startPrice,
    a.startedAt
    FROM Auction a
    WHERE a.seller.id = :id AND
    a.status = :filter AND
    a.activated = true
    ORDER BY a.startedAt DESC
  """)
  Slice<SalesHistoryRaw> getRawSalesHistory(@Param("id") Long userId, @Param("filter") AuctionStatus filter, Pageable pageable);

  @Query("""
    SELECT
    a.id,
    a.status,
    a.title,
    a.startPrice,
    a.startedAt
    FROM Auction a
    WHERE a.seller.id = :id AND
    a.activated = true
    ORDER BY a.startedAt DESC
  """)
  Slice<SalesHistoryRaw> getRawSalesHistoryWithoutFilter(@Param("id") Long userId, Pageable pageable);

  @Query("""
  SELECT
  a.id,
  a.currentPrice
  FROM Auction a
  WHERE a.id IN(:ids)
  """)
  List<ProcessSalesRaw> getProcessSales(@Param("ids") List<Long> auctionIds);

  @Query("""
  SELECT
  t.auction.id,
  t.id,
  t.finalPrice,
  t.status
  FROM Trade t
  WHERE t.auction.id IN (:ids)
  """)
  List<TradeInfoRaw> getTradeInfoRaws(@Param("ids") List<Long> auctionIds);

  @Query("""
  SELECT t.id, cr.id, COUNT(cm.id)
  FROM Trade t
      LEFT JOIN ChatRoom cr ON t.id = cr.trade.id
  LEFT JOIN ChatMessage cm ON cr.id = cm.chatRoom.id
  WHERE t.id IN (:tradeIds) AND cm.isRead = false AND cm.sender.id != :userId
  GROUP BY t.id, cr.id, cm.isRead
  """)
  List<ChatInfoProjection> getChatInfo(@Param("tradeIds") List<Long> tradeIds, @Param("userId") Long userId);
}
