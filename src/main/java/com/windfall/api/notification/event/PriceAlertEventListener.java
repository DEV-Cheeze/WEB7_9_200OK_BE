package com.windfall.api.notification.event;

import com.windfall.api.auction.service.component.AuctionMessageSender;
import com.windfall.api.notification.event.vo.AuctionPriceDroppedEvent;
import com.windfall.api.notification.event.vo.PriceDroppedBroadcastEvents;
import com.windfall.api.notification.service.SseService;
import com.windfall.domain.notification.entity.PriceNotification;
import com.windfall.domain.notification.repository.PriceNotificationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PriceAlertEventListener {

  private final PriceNotificationRepository priceNotificationRepository;
  private final AuctionMessageSender messageSender;
  private final SseService sseService;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void sendPriceAlert(AuctionPriceDroppedEvent event) {
    List<PriceNotification> alerts =
        priceNotificationRepository.findNotNotified(
            event.auctionId(),
            event.currentPrice()
        );

    for (PriceNotification alert : alerts) {
      if (event.previousPrice() > alert.getTargetPrice()
          && event.currentPrice() <= alert.getTargetPrice()) {
        sseService.priceNotificationSend(
            alert.getUserId(),
            alert.getAuctionId(),
            String.valueOf(alert.getTargetPrice())
        );
      }
    }
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void broadcastPriceUpdates(PriceDroppedBroadcastEvents events) {
    messageSender.broadcastPriceUpdates(events);
  }
}