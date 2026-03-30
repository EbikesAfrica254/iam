package com.ebikes.iam.support.fixtures;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.ebikes.iam.dtos.responses.users.UserExtensionDetailResponse;
import com.ebikes.iam.dtos.responses.users.UserProfileResponse;
import com.ebikes.iam.enums.UserStatus;

import net.datafaker.Faker;

public final class UserExtensionResponseFixtures {

  private static final Faker FAKER = new Faker();

  private UserExtensionResponseFixtures() {}

  public static UserExtensionDetailResponse detail() {
    return new UserExtensionDetailResponse(
        null,
        "KE",
        OffsetDateTime.now(),
        null,
        FAKER.internet().emailAddress(),
        true,
        FAKER.name().firstName(),
        UUID.randomUUID(),
        UUID.randomUUID().toString(),
        FAKER.name().lastName(),
        UUID.randomUUID().toString(),
        "+254" + FAKER.number().digits(9),
        false,
        UserStatus.ACTIVE,
        OffsetDateTime.now(),
        FAKER.credentials().username());
  }

  public static UserProfileResponse profile() {
    return new UserProfileResponse(
        null,
        "KE",
        OffsetDateTime.now(),
        FAKER.internet().emailAddress(),
        true,
        FAKER.name().firstName(),
        UUID.randomUUID(),
        FAKER.name().lastName(),
        List.of(),
        "+254" + FAKER.number().digits(9),
        false,
        UserStatus.ACTIVE,
        FAKER.credentials().username());
  }
}
