package com.ebikes.iam.seed.runners;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

import com.ebikes.iam.seed.SystemAdminSeedService;

@ExtendWith(MockitoExtension.class)
class SystemAdminSeedRunnerTest {

  @Mock private SystemAdminSeedService systemAdminSeedService;
  @Mock private ApplicationArguments args;

  @InjectMocks private SystemAdminSeedRunner runner;

  @Test
  @DisplayName("should delegate to SystemAdminSeedService to seed system admin if absent")
  void runDelegatesToSeedService() {
    runner.run(args);

    verify(systemAdminSeedService).seedIfAbsent();
  }
}
