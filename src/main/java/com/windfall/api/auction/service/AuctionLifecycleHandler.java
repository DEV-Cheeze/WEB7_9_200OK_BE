package com.windfall.api.auction.service;

import com.windfall.api.auction.dto.AuctionPriceChangeResult;
import com.windfall.api.auction.dto.PriceChangedAuctionInfo;
import com.windfall.api.notification.event.vo.AuctionPriceDroppedEvent;
import com.windfall.api.notification.event.vo.PriceDroppedBroadcastEvent;
import com.windfall.api.notification.event.vo.PriceDroppedBroadcastEvents;
import com.windfall.domain.auction.entity.Auction;
import com.windfall.domain.auction.entity.AuctionPriceHistory;
import com.windfall.domain.auction.repository.AuctionPriceHistoryRepository;
import com.windfall.domain.auction.repository.AuctionRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionLifecycleHandler {

  private final AuctionRepository auctionRepository;
  private final AuctionViewerService viewerService;
  private final AuctionPriceHistoryRepository historyRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public AuctionPriceChangeResult updatePrices(LocalDateTime now){
    List<PriceChangedAuctionInfo> auctionPriceChangeInfos = auctionRepository.getDecreasableAuctions(now);
    int failed = auctionRepository.setFailedAuctions(now);
    int decreased = auctionRepository.decreasePrice(now);

    return new AuctionPriceChangeResult(auctionPriceChangeInfos, failed, decreased);
  }

  @Async
  @Transactional
  public void savePriceHistoryWithViewers(List<PriceChangedAuctionInfo> auctions) {
    List<AuctionPriceHistory> histories = new ArrayList<>();

    for(PriceChangedAuctionInfo info : auctions){
      Auction auction = auctionRepository.getReferenceById(info.auctionId());

      long viewerCount = viewerService.getViewerCount(auction.getId());
      AuctionPriceHistory history = AuctionPriceHistory.create(auction, info.currentPrice(), viewerCount);

      histories.add(history);
    }
    historyRepository.saveAll(histories);
  }

  public void publishEventAndBroadcast(List<PriceChangedAuctionInfo> auctions, LocalDateTime now){
    List<PriceDroppedBroadcastEvent> broadcastAuctions = new ArrayList<>();

    for(PriceChangedAuctionInfo auction : auctions){
      eventPublisher.publishEvent(
          new AuctionPriceDroppedEvent(
              auction.auctionId(),
              auction.oldPrice(),
              auction.currentPrice(),
              now
          )
      );
      broadcastAuctions.add(new PriceDroppedBroadcastEvent(
          auction.auctionId(),
          auction.currentPrice(),
          auction.status()
      ));
    }

    eventPublisher.publishEvent(new PriceDroppedBroadcastEvents(broadcastAuctions));
  }
}
