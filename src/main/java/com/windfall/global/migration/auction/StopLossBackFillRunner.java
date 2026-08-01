package com.windfall.global.migration.auction;

import com.windfall.global.migration.auction.repository.StopLossBackFillRepo;
import com.windfall.global.migration.auction.service.StopLossBackFillService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@Profile("migration")
@RequiredArgsConstructor
@Slf4j
public class StopLossBackFillRunner implements CommandLineRunner {

  private final StopLossBackFillService stopLossBackFillService;
  private final StopLossBackFillRepo stopLossBackFillRepo;
  private static final int CHUNK_SIZE = 250;
  private static final int SLEEP_INTERVAL = 300;

  @Override //기존 데이터 migration 작업 실행 프로시저
  public void run(String... args) throws Exception {

    int page = 1;

    while(true){
      log.info("{} 번째 페이지 변환 작업", page);
      List<Long> auctionIds = stopLossBackFillRepo.findAllProcessAuctionIds(Pageable.ofSize(CHUNK_SIZE));
      if(auctionIds.isEmpty()){
        break;
      }

      int updated = stopLossBackFillService.updateChunks(auctionIds);
      page++;

      log.info("{} 번째 페이지 변환 작업 완료 :: updated row: {}", page, updated);
      Thread.sleep(SLEEP_INTERVAL); //운영 상황을 고려해 row lock 경합 최소화
    }
  }
}
