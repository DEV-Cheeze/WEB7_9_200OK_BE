package com.windfall.api.auction.service;

import static com.windfall.domain.auction.enums.AuctionStatus.COMPLETED;
import static com.windfall.domain.auction.enums.AuctionStatus.PROCESS;
import static com.windfall.global.exception.ErrorCode.NOT_FOUND_AUCTION;

import com.windfall.api.auction.dto.AuctionPriceChangeResult;
import com.windfall.api.auction.dto.PriceChangedAuctionInfo;
import com.windfall.api.auction.service.component.AuctionMessageSender;
import com.windfall.api.notification.service.SseService;
import com.windfall.domain.auction.entity.Auction;
import com.windfall.domain.auction.repository.AuctionRepository;
import com.windfall.domain.notification.entity.NotificationSetting;
import com.windfall.domain.notification.enums.NotificationSettingType;
import com.windfall.domain.notification.repository.NotificationSettingRepository;
import com.windfall.global.exception.ErrorException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionStateService {

  private final AuctionRepository auctionRepository;
  private final AuctionLifecycleHandler lifecycleHandler;
  private final AuctionMessageSender messageSender;
  private final NotificationSettingRepository notificationSettingRepository;
  private final SseService sseService;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void startAuction(Long auctionId) {
    auctionRepository.findById(auctionId).ifPresent(auction -> {

      auction.start();
      messageSender.broadcastPriceUpdate(auctionId, auction.getCurrentPrice(), PROCESS);

      notifyAuctionStart(auction);

      log.info("✅경매 시작 처리 완료 ( 경매 ID: {} )", auction.getId());
    });
  }

  @Transactional
  public void decreasePrice(LocalDateTime now){

    AuctionPriceChangeResult changedAuctions = lifecycleHandler.updatePrices(now);

    List<PriceChangedAuctionInfo> changes = changedAuctions.changes();

    int failed = changedAuctions.failed();
    int decreased = changedAuctions.decreased();

    lifecycleHandler.savePriceHistoryWithViewers(changes); //Async로 처리
    lifecycleHandler.publishEventAndBroadcast(changes, now);

    log.info("Auction Scheduler Info : 유찰 - {}, 감소 - {}", failed, decreased);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void completeAuction(Long auctionId) {
    Auction auction = findAuctionById(auctionId);

    if(auction.getStatus() == COMPLETED) return;

    auction.updateStatus(COMPLETED);

    notifyAuctionSuccess(auction);
  }

  private Auction findAuctionById(Long auctionId) {
    return auctionRepository.findById(auctionId)
        .orElseThrow(() -> new ErrorException(NOT_FOUND_AUCTION));
  }

  private void notifyAuctionStart(Auction auction) {
    notifySubscribers(
        auction,
        NotificationSettingType.AUCTION_START,
        (userId, auc) ->
            sseService.auctionStartNotificationSend(
                userId,
                auc.getId(),
                auc.getTitle()
            )
    );
  }

  private void notifyAuctionSuccess(Auction auction) {
    notifySeller(
        auction,
        (auc) ->
            sseService.sendAuctionSuccessToSeller(
                auc.getSeller().getId(),
                auc.getId(),
                auc.getTitle()
            )
    );

    notifySubscribers(
        auction,
        NotificationSettingType.AUCTION_END,
        (userId, auc) ->
            sseService.sendAuctionSuccessToSubscriber(
                userId,
                auc.getId(),
                auc.getTitle()
            )
    );
  }

  private void notifySeller(
      Auction auction,
      Consumer<Auction> notifyAction
  ) {
    try {
      notifyAction.accept(auction);
    } catch (Exception e) {
      log.error("판매자 알림 발송 실패 [User: {}]: {}", auction.getSeller().getId(), e.getMessage());
    }
  }

  private void notifySubscribers(
      Auction auction,
      NotificationSettingType settingType,
      BiConsumer<Long, Auction> notifyAction
  ) {
    List<NotificationSetting> settings = getActiveAuctionStartSettings(auction, settingType);

    if (settings.isEmpty()) return;

    for (NotificationSetting setting : settings) {
      Long userId = setting.getUser().getId();
      try {
        notifyAction.accept(userId, auction);
      } catch (Exception e) {
        log.error("알림 설정 유저 알림 발송 실패 [User: {}]: {}", userId, e.getMessage());
      }
    }
  }

  private List<NotificationSetting> getActiveAuctionStartSettings(
      Auction auction, NotificationSettingType type
  ) {
    return notificationSettingRepository.findAllActiveByAuctionAndType(
        auction.getId(),
        type
    );
  }
}