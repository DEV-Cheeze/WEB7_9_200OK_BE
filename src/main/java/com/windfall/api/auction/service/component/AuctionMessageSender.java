package com.windfall.api.auction.service.component;

import com.windfall.api.notification.event.vo.PriceDroppedBroadcastEvents;
import com.windfall.domain.auction.enums.AuctionStatus;
import com.windfall.domain.auction.enums.EmojiType;

public interface AuctionMessageSender {

  void broadcastPriceUpdates(PriceDroppedBroadcastEvents events);

  void broadcastPriceUpdate(Long auctionId, Long currentPrice, AuctionStatus status);

  void broadcastViewerCount(Long auctionId, long viewerCount);

  void broadcastSellerEmoji(Long auctionId, EmojiType emojiType);

}
