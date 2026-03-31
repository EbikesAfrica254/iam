package com.ebikes.iam.seed.runners;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.ebikes.iam.seed.SystemAdminSeedService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class SystemAdminSeedRunner implements ApplicationRunner {

  private final SystemAdminSeedService systemAdminSeedService;

  @Override
  public void run(@NonNull ApplicationArguments args) {
    log.info("Running system admin seed check");
    systemAdminSeedService.seedIfAbsent();
  }
}
