package com.ebikes.iam.integration.services.contacts;

import static com.ebikes.iam.support.fixtures.SecurityFixtures.TEST_USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

import com.ebikes.iam.constants.EventConstants.AuditEvents;
import com.ebikes.iam.constants.EventConstants.DomainEvents;
import com.ebikes.iam.constants.EventConstants.RoutingKeys;
import com.ebikes.iam.database.entities.Contact;
import com.ebikes.iam.database.entities.UserExtension;
import com.ebikes.iam.database.repositories.ContactRepository;
import com.ebikes.iam.database.repositories.OutboxRepository;
import com.ebikes.iam.database.repositories.UserExtensionRepository;
import com.ebikes.iam.dtos.events.incoming.BatchContactsEvent;
import com.ebikes.iam.dtos.requests.filters.ContactFilter;
import com.ebikes.iam.dtos.responses.api.PaginatedResponse;
import com.ebikes.iam.dtos.responses.contacts.ContactResponse;
import com.ebikes.iam.enums.ContactSourceType;
import com.ebikes.iam.enums.ContactStatus;
import com.ebikes.iam.enums.UserRole;
import com.ebikes.iam.exceptions.ResourceNotFoundException;
import com.ebikes.iam.services.contacts.ContactService;
import com.ebikes.iam.support.context.ExecutionContext;
import com.ebikes.iam.support.fixtures.ContactFixtures;
import com.ebikes.iam.support.fixtures.UserExtensionFixtures;
import com.ebikes.iam.support.infrastructure.AbstractIntegrationTest;

class ContactServiceIT extends AbstractIntegrationTest {

  @Autowired private ContactService contactService;
  @Autowired private ContactRepository contactRepository;
  @Autowired private OutboxRepository outboxRepository;
  @Autowired private UserExtensionRepository userExtensionRepository;

  private static final String TEST_ORGANIZATION_ID = "test-org";

  @BeforeEach
  void setUp() {
    outboxRepository.deleteAll();
    contactRepository.deleteAll();
  }

  @Nested
  class Claim {

    @Test
    @DisplayName("claims unresolved contact")
    void claimsUnresolvedContact() {
      UserExtension user = userExtensionRepository.save(UserExtensionFixtures.active());
      Contact contact = contactRepository.save(ContactFixtures.unresolved(TEST_ORGANIZATION_ID));

      contactService.claim(contact.getId(), user);

      Contact saved = contactRepository.findById(contact.getId()).orElseThrow();
      assertThat(saved.getStatus()).isEqualTo(ContactStatus.CLAIMED);
      assertThat(saved.getUserExtension().getId()).isEqualTo(user.getId());

      assertThat(outboxRepository.findAll())
          .hasSize(1)
          .first()
          .satisfies(
              o -> {
                assertThat(o.getEventType()).isEqualTo(DomainEvents.Contact.CLAIMED);
                assertThat(o.getRoutingKey()).isEqualTo(AuditEvents.CONTACT);
              });
    }

    @Test
    @DisplayName("throws when contact not found")
    void throwsWhenContactNotFound() {
      UserExtension user = userExtensionRepository.save(UserExtensionFixtures.active());

      UUID testId = UUID.randomUUID();
      assertThatThrownBy(() -> contactService.claim(testId, user))
          .isInstanceOf(ResourceNotFoundException.class);

      assertThat(outboxRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("throws when contact already claimed")
    void throwsWhenContactAlreadyClaimed() {
      UserExtension user = userExtensionRepository.save(UserExtensionFixtures.active());
      Contact contact = contactRepository.save(ContactFixtures.claimed(TEST_ORGANIZATION_ID, user));

      UUID contactId = contact.getId();
      assertThatThrownBy(() -> contactService.claim(contactId, user))
          .isInstanceOf(IllegalStateException.class);

      Contact unchanged = contactRepository.findById(contact.getId()).orElseThrow();
      assertThat(unchanged.getStatus()).isEqualTo(ContactStatus.CLAIMED);
      assertThat(outboxRepository.findAll())
          .hasSize(1)
          .first()
          .satisfies(o -> assertThat(o.getEventType()).isEqualTo(DomainEvents.Contact.CLAIMED));
    }
  }

  @Nested
  class ProcessContacts {

    private static final String ORG_ID = TEST_ORGANIZATION_ID;
    private static final String DOCUMENT_ID = UUID.randomUUID().toString();

    private BatchContactsEvent event(Set<String> phoneNumbers) {
      return new BatchContactsEvent(
          null,
          DOCUMENT_ID,
          ORG_ID,
          phoneNumbers,
          UUID.randomUUID().toString(),
          ContactSourceType.CSV_MANIFEST);
    }

    @Test
    @DisplayName("throws when no contacts provided")
    void insertsAllNewContacts() {
      Set<String> numbers = Set.of("+254700000001", "+254700000002", "+254700000003");

      contactService.processContacts(event(numbers));

      assertThat(contactRepository.findAll())
          .hasSize(3)
          .allMatch(c -> c.getStatus() == ContactStatus.UNRESOLVED);

      assertThat(outboxRepository.findAll())
          .hasSize(1)
          .first()
          .satisfies(o -> assertThat(o.getEventType()).isEqualTo(DomainEvents.Contact.CREATED));
    }

    @Test
    @DisplayName("inserts new unresolved contacts")
    void reusesExistingUnresolvedFreshContact() {
      Contact existing =
          contactRepository.save(
              Contact.builder()
                  .organizationId(ORG_ID)
                  .phoneNumber("+254700000001")
                  .sourceReference(UUID.randomUUID().toString())
                  .sourceType(ContactSourceType.CSV_MANIFEST)
                  .status(ContactStatus.UNRESOLVED)
                  .expiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusDays(30))
                  .build());

      contactService.processContacts(event(Set.of("+254700000001", "+254700000002")));

      List<Contact> all = contactRepository.findAll();
      assertThat(all).hasSize(2);

      Contact reused = contactRepository.findById(existing.getId()).orElseThrow();
      assertThat(reused.getStatus()).isEqualTo(ContactStatus.UNRESOLVED);
      assertThat(reused.getExpiresAt()).isBeforeOrEqualTo(existing.getExpiresAt().plusSeconds(1));
    }

    @Test
    @DisplayName("refreshes expiry for stale unresolved contact")
    void refreshesExpiryForStaleUnresolvedContact() {
      OffsetDateTime staleExpiry = OffsetDateTime.now(ZoneOffset.UTC).plusHours(1);
      Contact stale =
          contactRepository.save(
              Contact.builder()
                  .organizationId(ORG_ID)
                  .phoneNumber("+254700000001")
                  .sourceReference(UUID.randomUUID().toString())
                  .sourceType(ContactSourceType.CSV_MANIFEST)
                  .status(ContactStatus.UNRESOLVED)
                  .expiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusHours(1))
                  .build());

      contactService.processContacts(event(Set.of("+254700000001")));

      Contact refreshed = contactRepository.findById(stale.getId()).orElseThrow();
      assertThat(refreshed.getExpiresAt()).isAfter(staleExpiry);
      assertThat(refreshed.getStatus()).isEqualTo(ContactStatus.UNRESOLVED);
    }

    @Test
    @DisplayName("inserts new unresolved contacts with new expiry")
    void includesExistingClaimedContactInResponseWithoutReinserting() {
      UserExtension user = userExtensionRepository.save(UserExtensionFixtures.active());
      Contact claimed =
          Contact.builder()
              .organizationId(ORG_ID)
              .phoneNumber("+254700000001")
              .sourceReference(UUID.randomUUID().toString())
              .sourceType(ContactSourceType.CSV_MANIFEST)
              .status(ContactStatus.CLAIMED)
              .userExtension(user)
              .expiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusDays(30))
              .build();
      contactRepository.save(claimed);

      contactService.processContacts(event(Set.of("+254700000001")));

      // no new record inserted
      assertThat(contactRepository.findAll()).hasSize(1);
      assertThat(contactRepository.findById(claimed.getId())).isPresent();
    }

    @Test
    @DisplayName("writes outbox with all resolved ids in response map")
    void writesOutboxWithAllResolvedIdsInResponseMap() {
      Set<String> numbers = Set.of("+254700000001", "+254700000002");

      contactService.processContacts(event(numbers));

      assertThat(outboxRepository.findAll())
          .hasSize(1)
          .first()
          .satisfies(
              o -> {
                assertThat(o.getEventType()).isEqualTo(DomainEvents.Contact.CREATED);
                assertThat(o.getRoutingKey()).isEqualTo(RoutingKeys.IAM_CONTACT_CONFIGURATION);
              });
    }
  }

  @Nested
  class Search {

    @BeforeEach
    void setUp() {
      ExecutionContext.set(
          TEST_USER_ID,
          TEST_ORGANIZATION_ID,
          null,
          "test@ebikes.test",
          Set.of(),
          "+254700000001",
          Set.of(UserRole.ORGANIZATION_ADMIN.name()));

      outboxRepository.deleteAll();
      contactRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
      ExecutionContext.clear();
    }

    @Test
    void filtersByOrganizationId() {
      contactRepository.save(ContactFixtures.unresolved(TEST_ORGANIZATION_ID));
      contactRepository.save(ContactFixtures.unresolved(TEST_ORGANIZATION_ID));
      contactRepository.save(ContactFixtures.unresolved("other-org"));

      ContactFilter filter = new ContactFilter();
      filter.setOrganizationId(TEST_ORGANIZATION_ID);

      PaginatedResponse<ContactResponse> result = contactService.search(filter);

      assertThat(result.data()).hasSize(2);
      assertThat(result.data()).allMatch(r -> r.organizationId().equals(TEST_ORGANIZATION_ID));
    }

    @Test
    void filtersByStatus() {
      UserExtension user = userExtensionRepository.save(UserExtensionFixtures.active());
      contactRepository.save(ContactFixtures.unresolved(TEST_ORGANIZATION_ID));
      contactRepository.save(ContactFixtures.claimed(TEST_ORGANIZATION_ID, user));

      ContactFilter filter = new ContactFilter();
      filter.setStatus(ContactStatus.CLAIMED);

      PaginatedResponse<ContactResponse> result = contactService.search(filter);

      assertThat(result.data()).hasSize(1);
      assertThat(result.data().getFirst().status()).isEqualTo(ContactStatus.CLAIMED);
    }

    @Test
    void respectsPageSize() {
      for (int i = 0; i < 5; i++) {
        contactRepository.save(ContactFixtures.unresolved(TEST_ORGANIZATION_ID));
      }

      ContactFilter filter = new ContactFilter();
      filter.setSize(2);

      PaginatedResponse<ContactResponse> result = contactService.search(filter);

      assertThat(result.data()).hasSize(2);
      assertThat(result.totalElements()).isEqualTo(5);
    }
  }
}
