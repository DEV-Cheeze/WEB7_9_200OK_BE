package com.windfall.api.auction.service.component;

import com.windfall.api.auction.dto.response.message.AuctionMessage;
import com.windfall.api.auction.dto.response.message.AuctionViewerMessage;
import com.windfall.api.auction.dto.response.message.SellerEmojiMessage;
import com.windfall.api.notification.event.vo.PriceDroppedBroadcastEvent;
import com.windfall.api.notification.event.vo.PriceDroppedBroadcastEvents;
import com.windfall.domain.auction.enums.AuctionStatus;
import com.windfall.domain.auction.enums.EmojiType;
import com.windfall.global.redis.RedisPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@Async("socketTaskExecutor")
public class StompAuctionMessageSender implements AuctionMessageSender {

  private final RedisPublisher redisPublisher;

  @Override
  public void broadcastPriceUpdates(PriceDroppedBroadcastEvents events) {

    Long success = 0L;
    Long fail = 0L;

    for(PriceDroppedBroadcastEvent event : events.events()){
      try{
        broadcastPriceUpdate(event.auctionId(), event.currentPrice(), event.status());
        success++;
      } catch (RuntimeException e){
        log.warn("가격 브로드캐스트 실패. auctionId={}", event.auctionId(), e);
        fail++;
      }
    }
    log.info("Redis 실시간 가격 발행 완료. success={}, fail={}", success, fail);
  }

  @Override
  public void broadcastPriceUpdate(Long auctionId, Long currentPrice, AuctionStatus status) {
    AuctionMessage message = AuctionMessage.of(auctionId, currentPrice, status);
    redisPublisher.publish("/topic/auction/" + auctionId, message);
  }

  @Override
  public void broadcastViewerCount(Long auctionId, long viewerCount) {
    AuctionViewerMessage message = AuctionViewerMessage.of(auctionId, viewerCount);
    redisPublisher.publish("/topic/auction/" + auctionId, message);

  }

  @Override
  public void broadcastSellerEmoji(Long auctionId, EmojiType emojiType) {
    SellerEmojiMessage message = SellerEmojiMessage.of(auctionId, emojiType);
    redisPublisher.publish("/topic/auction/" + auctionId, message);
  }
}
