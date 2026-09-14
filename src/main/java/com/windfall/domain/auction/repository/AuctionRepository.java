package com.windfall.domain.auction.repository;

import com.windfall.api.auction.dto.PriceChangedAuctionInfo;
import com.windfall.domain.auction.entity.Auction;
import com.windfall.domain.auction.enums.AuctionStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuctionRepository extends JpaRepository<Auction,Long>, AuctionRepositoryCustom {

  List<Auction> findAllByStatusAndStartedAtLessThanEqual(AuctionStatus status, LocalDateTime now);

  @Modifying
  @Query(value = """
  UPDATE Auction a
  SET a.status = 'FAILED', a.currentPrice = a.stopLoss WHERE
  a.activated = true AND
  a.status = 'PROCESS' AND
  a.stopLossReachedAt <= :now
  """)
  int setFailedAuctions(@Param("now") LocalDateTime now);

  @Modifying
  @Query(value = """
  UPDATE auction a
  SET a.current_price = 
        a.start_price - 
        FLOOR(TIMESTAMPDIFF(MINUTE, started_at, :now) / 5)
        * a.drop_amount
  WHERE
  a.activated = true AND
  a.status = 'PROCESS' AND
  a.stop_loss_reached_at > :now
  """, nativeQuery = true)
  int decreasePrice(@Param("now") LocalDateTime now);

  @Query(value = """
  SELECT
    a.id,
    a.currentPrice,
    CAST(a.startPrice - FLOOR(TIMESTAMPDIFF(minute, a.startedAt, :now) / 5) * a.dropAmount as Long),
    a.status
  FROM Auction a
  WHERE
  a.activated = true AND
  a.status = 'PROCESS' AND
  a.stopLossReachedAt > :now
  """)
  List<PriceChangedAuctionInfo> getDecreasableAuctions(@Param("now") LocalDateTime now);
}