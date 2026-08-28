package com.windfall.api.auction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.windfall.api.auction.dto.AuctionSellerInfoCachedData;
import com.windfall.api.auction.dto.AuctionSellerInfoData;
import com.windfall.api.auction.dto.response.AuctionSellerInfoResponse;
import com.windfall.global.redis.enums.CacheDataStatus;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuctionSellerInfoServiceTest {
  private static final Long SELLER_ID = 1L;

  @Mock
  private AuctionSellerInfoLoader auctionSellerInfoLoader;

  @Mock
  private AuctionSellerInfoCache auctionSellerInfoCache;

  @InjectMocks
  private AuctionSellerInfoService auctionSellerInfoService;

  @Test
  @DisplayName("캐시 HIT이면 캐시 데이터를 반환하고 DB를 조회하지 않는다")
  void hit_returnsCachedResponse_withoutDatabaseLookup() {
    // given
    AuctionSellerInfoResponse cachedResponse =
        mock(AuctionSellerInfoResponse.class);

    AuctionSellerInfoCachedData cachedData =
        new AuctionSellerInfoCachedData(
            CacheDataStatus.HIT,
            cachedResponse
        );

    given(auctionSellerInfoCache.get(SELLER_ID))
        .willReturn(cachedData);

    // when
    AuctionSellerInfoResponse result =
        auctionSellerInfoService.getAuctionSellerInfo(SELLER_ID);

    // then
    assertThat(result).isSameAs(cachedResponse);

    verify(auctionSellerInfoCache).get(SELLER_ID);
    verifyNoInteractions(auctionSellerInfoLoader);

    verify(auctionSellerInfoCache, never())
        .put(
            anyLong(),
            any(AuctionSellerInfoResponse.class)
        );
  }

  @Test
  @DisplayName("캐시 MISS이면 DB를 조회하고 결과를 캐시에 저장한다")
  void miss_loadsDatabase_andStoresCache() {
    // given
    AuctionSellerInfoData sellerData =
        createSellerData(SELLER_ID);

    AuctionSellerInfoCachedData cachedData =
        new AuctionSellerInfoCachedData(
            CacheDataStatus.MISS,
            null
        );

    given(auctionSellerInfoCache.get(SELLER_ID))
        .willReturn(cachedData);

    given(auctionSellerInfoLoader.loadAuctionSellerInfo(SELLER_ID))
        .willReturn(sellerData);

    given(auctionSellerInfoCache.put(
        eq(SELLER_ID),
        any(AuctionSellerInfoResponse.class)
    )).willReturn(true);

    // when
    AuctionSellerInfoResponse result =
        auctionSellerInfoService.getAuctionSellerInfo(SELLER_ID);

    // then
    assertThat(result).isNotNull();

    verify(auctionSellerInfoCache).get(SELLER_ID);

    verify(auctionSellerInfoLoader)
        .loadAuctionSellerInfo(SELLER_ID);

    verify(auctionSellerInfoCache)
        .put(eq(SELLER_ID), same(result));
  }

  @Test
  @DisplayName("Redis UNAVAILABLE이면 DB를 조회하지만 캐시에 저장하지 않는다")
  void unavailable_loadsDatabase_withoutCacheStore() {
    // given
    AuctionSellerInfoData sellerData =
        createSellerData(SELLER_ID);

    AuctionSellerInfoCachedData cachedData =
        new AuctionSellerInfoCachedData(
            CacheDataStatus.UNAVAILABLE,
            null
        );

    given(auctionSellerInfoCache.get(SELLER_ID))
        .willReturn(cachedData);

    given(auctionSellerInfoLoader.loadAuctionSellerInfo(SELLER_ID))
        .willReturn(sellerData);

    // when
    AuctionSellerInfoResponse result =
        auctionSellerInfoService.getAuctionSellerInfo(SELLER_ID);

    // then
    assertThat(result).isNotNull();

    verify(auctionSellerInfoCache).get(SELLER_ID);

    verify(auctionSellerInfoLoader)
        .loadAuctionSellerInfo(SELLER_ID);

    verify(auctionSellerInfoCache, never())
        .put(
            anyLong(),
            any(AuctionSellerInfoResponse.class)
        );
  }

  @Test
  @DisplayName("MISS 이후 캐시 저장이 실패해도 DB 조회 결과를 반환한다")
  void cachePutFailure_stillReturnsDatabaseResponse() {
    // given
    AuctionSellerInfoData sellerData =
        createSellerData(SELLER_ID);

    AuctionSellerInfoCachedData cachedData =
        new AuctionSellerInfoCachedData(
            CacheDataStatus.MISS,
            null
        );

    given(auctionSellerInfoCache.get(SELLER_ID))
        .willReturn(cachedData);

    given(auctionSellerInfoLoader.loadAuctionSellerInfo(SELLER_ID))
        .willReturn(sellerData);

    given(auctionSellerInfoCache.put(
        eq(SELLER_ID),
        any(AuctionSellerInfoResponse.class)
    )).willReturn(false);

    // when
    AuctionSellerInfoResponse result =
        auctionSellerInfoService.getAuctionSellerInfo(SELLER_ID);

    // then
    assertThat(result).isNotNull();

    verify(auctionSellerInfoLoader)
        .loadAuctionSellerInfo(SELLER_ID);

    verify(auctionSellerInfoCache)
        .put(eq(SELLER_ID), same(result));
  }

  /**
   * 서비스의 DTO 조립 로직이 실행될 수 있도록 최소 데이터만 구성한다.
   *
   * 빈 경매와 빈 리뷰 목록을 사용하므로 AuctionImageRaw,
   * SellerAuctionsRaw 등의 실제 Fixture는 필요하지 않다.
   */
  private AuctionSellerInfoData createSellerData(Long sellerId) {
    AuctionSellerInfoData sellerData =
        mock(AuctionSellerInfoData.class, RETURNS_DEEP_STUBS);

    given(sellerData.seller().getId())
        .willReturn(sellerId);

    given(sellerData.auctionImages())
        .willReturn(List.of());

    given(sellerData.sellerAuctionsRaws())
        .willReturn(List.of());

    given(sellerData.buyerReviewInfo())
        .willReturn(List.of());

    /*
     * reviewStats()와 seller()의 나머지 메서드는
     * RETURNS_DEEP_STUBS가 기본값을 반환한다.
     */
    return sellerData;
  }
}