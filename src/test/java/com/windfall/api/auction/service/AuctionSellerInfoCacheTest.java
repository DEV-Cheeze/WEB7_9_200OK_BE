package com.windfall.api.auction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.windfall.api.auction.dto.AuctionSellerInfoCachedData;
import com.windfall.api.auction.dto.response.AuctionSellerInfoResponse;
import com.windfall.global.redis.enums.CacheDataStatus;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class AuctionSellerInfoCacheTest {
  private static final Long SELLER_ID = 1L;

  private static final String CACHE_KEY =
      "windfall:local:cache:auction-seller-info:v1:1";

  private static final String CACHE_JSON =
      "{\"sellerId\":1}";

  @Mock
  private RedisTemplate<String, String> redisTemplate;

  @Mock
  private ValueOperations<String, String> valueOperations;

  @Mock
  private ObjectMapper objectMapper;

  @InjectMocks
  private AuctionSellerInfoCache auctionSellerInfoCache;

  @Test
  @DisplayName("Redis에 데이터가 없으면 MISS를 반환한다")
  void absentCache_returnsMiss() {
    // given
    given(redisTemplate.opsForValue())
        .willReturn(valueOperations);

    given(valueOperations.get(CACHE_KEY))
        .willReturn(null);

    // when
    AuctionSellerInfoCachedData result =
        auctionSellerInfoCache.get(SELLER_ID);

    // then
    assertThat(result.status())
        .isEqualTo(CacheDataStatus.MISS);

    assertThat(result.response()).isNull();

    verify(redisTemplate.opsForValue())
        .get(CACHE_KEY);

    verifyNoInteractions(objectMapper);
  }

  @Test
  @DisplayName("Redis 데이터가 존재하고 역직렬화에 성공하면 HIT를 반환한다")
  void existingCache_returnsHit() throws Exception {
    // given
    AuctionSellerInfoResponse expectedResponse =
        mock(AuctionSellerInfoResponse.class);

    given(redisTemplate.opsForValue())
        .willReturn(valueOperations);

    given(valueOperations.get(CACHE_KEY))
        .willReturn(CACHE_JSON);

    given(objectMapper.readValue(
        CACHE_JSON,
        AuctionSellerInfoResponse.class
    )).willReturn(expectedResponse);

    // when
    AuctionSellerInfoCachedData result =
        auctionSellerInfoCache.get(SELLER_ID);

    // then
    assertThat(result.status())
        .isEqualTo(CacheDataStatus.HIT);

    assertThat(result.response())
        .isSameAs(expectedResponse);
  }

  @Test
  @DisplayName("Redis 조회 실패 시 UNAVAILABLE을 반환한다")
  void redisGetFailure_returnsUnavailable() {
    // given
    given(redisTemplate.opsForValue())
        .willReturn(valueOperations);

    given(valueOperations.get(CACHE_KEY))
        .willThrow(
            new RedisConnectionFailureException(
                "Redis 연결 실패"
            )
        );

    // when
    AuctionSellerInfoCachedData result =
        auctionSellerInfoCache.get(SELLER_ID);

    // then
    assertThat(result.status())
        .isEqualTo(CacheDataStatus.UNAVAILABLE);

    assertThat(result.response()).isNull();

    verifyNoInteractions(objectMapper);
  }

  @Test
  @DisplayName("역직렬화 실패 시 캐시를 삭제하고 MISS를 반환한다")
  void deserializationFailure_deletesCache_andReturnsMiss()
      throws Exception {

    // given
    given(redisTemplate.opsForValue())
        .willReturn(valueOperations);

    given(valueOperations.get(CACHE_KEY))
        .willReturn("invalid-json");

    given(objectMapper.readValue(
        "invalid-json",
        AuctionSellerInfoResponse.class
    )).willThrow(
        new JsonProcessingException("역직렬화 실패") {}
    );

    // when
    AuctionSellerInfoCachedData result =
        auctionSellerInfoCache.get(SELLER_ID);

    // then
    assertThat(result.status())
        .isEqualTo(CacheDataStatus.MISS);

    assertThat(result.response()).isNull();

    verify(redisTemplate).delete(CACHE_KEY);
  }

  @Test
  @DisplayName("정상 저장 시 TTL과 함께 Redis SET을 실행하고 true를 반환한다")
  void putSuccess_setsJsonWithTtl_andReturnsTrue()
      throws Exception {

    // given
    AuctionSellerInfoResponse response =
        mock(AuctionSellerInfoResponse.class);

    given(objectMapper.writeValueAsString(response))
        .willReturn(CACHE_JSON);

    given(redisTemplate.opsForValue())
        .willReturn(valueOperations);

    ArgumentCaptor<Duration> ttlCaptor =
        ArgumentCaptor.forClass(Duration.class);

    // when
    boolean result =
        auctionSellerInfoCache.put(SELLER_ID, response);

    // then
    assertThat(result).isTrue();

    verify(valueOperations).set(
        eq(CACHE_KEY),
        eq(CACHE_JSON),
        ttlCaptor.capture()
    );

    /*
     * 현재 구현은 BASE 300초에 0~30초를 더하므로
     * 실제 TTL 범위는 300~330초다.
     */
    assertThat(ttlCaptor.getValue().getSeconds())
        .isBetween(300L, 330L);
  }

  @Test
  @DisplayName("직렬화 실패 시 Redis에 저장하지 않고 false를 반환한다")
  void serializationFailure_returnsFalse_withoutRedisSet()
      throws Exception {

    // given
    AuctionSellerInfoResponse response =
        mock(AuctionSellerInfoResponse.class);

    given(objectMapper.writeValueAsString(response))
        .willThrow(
            new JsonProcessingException("직렬화 실패") {}
        );

    // when
    boolean result =
        auctionSellerInfoCache.put(SELLER_ID, response);

    // then
    assertThat(result).isFalse();

    verify(redisTemplate, never()).opsForValue();
    verifyNoInteractions(valueOperations);
  }

  @Test
  @DisplayName("Redis SET 실패 시 예외를 전파하지 않고 false를 반환한다")
  void redisSetFailure_returnsFalse() throws Exception {
    // given
    AuctionSellerInfoResponse response =
        mock(AuctionSellerInfoResponse.class);

    given(objectMapper.writeValueAsString(response))
        .willReturn(CACHE_JSON);

    given(redisTemplate.opsForValue())
        .willReturn(valueOperations);

    doThrow(
        new RedisConnectionFailureException(
            "Redis 연결 실패"
        )
    ).when(valueOperations).set(
        eq(CACHE_KEY),
        eq(CACHE_JSON),
        any(Duration.class)
    );

    // when
    boolean result =
        auctionSellerInfoCache.put(SELLER_ID, response);

    // then
    assertThat(result).isFalse();
  }
}