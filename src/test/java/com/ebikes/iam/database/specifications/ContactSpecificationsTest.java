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

import com.ebikes.iam.database.entities.Contact;
import com.ebikes.iam.database.repositories.ContactRepository;
import com.ebikes.iam.dtos.requests.filters.ContactFilter;
import com.ebikes.iam.enums.ContactSourceType;
import com.ebikes.iam.enums.ContactStatus;
import com.ebikes.iam.enums.UserRole;
import com.ebikes.iam.support.context.ExecutionContext;
import com.ebikes.iam.support.fixtures.ContactFixtures;
import com.ebikes.iam.support.infrastructure.AbstractRepositoryTest;

@DisplayName("ContactSpecifications")
class ContactSpecificationsTest extends AbstractRepositoryTest {

  @Autowired private ContactRepository repository;

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
    @DisplayName("organization admin only sees contacts belonging to their organization")
    void orgAdminOnlySeesOwnOrganizationContacts() {
      repository.saveAll(
          List.of(
              ContactFixtures.unresolved(ORGANIZATION_A),
              ContactFixtures.unresolved(ORGANIZATION_A),
              ContactFixtures.unresolved(ORGANIZATION_B)));

      List<Contact> results =
          repository.findAll(ContactSpecifications.buildSpecification(new ContactFilter()));

      assertThat(results).hasSize(2).allMatch(c -> c.getOrganizationId().equals(ORGANIZATION_A));
    }
  }

  @Nested
  @DisplayName("Filter by status")
  class FilterByStatus {

    @BeforeEach
    void setUp() {
      repository.saveAll(
          List.of(
              ContactFixtures.unresolved(ORGANIZATION_A),
              ContactFixtures.unresolved(ORGANIZATION_A),
              ContactFixtures.claimed(ORGANIZATION_A, null),
              ContactFixtures.expired(ORGANIZATION_A)));
    }

    @Test
    @DisplayName("should return only UNRESOLVED contacts")
    void shouldReturnOnlyUnresolvedContacts() {
      ContactFilter filter = new ContactFilter();
      filter.setStatus(ContactStatus.UNRESOLVED);

      List<Contact> results = repository.findAll(ContactSpecifications.buildSpecification(filter));

      assertThat(results).hasSize(2).allMatch(c -> c.getStatus() == ContactStatus.UNRESOLVED);
    }

    @Test
    @DisplayName("should return only CLAIMED contacts")
    void shouldReturnOnlyClaimedContacts() {
      ContactFilter filter = new ContactFilter();
      filter.setStatus(ContactStatus.CLAIMED);

      List<Contact> results = repository.findAll(ContactSpecifications.buildSpecification(filter));

      assertThat(results).hasSize(1).allMatch(c -> c.getStatus() == ContactStatus.CLAIMED);
    }

    @Test
    @DisplayName("should return only EXPIRED contacts")
    void shouldReturnOnlyExpiredContacts() {
      ContactFilter filter = new ContactFilter();
      filter.setStatus(ContactStatus.EXPIRED);

      List<Contact> results = repository.findAll(ContactSpecifications.buildSpecification(filter));

      assertThat(results).hasSize(1).allMatch(c -> c.getStatus() == ContactStatus.EXPIRED);
    }
  }

  @Nested
  @DisplayName("Filter by source type")
  class FilterBySourceType {

    @BeforeEach
    void setUp() {
      repository.saveAll(
          List.of(
              ContactFixtures.withSourceType(ORGANIZATION_A, ContactSourceType.CSV_MANIFEST),
              ContactFixtures.withSourceType(ORGANIZATION_A, ContactSourceType.CSV_MANIFEST),
              ContactFixtures.withSourceType(ORGANIZATION_A, ContactSourceType.WHATSAPP)));
    }

    @Test
    @DisplayName("should return only CSV_MANIFEST contacts")
    void shouldReturnOnlyCsvManifestContacts() {
      ContactFilter filter = new ContactFilter();
      filter.setSourceType(ContactSourceType.CSV_MANIFEST);

      List<Contact> results = repository.findAll(ContactSpecifications.buildSpecification(filter));

      assertThat(results)
          .hasSize(2)
          .allMatch(c -> c.getSourceType() == ContactSourceType.CSV_MANIFEST);
    }

    @Test
    @DisplayName("should return only WHATSAPP contacts")
    void shouldReturnOnlyWhatsappContacts() {
      ContactFilter filter = new ContactFilter();
      filter.setSourceType(ContactSourceType.WHATSAPP);

      List<Contact> results = repository.findAll(ContactSpecifications.buildSpecification(filter));

      assertThat(results).hasSize(1).allMatch(c -> c.getSourceType() == ContactSourceType.WHATSAPP);
    }
  }

  @Nested
  @DisplayName("Filter by phone number")
  class FilterByPhoneNumber {

    @Test
    @DisplayName("should return contact matching exact phone number")
    void shouldReturnContactMatchingExactPhoneNumber() {
      Contact saved = repository.save(ContactFixtures.unresolved(ORGANIZATION_A));
      repository.save(ContactFixtures.unresolved(ORGANIZATION_A));

      ContactFilter filter = new ContactFilter();
      filter.setPhoneNumber(saved.getPhoneNumber());

      List<Contact> results = repository.findAll(ContactSpecifications.buildSpecification(filter));

      assertThat(results)
          .hasSize(1)
          .first()
          .satisfies(c -> assertThat(c.getPhoneNumber()).isEqualTo(saved.getPhoneNumber()));
    }

    @Test
    @DisplayName("should return nothing for unknown phone number")
    void shouldReturnNothingForUnknownPhoneNumber() {
      repository.save(ContactFixtures.unresolved(ORGANIZATION_A));

      ContactFilter filter = new ContactFilter();
      filter.setPhoneNumber("+254000000000");

      List<Contact> results = repository.findAll(ContactSpecifications.buildSpecification(filter));

      assertThat(results).isEmpty();
    }
  }

  @Nested
  @DisplayName("Filter by branch")
  class FilterByBranch {

    @Test
    @DisplayName("should return only contacts belonging to the specified branch")
    void shouldReturnOnlyContactsForBranch() {
      repository.saveAll(
          List.of(
              ContactFixtures.withBranch(ORGANIZATION_A, BRANCH_A),
              ContactFixtures.withBranch(ORGANIZATION_A, BRANCH_A),
              ContactFixtures.unresolved(ORGANIZATION_A)));

      ContactFilter filter = new ContactFilter();
      filter.setBranchId(BRANCH_A);

      List<Contact> results = repository.findAll(ContactSpecifications.buildSpecification(filter));

      assertThat(results).hasSize(2).allMatch(c -> BRANCH_A.equals(c.getBranchId()));
    }
  }

  @Nested
  @DisplayName("Filter by source reference")
  class FilterBySourceReference {

    @Test
    @DisplayName("should return contact matching source reference")
    void shouldReturnContactMatchingSourceReference() {
      Contact saved = repository.save(ContactFixtures.unresolved(ORGANIZATION_A));
      repository.save(ContactFixtures.unresolved(ORGANIZATION_A));

      ContactFilter filter = new ContactFilter();
      filter.setSourceReference(saved.getSourceReference());

      List<Contact> results = repository.findAll(ContactSpecifications.buildSpecification(filter));

      assertThat(results)
          .hasSize(1)
          .first()
          .satisfies(c -> assertThat(c.getSourceReference()).isEqualTo(saved.getSourceReference()));
    }
  }

  @Nested
  @DisplayName("Filter by created date range")
  class FilterByCreatedDateRange {

    @BeforeEach
    void setUp() {
      repository.save(ContactFixtures.unresolved(ORGANIZATION_A));
    }

    @Test
    @DisplayName("createdAtFrom filters out records created before the threshold")
    void createdAtFromFiltersOldRecords() {
      ContactFilter filter = new ContactFilter();
      filter.setCreatedAtFrom(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(1));

      List<Contact> results = repository.findAll(ContactSpecifications.buildSpecification(filter));

      assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("createdAtTo filters out records created after the threshold")
    void createdAtToFiltersNewRecords() {
      ContactFilter filter = new ContactFilter();
      filter.setCreatedAtTo(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1));

      List<Contact> results = repository.findAll(ContactSpecifications.buildSpecification(filter));

      assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("createdAtFrom and createdAtTo combined returns records within range")
    void createdAtRangeReturnsRecordsWithinRange() {
      ContactFilter filter = new ContactFilter();
      filter.setCreatedAtFrom(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1));
      filter.setCreatedAtTo(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(1));

      List<Contact> results = repository.findAll(ContactSpecifications.buildSpecification(filter));

      assertThat(results).hasSize(1);
    }
  }

  @Nested
  @DisplayName("Combined filters")
  class CombinedFilters {

    @Test
    @DisplayName("status and sourceType combined returns correct subset")
    void statusAndSourceTypeCombinedReturnsCorrectSubset() {
      repository.saveAll(
          List.of(
              ContactFixtures.withSourceType(ORGANIZATION_A, ContactSourceType.CSV_MANIFEST),
              ContactFixtures.withSourceType(ORGANIZATION_A, ContactSourceType.WHATSAPP),
              ContactFixtures.claimed(ORGANIZATION_A, null)));

      ContactFilter filter = new ContactFilter();
      filter.setStatus(ContactStatus.UNRESOLVED);
      filter.setSourceType(ContactSourceType.CSV_MANIFEST);

      List<Contact> results = repository.findAll(ContactSpecifications.buildSpecification(filter));

      assertThat(results)
          .hasSize(1)
          .first()
          .satisfies(
              c -> {
                assertThat(c.getStatus()).isEqualTo(ContactStatus.UNRESOLVED);
                assertThat(c.getSourceType()).isEqualTo(ContactSourceType.CSV_MANIFEST);
              });
    }
  }
}
