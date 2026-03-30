package com.ebikes.iam.database.specifications;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.ebikes.iam.database.entities.UserExtension;
import com.ebikes.iam.database.repositories.UserExtensionRepository;
import com.ebikes.iam.dtos.requests.filters.UserExtensionFilter;
import com.ebikes.iam.enums.UserRole;
import com.ebikes.iam.enums.UserStatus;
import com.ebikes.iam.support.context.ExecutionContext;
import com.ebikes.iam.support.fixtures.UserExtensionFixtures;
import com.ebikes.iam.support.infrastructure.AbstractRepositoryTest;

@DisplayName("UserExtensionSpecifications")
class UserExtensionSpecificationsTest extends AbstractRepositoryTest {

  @Autowired private UserExtensionRepository repository;

  private static final String ORGANIZATION_A = UUID.randomUUID().toString();
  private static final String ORGANIZATION_B = UUID.randomUUID().toString();
  private static final String BRANCH_A = UUID.randomUUID().toString();

  @BeforeEach
  void setUpContext() {
    ExecutionContext.set(
        UUID.randomUUID().toString(),
        ORGANIZATION_A,
        null,
        "test@ebikes.test",
        Set.of(),
        null,
        Set.of(UserRole.ORGANIZATION_ADMIN.name()));
  }

  @AfterEach
  void clearContext() {
    ExecutionContext.clear();
  }

  @Nested
  @DisplayName("Authorization scoping")
  class AuthorizationScoping {

    @Test
    @DisplayName("org admin only sees user extensions belonging to their organization")
    void orgAdminOnlySeesOwnOrganizationUsers() {
      repository.saveAll(
          List.of(
              UserExtensionFixtures.withOrganization(ORGANIZATION_A),
              UserExtensionFixtures.withOrganization(ORGANIZATION_A),
              UserExtensionFixtures.withOrganization(ORGANIZATION_B)));

      List<UserExtension> results =
          repository.findAll(
              UserExtensionSpecifications.buildSpecification(new UserExtensionFilter()));

      assertThat(results).hasSize(2).allMatch(u -> u.getOrganizationId().equals(ORGANIZATION_A));
    }
  }

  @Nested
  @DisplayName("Filter by status")
  class FilterByStatus {

    @BeforeEach
    void setUp() {
      repository.saveAll(
          List.of(
              UserExtensionFixtures.active(ORGANIZATION_A),
              UserExtensionFixtures.active(ORGANIZATION_A),
              UserExtensionFixtures.inactive(ORGANIZATION_A)));
    }

    @Test
    @DisplayName("should return only ACTIVE users")
    void shouldReturnOnlyActiveUsers() {
      UserExtensionFilter filter = new UserExtensionFilter();
      filter.setStatus(UserStatus.ACTIVE);

      List<UserExtension> results =
          repository.findAll(UserExtensionSpecifications.buildSpecification(filter));

      assertThat(results).hasSize(2).allMatch(u -> u.getStatus() == UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("should return only INACTIVE users")
    void shouldReturnOnlyInactiveUsers() {
      UserExtensionFilter filter = new UserExtensionFilter();
      filter.setStatus(UserStatus.INACTIVE);

      List<UserExtension> results =
          repository.findAll(UserExtensionSpecifications.buildSpecification(filter));

      assertThat(results).hasSize(1).allMatch(u -> u.getStatus() == UserStatus.INACTIVE);
    }
  }

  @Nested
  @DisplayName("Filter by email (partial match)")
  class FilterByEmail {

    @Test
    @DisplayName("should return users with email matching partial search term")
    void shouldReturnUsersWithEmailMatchingPartial() {
      UserExtension saved = repository.save(UserExtensionFixtures.active(ORGANIZATION_A));
      repository.save(UserExtensionFixtures.active(ORGANIZATION_A));

      // Use a substring from the middle of the email to verify LIKE behaviour
      String partial = saved.getEmail().substring(2, 8);
      UserExtensionFilter filter = new UserExtensionFilter();
      filter.setEmail(partial);

      List<UserExtension> results =
          repository.findAll(UserExtensionSpecifications.buildSpecification(filter));

      assertThat(results)
          .isNotEmpty()
          .allMatch(u -> u.getEmail().toLowerCase().contains(partial.toLowerCase()));
    }

    @Test
    @DisplayName("should return nothing for unknown email fragment")
    void shouldReturnNothingForUnknownEmail() {
      repository.save(UserExtensionFixtures.active(ORGANIZATION_A));

      UserExtensionFilter filter = new UserExtensionFilter();
      filter.setEmail("zzznomatch@nowhere.invalid");

      List<UserExtension> results =
          repository.findAll(UserExtensionSpecifications.buildSpecification(filter));

      assertThat(results).isEmpty();
    }
  }

  @Nested
  @DisplayName("Filter by first name (partial match)")
  class FilterByFirstName {

    @Test
    @DisplayName("should return users with matching first name fragment")
    void shouldReturnUsersMatchingFirstNameFragment() {
      UserExtension saved = repository.save(UserExtensionFixtures.active(ORGANIZATION_A));
      repository.save(UserExtensionFixtures.active(ORGANIZATION_A));

      String partial = saved.getFirstName().substring(0, 2);
      UserExtensionFilter filter = new UserExtensionFilter();
      filter.setFirstName(partial);

      List<UserExtension> results =
          repository.findAll(UserExtensionSpecifications.buildSpecification(filter));

      assertThat(results)
          .isNotEmpty()
          .allMatch(u -> u.getFirstName().toLowerCase().contains(partial.toLowerCase()));
    }
  }

  @Nested
  @DisplayName("Filter by last name (partial match)")
  class FilterByLastName {

    @Test
    @DisplayName("should return users with matching last name fragment")
    void shouldReturnUsersMatchingLastNameFragment() {
      UserExtension saved = repository.save(UserExtensionFixtures.active(ORGANIZATION_A));
      repository.save(UserExtensionFixtures.active(ORGANIZATION_A));

      String partial = saved.getLastName().substring(0, 2);
      UserExtensionFilter filter = new UserExtensionFilter();
      filter.setLastName(partial);

      List<UserExtension> results =
          repository.findAll(UserExtensionSpecifications.buildSpecification(filter));

      assertThat(results)
          .isNotEmpty()
          .allMatch(u -> u.getLastName().toLowerCase().contains(partial.toLowerCase()));
    }
  }

  @Nested
  @DisplayName("Filter by country code")
  class FilterByCountryCode {

    @Test
    @DisplayName("should return only users with matching country code (case-insensitive)")
    void shouldReturnUsersWithMatchingCountryCode() {
      repository.saveAll(
          List.of(
              UserExtensionFixtures.active(ORGANIZATION_A), // countryCode = KE
              UserExtensionFixtures.withCountryCode(ORGANIZATION_A, "UG")));

      UserExtensionFilter filter = new UserExtensionFilter();
      filter.setCountryCode("ke");

      List<UserExtension> results =
          repository.findAll(UserExtensionSpecifications.buildSpecification(filter));

      assertThat(results).hasSize(1).allMatch(u -> u.getCountryCode().equalsIgnoreCase("KE"));
    }
  }

  @Nested
  @DisplayName("Filter by keycloak user ID")
  class FilterByKeycloakUserId {

    @Test
    @DisplayName("should return only the user with matching keycloak ID")
    void shouldReturnUserWithMatchingKeycloakId() {
      String keycloakId = UUID.randomUUID().toString();
      repository.saveAll(
          List.of(
              UserExtensionFixtures.withKeycloakId(ORGANIZATION_A, keycloakId),
              UserExtensionFixtures.active(ORGANIZATION_A)));

      UserExtensionFilter filter = new UserExtensionFilter();
      filter.setKeycloakUserId(keycloakId);

      List<UserExtension> results =
          repository.findAll(UserExtensionSpecifications.buildSpecification(filter));

      assertThat(results)
          .hasSize(1)
          .first()
          .satisfies(u -> assertThat(u.getKeycloakUserId()).isEqualTo(keycloakId));
    }
  }

  @Nested
  @DisplayName("Filter by branch")
  class FilterByBranch {

    @Test
    @DisplayName("should return only users belonging to the specified branch")
    void shouldReturnOnlyUsersForBranch() {
      repository.saveAll(
          List.of(
              UserExtensionFixtures.withBranch(ORGANIZATION_A, BRANCH_A),
              UserExtensionFixtures.withBranch(ORGANIZATION_A, BRANCH_A),
              UserExtensionFixtures.active(ORGANIZATION_A)));

      UserExtensionFilter filter = new UserExtensionFilter();
      filter.setBranchId(BRANCH_A);

      List<UserExtension> results =
          repository.findAll(UserExtensionSpecifications.buildSpecification(filter));

      assertThat(results).hasSize(2).allMatch(u -> BRANCH_A.equals(u.getBranchId()));
    }
  }

  @Nested
  @DisplayName("Filter by email verified")
  class FilterByEmailVerified {

    @Test
    @DisplayName("should return only users with verified email")
    void shouldReturnOnlyUsersWithVerifiedEmail() {
      repository.saveAll(
          List.of(
              UserExtensionFixtures.active(ORGANIZATION_A), // emailVerified = true
              UserExtensionFixtures.inactive(ORGANIZATION_A))); // emailVerified = false

      UserExtensionFilter filter = new UserExtensionFilter();
      filter.setEmailVerified(true);

      List<UserExtension> results =
          repository.findAll(UserExtensionSpecifications.buildSpecification(filter));

      assertThat(results).hasSize(1).allMatch(UserExtension::isEmailVerified);
    }
  }

  @Nested
  @DisplayName("Filter by phone number verified")
  class FilterByPhoneNumberVerified {

    @Test
    @DisplayName("should return only users with verified phone number")
    void shouldReturnOnlyUsersWithVerifiedPhone() {
      repository.saveAll(
          List.of(
              UserExtensionFixtures.withVerifiedPhone(ORGANIZATION_A),
              UserExtensionFixtures.active(ORGANIZATION_A)));

      UserExtensionFilter filter = new UserExtensionFilter();
      filter.setPhoneNumberVerified(true);

      List<UserExtension> results =
          repository.findAll(UserExtensionSpecifications.buildSpecification(filter));

      assertThat(results).hasSize(1).allMatch(UserExtension::isPhoneNumberVerified);
    }
  }

  @Nested
  @DisplayName("Filter by created date range")
  class FilterByCreatedDateRange {

    @BeforeEach
    void setUp() {
      repository.save(UserExtensionFixtures.active(ORGANIZATION_A));
    }

    @Test
    @DisplayName("createdAtFrom filters out records created before the threshold")
    void createdAtFromFiltersOldRecords() {
      UserExtensionFilter filter = new UserExtensionFilter();
      filter.setCreatedAtFrom(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(1));

      List<UserExtension> results =
          repository.findAll(UserExtensionSpecifications.buildSpecification(filter));

      assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("createdAtTo filters out records created after the threshold")
    void createdAtToFiltersNewRecords() {
      UserExtensionFilter filter = new UserExtensionFilter();
      filter.setCreatedAtTo(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1));

      List<UserExtension> results =
          repository.findAll(UserExtensionSpecifications.buildSpecification(filter));

      assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("createdAtFrom and createdAtTo combined returns records within range")
    void createdAtRangeReturnsRecordsWithinRange() {
      UserExtensionFilter filter = new UserExtensionFilter();
      filter.setCreatedAtFrom(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1));
      filter.setCreatedAtTo(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(1));

      List<UserExtension> results =
          repository.findAll(UserExtensionSpecifications.buildSpecification(filter));

      assertThat(results).hasSize(1);
    }
  }

  @Nested
  @DisplayName("Combined filters")
  class CombinedFilters {

    @Test
    @DisplayName("status and emailVerified combined returns correct subset")
    void statusAndEmailVerifiedCombinedReturnsCorrectSubset() {
      repository.saveAll(
          List.of(
              UserExtensionFixtures.active(ORGANIZATION_A), // ACTIVE, emailVerified=true
              UserExtensionFixtures.inactive(ORGANIZATION_A), // INACTIVE, emailVerified=false
              UserExtensionFixtures.active(ORGANIZATION_A))); // ACTIVE, emailVerified=true

      UserExtensionFilter filter = new UserExtensionFilter();
      filter.setStatus(UserStatus.ACTIVE);
      filter.setEmailVerified(true);

      List<UserExtension> results =
          repository.findAll(UserExtensionSpecifications.buildSpecification(filter));

      assertThat(results)
          .hasSize(2)
          .allMatch(u -> u.getStatus() == UserStatus.ACTIVE && u.isEmailVerified());
    }
  }
}
