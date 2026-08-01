package com.windfall.global.migration.auction.repository;

import com.windfall.domain.auction.entity.Auction;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StopLossBackFillRepo extends JpaRepository<Auction, Long> {

  @Modifying
  @Query(value = """
  UPDATE auction a
  SET a.stop_loss_reached_at = 
        DATE_ADD(
        a.started_at,
        INTERVAL (
            (FLOOR((a.start_price - a.stop_loss) / a.drop_amount) + 1) * 5
        ) MINUTE)
  WHERE a.activated = true AND
        a.stop_loss_reached_at IS NULL AND
        a.status = 'PROCESS' AND
        a.id IN :auctionIds
  """, nativeQuery = true)
  int backfillStopLossReachedAtForAlreadyReached(@Param("auctionIds") List<Long> auctionIds);

  @Query(value = """
  SELECT a.id FROM Auction a
  WHERE a.activated = true AND
  a.status = 'PROCESS' AND
  a.stopLossReachedAt IS NULL
  """)
  List<Long> findAllProcessAuctionIds(Pageable page);


}
