package com.windfall.api.user.service;

import com.windfall.api.user.dto.request.UpdateUsernameRequest;
import com.windfall.api.user.dto.response.UpdateUsernameResponse;
import com.windfall.api.user.dto.response.UserInfoResponse;
import com.windfall.api.user.dto.response.UpdateUserProfileImageResponse;
import com.windfall.api.user.dto.response.reviewlist.AuctionImageRaw;
import com.windfall.api.user.dto.response.reviewlist.ReviewListRaw;
import com.windfall.api.user.dto.response.reviewlist.ReviewListResponse;
import com.windfall.api.user.dto.response.saleshistory.SalesHistoryResponse;
import com.windfall.api.user.dto.response.saleshistory.projections.SalesHistoryQueryResult;
import com.windfall.domain.auction.enums.AuctionStatus;
import com.windfall.domain.auction.repository.AuctionImageRepository;
import com.windfall.domain.user.entity.User;
import com.windfall.domain.user.repository.SalesHistoryQueryRepository;
import com.windfall.domain.user.repository.UserInfoRepository;
import com.windfall.global.exception.ErrorCode;
import com.windfall.global.exception.ErrorException;
import com.windfall.global.response.SliceResponse;
import com.windfall.global.s3.S3Uploader;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserInfoService {

  private final UserService userService;
  private final UserInfoRepository userInfoRepository;
  private final SalesHistoryQueryRepository salesHistoryQueryRepository;
  private final AuctionImageRepository auctionImageRepository;
  private final SalesInfoQueryService salesInfoQueryService;
  private final SalesHistoryQueryServiceV3 salesHistoryQueryServiceV3;
  private final SalesHistoryAssembler salesHistoryAssembler;
  private final S3Uploader s3Uploader;

  @Transactional
  public UserInfoResponse getUserInfo(Long userid, Long loginId){ //userDetails 추가될 경우 리팩토링 예정
    userService.getUserById(userid); //조회할 유저가 있는지 먼저 검색

    return userInfoRepository.findByUserInfo(userid, loginId);
  }

  public SliceResponse<SalesHistoryResponse> getUserSalesHistory(Long userid, Long loginId, String filter, Pageable pageable) {
    //userService.getUserById(userid); //조회할 유저가 있는지 먼저 검색

    AuctionStatus status = filter == null ? null : AuctionStatus.valueOf(filter);

    SalesHistoryQueryResult result = salesHistoryQueryServiceV3.fetch(userid, loginId, status, pageable);

    Slice<SalesHistoryResponse> slicedResult = salesHistoryAssembler.assemble(result);

    return SliceResponse.from(slicedResult);
  }

  @Transactional(readOnly = true)
  public SliceResponse<ReviewListResponse> getUserReviewList(Long userId, Pageable pageable){
    userService.getUserById(userId); //조회할 유저가 있는지 먼저 검색

    Slice<ReviewListRaw> rawData = userInfoRepository.getUserReviewList(userId, pageable); //필요 데이터 꺼내오기 (이미지 제외)

    List<Long> auctionIds = rawData.getContent().stream().map(ReviewListRaw::auctionId).toList(); //데이터 내부의 auctionId 추출

    List<AuctionImageRaw> auctionImages = auctionImageRepository.findFirstImagesProjection(auctionIds); //각 경매의 첫 번째 이미지 추출

    Map<Long, String> mappingImage = auctionImages.stream().collect(Collectors.toMap(
        AuctionImageRaw::auctionId, //매핑용 이미지 설정
        AuctionImageRaw::auctionImageUrl));

    List<ReviewListResponse> resultContent = rawData.getContent().stream().map(data -> ReviewListResponse.of(data, mappingImage.get(data.auctionId()))).toList(); //순서대로 DTO 변환

    Slice<ReviewListResponse> resultSlice = toSlice(resultContent, rawData); //Slice 재정의

    return SliceResponse.from(resultSlice);
  }

  @Transactional
  public UpdateUserProfileImageResponse updateUserProfileImage(MultipartFile image, Long loginId){
    User user = userService.getUserById(loginId);

    validateImage(image);

    String oldUrl = user.getProfileImageUrl();
    String dirName = "profile/" + loginId;
    String newUrl = s3Uploader.upload(image, dirName);

    user.updateProfileImage(newUrl); //S3에 새로운 이미지 저장

    if(oldUrl != null){ //S3에 기존 이미지 삭제
      String key = URIPathParser(oldUrl);
      s3Uploader.deleteFile(key);
    }
    return new UpdateUserProfileImageResponse(newUrl);
  }

  @Transactional
  public UpdateUsernameResponse updateUsername(UpdateUsernameRequest request, Long loginId){
    User user = userService.getUserById(loginId);

    user.updateUsername(request.username());

    return new UpdateUsernameResponse(request.username());
  }

  private void validateImage(MultipartFile files) {
    if (files == null || files.isEmpty()) {
      throw new ErrorException(ErrorCode.INVALID_IMAGE_FILE);
    }
  }

  private String URIPathParser(String url){
    URI uri = URI.create(url);
    return uri.getPath().substring(1);
  }

  private <T> Slice<T> toSlice (List<T> content, Slice<?> sliceData){
    return new SliceImpl<>(
        content,
        sliceData.getPageable(),
        sliceData.hasNext());
  }
}
