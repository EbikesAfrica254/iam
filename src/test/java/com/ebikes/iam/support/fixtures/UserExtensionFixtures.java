package com.ebikes.iam.support.fixtures;

import java.util.UUID;

import com.ebikes.iam.database.entities.UserExtension;
import com.ebikes.iam.enums.UserStatus;

import net.datafaker.Faker;

public final class UserExtensionFixtures {

  private static final Faker FAKER = new Faker();

  private UserExtensionFixtures() {}

  public static UserExtension active() {
    return base(UUID.randomUUID().toString()).status(UserStatus.ACTIVE).emailVerified(true).build();
  }

  public static UserExtension inactive() {
    return base(UUID.randomUUID().toString())
        .status(UserStatus.INACTIVE)
        .emailVerified(false)
        .build();
  }

  public static UserExtension active(String organizationId) {
    return base(organizationId).status(UserStatus.ACTIVE).emailVerified(true).build();
  }

  public static UserExtension inactive(String organizationId) {
    return base(organizationId).status(UserStatus.INACTIVE).emailVerified(false).build();
  }

  public static UserExtension withKeycloakId(String keycloakUserId) {
    return base(UUID.randomUUID().toString())
        .keycloakUserId(keycloakUserId)
        .status(UserStatus.ACTIVE)
        .emailVerified(true)
        .build();
  }

  public static UserExtension withKeycloakId(String organizationId, String keycloakUserId) {
    return base(organizationId)
        .keycloakUserId(keycloakUserId)
        .status(UserStatus.ACTIVE)
        .emailVerified(true)
        .build();
  }

  public static UserExtension withOrganization(String organizationId) {
    return base(organizationId).status(UserStatus.ACTIVE).emailVerified(true).build();
  }

  public static UserExtension withBranch(String organizationId, String branchId) {
    return base(organizationId)
        .branchId(branchId)
        .status(UserStatus.ACTIVE)
        .emailVerified(true)
        .build();
  }

  public static UserExtension withCountryCode(String organizationId, String countryCode) {
    return base(organizationId)
        .countryCode(countryCode)
        .status(UserStatus.ACTIVE)
        .emailVerified(true)
        .build();
  }

  public static UserExtension withVerifiedPhone(String organizationId) {
    return base(organizationId)
        .status(UserStatus.ACTIVE)
        .emailVerified(true)
        .phoneNumberVerified(true)
        .build();
  }

  public static UserExtension persisted() {
    return persistedBase(UUID.randomUUID().toString()).build();
  }

  private static UserExtension.UserExtensionBuilder<?, ?> persistedBase(String organizationId) {
    return base(organizationId).id(UUID.randomUUID()).version(0L);
  }

  private static UserExtension.UserExtensionBuilder<?, ?> base(String organizationId) {
    return UserExtension.builder()
        .keycloakUserId(UUID.randomUUID().toString())
        .organizationId(organizationId)
        .email(FAKER.internet().emailAddress())
        .firstName(FAKER.name().firstName())
        .lastName(FAKER.name().lastName())
        .username(FAKER.credentials().username())
        .phoneNumber("+254" + FAKER.number().digits(9))
        .countryCode("KE")
        .status(UserStatus.ACTIVE)
        .emailVerified(false)
        .phoneNumberVerified(false);
  }
}
