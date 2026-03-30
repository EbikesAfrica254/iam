package com.ebikes.iam.support.fixtures;

import java.util.Set;
import java.util.UUID;

import com.ebikes.iam.dtos.requests.users.CreateUserRequest;
import com.ebikes.iam.dtos.requests.users.SignupRequest;
import com.ebikes.iam.enums.UserRole;

import net.datafaker.Faker;

public final class UserRequestFixtures {

  private static final Faker FAKER = new Faker();

  private UserRequestFixtures() {}

  public static CreateUserRequest createUser() {
    return baseCreateUser(UUID.randomUUID().toString(), Set.of(UserRole.CUSTOMER));
  }

  public static CreateUserRequest createUser(String organizationId) {
    return baseCreateUser(organizationId, Set.of(UserRole.CUSTOMER));
  }

  public static SignupRequest signup() {
    return new SignupRequest(
        "KE",
        FAKER.internet().emailAddress(),
        FAKER.name().firstName(),
        FAKER.name().lastName(),
        "+254" + FAKER.number().digits(9),
        FAKER.credentials().username());
  }

  private static CreateUserRequest baseCreateUser(String organizationId, Set<UserRole> roles) {
    return new CreateUserRequest(
        null,
        "KE",
        FAKER.internet().emailAddress(),
        FAKER.name().firstName(),
        true,
        FAKER.name().lastName(),
        organizationId,
        "+254" + FAKER.number().digits(9),
        roles,
        FAKER.credentials().username());
  }
}
