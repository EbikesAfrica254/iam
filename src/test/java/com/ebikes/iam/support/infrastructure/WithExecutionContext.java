package com.ebikes.iam.support.infrastructure;

import java.util.Set;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.ebikes.iam.support.context.ExecutionContext;
import com.ebikes.iam.support.fixtures.SecurityFixtures;

public class WithExecutionContext implements BeforeEachCallback, AfterEachCallback {

  @Override
  public void beforeEach(ExtensionContext context) {
    ExecutionContext.set(
        SecurityFixtures.TEST_USER_ID,
        SecurityFixtures.TEST_ORGANIZATION_ID,
        null,
        SecurityFixtures.TEST_EMAIL,
        Set.of(),
        SecurityFixtures.TEST_PHONE_NUMBER,
        Set.of());
  }

  @Override
  public void afterEach(ExtensionContext context) {
    ExecutionContext.clear();
  }
}
