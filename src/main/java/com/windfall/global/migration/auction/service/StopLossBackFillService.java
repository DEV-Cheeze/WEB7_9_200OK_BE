package com.windfall.global.migration.auction.service;

import com.windfall.global.migration.auction.repository.StopLossBackFillRepo;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StopLossBackFillService {

  //값 계산을 어떻게 하느냐?
  //필요한 하락 횟수 =
  //ceil((시작가 - 유찰가) / 1회 하락 금액)
  //
  //유찰 도달 시간 =
  //시작 시간 + 필요한 하락 횟수 * 하락 주기

  private final StopLossBackFillRepo stopLossBackFillRepo;

  @Transactional
  public int updateChunks(List<Long> ids){
    return stopLossBackFillRepo.backfillStopLossReachedAtForAlreadyReached(ids);
  }
}
