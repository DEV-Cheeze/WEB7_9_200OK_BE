package com.windfall.api.auction.service;

import com.windfall.api.auction.dto.AuctionSellerInfoCachedData;
import com.windfall.api.auction.dto.AuctionSellerInfoData;
import com.windfall.api.auction.dto.response.AuctionSellerInfoResponse;
import com.windfall.api.auction.dto.response.info.BuyerReviewInfo;
import com.windfall.api.auction.dto.response.info.SellerAuctionsInfo;
import com.windfall.api.auction.dto.response.raw.SellerAuctionsRaw;
import com.windfall.api.auction.dto.response.stats.SellerReviewStats;
import com.windfall.api.user.dto.response.reviewlist.AuctionImageRaw;
import com.windfall.domain.auction.entity.AuctionImage;
import com.windfall.domain.auction.repository.AuctionImageRepository;
import com.windfall.domain.auction.repository.AuctionRepository;
import com.windfall.domain.auction.repository.AuctionSellerInfoRepository;
import com.windfall.domain.user.entity.User;
import com.windfall.domain.user.repository.UserRepository;
import com.windfall.global.exception.ErrorCode;
import com.windfall.global.exception.ErrorException;
import com.windfall.global.redis.enums.CacheDataStatus;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuctionSellerInfoService {

  private final AuctionSellerInfoLoader auctionSellerInfoLoader;
  private final AuctionSellerInfoCache auctionSellerInfoCache;

  public AuctionSellerInfoResponse getAuctionSellerInfo(Long sellerId){
    return getOrLoad(sellerId);
  }

  private AuctionSellerInfoResponse loadAndCache(Long sellerId, CacheDataStatus status){ //MISS거나, UNAVAILABLE일 경우
    AuctionSellerInfoData sellerData = auctionSellerInfoLoader.loadAuctionSellerInfo(sellerId);

    User seller = sellerData.seller();
    List<AuctionImageRaw> auctionImages = sellerData.auctionImages();
    List<SellerAuctionsRaw> sellerAuctionsRaws = sellerData.sellerAuctionsRaws();
    List<BuyerReviewInfo> buyerReviewInfo = sellerData.buyerReviewInfo();
    SellerReviewStats reviewStats = sellerData.reviewStats();

    Map<Long, String> mappingImage = auctionImages.stream().collect(Collectors.toMap(
        AuctionImageRaw::auctionId, //매핑용 이미지 설정
        AuctionImageRaw::auctionImageUrl));

    List<SellerAuctionsInfo> sellerAuctionsInfo = sellerAuctionsRaws.stream().map(data -> SellerAuctionsInfo.of(data, mappingImage.get(data.auctionId()))).toList(); //순서대로 DTO 변환

    AuctionSellerInfoResponse response = AuctionSellerInfoResponse.of(seller, reviewStats, buyerReviewInfo, sellerAuctionsInfo);

    if(status.equals(CacheDataStatus.MISS)) auctionSellerInfoCache.put(seller.getId(), response);

    return response;
  }

  private AuctionSellerInfoResponse getOrLoad(Long sellerId){

    AuctionSellerInfoCachedData data = auctionSellerInfoCache.get(sellerId);

    if(data.status().equals(CacheDataStatus.HIT)){
      return data.response();
    }

    return loadAndCache(sellerId, data.status());
  }
}


//Cache Miss 일 때
//DB 조회
//JSON 만들고
//직렬화 해서 바이트코드로 저장



//Cache Hit 일 때
//역직렬화해서 JSON으로 변환

