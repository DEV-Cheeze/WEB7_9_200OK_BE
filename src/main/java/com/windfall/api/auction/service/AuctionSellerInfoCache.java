package com.windfall.api.auction.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.windfall.api.auction.dto.AuctionSellerInfoCachedData;
import com.windfall.api.auction.dto.response.AuctionSellerInfoResponse;
import com.windfall.global.redis.enums.CacheDataStatus;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class AuctionSellerInfoCache {

  private final RedisTemplate<String, String> redisTemplate;
  private final ObjectMapper objectMapper;
  private static final String KEY_PREFIX = "windfall:local:cache:auction-seller-info:v1:";

  private static final long BASE_TTL_SECONDS = 300;
  private static final int TTL_JITTER_SECONDS_PERCENTS = 10; //Cache Avalanche 방지


  public AuctionSellerInfoCachedData get(Long sellerId){

    String key = createKey(sellerId);

    try {
      String json = redisTemplate.opsForValue().get(key);

      if(json == null){
        return new AuctionSellerInfoCachedData(CacheDataStatus.MISS, null);
      }

      return new AuctionSellerInfoCachedData(CacheDataStatus.HIT,  objectMapper.readValue(json, AuctionSellerInfoResponse.class));

    }catch (JsonProcessingException e){ //역직렬화 실패 시 => 잘못된 캐시 형태로 존재 => 캐시 삭제
      log.warn("캐시 역직렬화 실패. key={}", key, e);
      delete(key);
      return new AuctionSellerInfoCachedData(CacheDataStatus.MISS, null);
    }catch (DataAccessException e){ //Redis 연결 실패
      log.warn("Redis 캐시 조회 실패. key={}", key, e);
      return new AuctionSellerInfoCachedData(CacheDataStatus.UNAVAILABLE, null);
    }
  }

  public boolean put(Long sellerId, AuctionSellerInfoResponse response){

    String key = createKey(sellerId);

    try {
      String json = objectMapper.writeValueAsString(response);
      Duration ttl = createTTL();

      redisTemplate.opsForValue().set(key, json, ttl);

      return true;
    }catch (JsonProcessingException | DataAccessException e){
      log.warn("Redis 캐시 저장 실패. key={}", key, e);
      return false;
    }
  }

  private boolean delete(String key){
    try{
      redisTemplate.delete(key);
      return true;
    }catch (DataAccessException e){
      log.error("Redis 캐시 삭제 실패. key={}", key, e);
      return false;
    }

  }

  private String createKey(Long sellerId){
    return KEY_PREFIX + sellerId;
  }

  private Duration createTTL(){

    long percent = (BASE_TTL_SECONDS * TTL_JITTER_SECONDS_PERCENTS) / 100;

    long jitter = ThreadLocalRandom.current()
        .nextLong(percent + 1);

    return Duration.ofSeconds(BASE_TTL_SECONDS + jitter);
  }

  private void evict(Long sellerId){
    String key = createKey(sellerId);
    redisTemplate.delete(key);
  }
}
