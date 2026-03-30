package com.ebikes.iam.services.contacts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import com.ebikes.iam.configurations.properties.ContactProperties;
import com.ebikes.iam.constants.EventConstants.DomainEvents;
import com.ebikes.iam.constants.EventConstants.RoutingKeys;
import com.ebikes.iam.database.entities.Contact;
import com.ebikes.iam.database.entities.UserExtension;
import com.ebikes.iam.database.repositories.ContactRepository;
import com.ebikes.iam.database.specifications.ContactSpecifications;
import com.ebikes.iam.dtos.events.incoming.BatchContactsEvent;
import com.ebikes.iam.dtos.events.outgoing.ContactsResolvedEvent;
import com.ebikes.iam.dtos.requests.filters.ContactFilter;
import com.ebikes.iam.dtos.responses.api.PaginatedResponse;
import com.ebikes.iam.dtos.responses.contacts.ContactResponse;
import com.ebikes.iam.enums.ContactSourceType;
import com.ebikes.iam.enums.ContactStatus;
import com.ebikes.iam.exceptions.ResourceNotFoundException;
import com.ebikes.iam.mappers.ContactMapper;
import com.ebikes.iam.services.events.OutboxService;
import com.ebikes.iam.support.audit.AuditContext;
import com.ebikes.iam.support.audit.AuditTemplate;
import com.ebikes.iam.support.audit.ThrowingRunnable;
import com.ebikes.iam.support.database.FilterUtilities;
import com.ebikes.iam.support.fixtures.ContactFixtures;
import com.ebikes.iam.support.fixtures.UserExtensionFixtures;

@DisplayName("ContactService")
@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

  private static final String ORGANIZATION_ID = "org-1";
  private static final String DOCUMENT_ID = "doc-1";
  private static final String BRANCH_ID = "branch-1";
  private static final String SERVICE_REFERENCE = "svc-ref";

  @Mock private AuditTemplate auditTemplate;
  @Mock private ContactMapper contactMapper;
  @Mock private ContactProperties contactProperties;
  @Mock private ContactRepository contactRepository;
  @Mock private OutboxService outboxService;

  @InjectMocks private ContactService contactService;

  @Captor private ArgumentCaptor<List<Contact>> contactsCaptor;
  @Captor private ArgumentCaptor<ContactsResolvedEvent> resolvedEventCaptor;

  @SuppressWarnings("unchecked")
  private void stubAuditTemplateToExecute() {
    doAnswer(
            invocation -> {
              ThrowingRunnable<Exception> operation = invocation.getArgument(1);
              operation.run();
              return null;
            })
        .when(auditTemplate)
        .execute(any(AuditContext.class), any(ThrowingRunnable.class));
  }

  private BatchContactsEvent batchEvent(Set<String> phoneNumbers) {
    return new BatchContactsEvent(
        BRANCH_ID,
        DOCUMENT_ID,
        ORGANIZATION_ID,
        phoneNumbers,
        SERVICE_REFERENCE,
        ContactSourceType.CSV_MANIFEST);
  }

  private Contact unresolvedContact(UUID id, String phoneNumber, OffsetDateTime expiresAt) {
    Contact contact = ContactFixtures.unresolved(ORGANIZATION_ID);
    ReflectionTestUtils.setField(contact, "id", id);
    ReflectionTestUtils.setField(contact, "phoneNumber", phoneNumber);
    ReflectionTestUtils.setField(contact, "expiresAt", expiresAt);
    return contact;
  }

  private Contact claimedContact(UUID id, OffsetDateTime expiresAt) {
    Contact contact =
        ContactFixtures.claimed(ORGANIZATION_ID, UserExtensionFixtures.active(ORGANIZATION_ID));
    ReflectionTestUtils.setField(contact, "id", id);
    ReflectionTestUtils.setField(contact, "phoneNumber", "+254700000001");
    ReflectionTestUtils.setField(contact, "expiresAt", expiresAt);
    return contact;
  }

  private Contact savedContact(UUID id, String phoneNumber) {
    Contact contact = ContactFixtures.unresolved(ORGANIZATION_ID);
    ReflectionTestUtils.setField(contact, "id", id);
    ReflectionTestUtils.setField(contact, "phoneNumber", phoneNumber);
    return contact;
  }

  @Nested
  @DisplayName("claim")
  class Claim {

    @Test
    @DisplayName("should claim contact successfully")
    @SuppressWarnings("unchecked")
    void shouldClaimContactSuccessfully() {
      stubAuditTemplateToExecute();
      UUID contactId = UUID.randomUUID();
      UserExtension userExtension = UserExtensionFixtures.active(ORGANIZATION_ID);
      Contact contact =
          unresolvedContact(
              contactId, "+254700000001", OffsetDateTime.now(ZoneOffset.UTC).plusHours(1));

      when(contactRepository.findById(contactId)).thenReturn(Optional.of(contact));

      contactService.claim(contactId, userExtension);

      assertThat(contact.getStatus()).isEqualTo(ContactStatus.CLAIMED);
      assertThat(contact.getUserExtension()).isSameAs(userExtension);
      verify(contactRepository).save(contact);
      verify(auditTemplate).execute(any(AuditContext.class), any(ThrowingRunnable.class));
    }

    @Test
    @DisplayName("should throw when contact not found")
    void shouldThrowWhenContactNotFound() {
      UUID contactId = UUID.randomUUID();
      UserExtension userExtension = UserExtensionFixtures.active(ORGANIZATION_ID);

      when(contactRepository.findById(contactId)).thenReturn(Optional.empty());

      ThrowingCallable claim = () -> contactService.claim(contactId, userExtension);

      assertThatThrownBy(claim).isInstanceOf(ResourceNotFoundException.class);
      verify(contactRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("processContacts")
  class ProcessContacts {

    @Test
    @DisplayName("should insert all when no existing contacts")
    void shouldInsertAllWhenNoExistingContacts() {
      BatchContactsEvent event = batchEvent(Set.of("+254700000001", "+254700000002"));

      Contact savedOne = savedContact(UUID.randomUUID(), "+254700000001");
      Contact savedTwo = savedContact(UUID.randomUUID(), "+254700000002");

      when(contactProperties.getExpiryHours()).thenReturn(24);
      when(contactProperties.getStaleThresholdHours()).thenReturn(-24);
      when(contactRepository.findAllByPhoneNumberInAndOrganizationId(any(), eq(ORGANIZATION_ID)))
          .thenReturn(List.of());
      when(contactRepository.saveAll(any())).thenReturn(List.of(savedOne, savedTwo));

      contactService.processContacts(event);

      verify(contactRepository).saveAll(contactsCaptor.capture());
      assertThat(contactsCaptor.getValue()).hasSize(2);

      verify(outboxService)
          .save(
              eq(DomainEvents.Contact.CREATED),
              resolvedEventCaptor.capture(),
              eq(RoutingKeys.IAM_CONTACT_CONFIGURATION));

      assertThat(resolvedEventCaptor.getValue().documentId()).isEqualTo(DOCUMENT_ID);
      assertThat(resolvedEventCaptor.getValue().phoneNumberToContactId())
          .containsEntry("+254700000001", savedOne.getId().toString())
          .containsEntry("+254700000002", savedTwo.getId().toString());
    }

    @Test
    @DisplayName("should return existing claimed contacts")
    void shouldReturnExistingClaimedContacts() {
      UUID existingId = UUID.randomUUID();
      Contact existing =
          claimedContact(existingId, OffsetDateTime.now(ZoneOffset.UTC).plusHours(24));
      BatchContactsEvent event = batchEvent(Set.of("+254700000001"));

      when(contactProperties.getExpiryHours()).thenReturn(24);
      when(contactProperties.getStaleThresholdHours()).thenReturn(-24);
      when(contactRepository.findAllByPhoneNumberInAndOrganizationId(any(), eq(ORGANIZATION_ID)))
          .thenReturn(List.of(existing));

      contactService.processContacts(event);

      verify(contactRepository, never()).saveAll(any());
      verify(contactRepository, never()).bulkUpdateExpiresAt(any(), any());

      verify(outboxService)
          .save(
              eq(DomainEvents.Contact.CREATED),
              resolvedEventCaptor.capture(),
              eq(RoutingKeys.IAM_CONTACT_CONFIGURATION));

      assertThat(resolvedEventCaptor.getValue().phoneNumberToContactId())
          .containsEntry("+254700000001", existingId.toString());
    }

    @Test
    @DisplayName("should return existing unresolved without refresh")
    void shouldReturnExistingUnresolvedWithoutRefresh() {
      UUID existingId = UUID.randomUUID();
      Contact existing =
          unresolvedContact(
              existingId, "+254700000001", OffsetDateTime.now(ZoneOffset.UTC).plusHours(2));
      BatchContactsEvent event = batchEvent(Set.of("+254700000001"));

      when(contactProperties.getExpiryHours()).thenReturn(24);
      when(contactProperties.getStaleThresholdHours()).thenReturn(-1);
      when(contactRepository.findAllByPhoneNumberInAndOrganizationId(any(), eq(ORGANIZATION_ID)))
          .thenReturn(List.of(existing));

      contactService.processContacts(event);

      verify(contactRepository, never()).bulkUpdateExpiresAt(any(), any());
      verify(contactRepository, never()).saveAll(any());

      verify(outboxService)
          .save(
              eq(DomainEvents.Contact.CREATED),
              resolvedEventCaptor.capture(),
              eq(RoutingKeys.IAM_CONTACT_CONFIGURATION));

      assertThat(resolvedEventCaptor.getValue().phoneNumberToContactId())
          .containsEntry("+254700000001", existingId.toString());
    }

    @Test
    @DisplayName("should refresh stale contacts")
    void shouldRefreshStaleContacts() {
      UUID staleId = UUID.randomUUID();
      Contact existing =
          unresolvedContact(
              staleId, "+254700000001", OffsetDateTime.now(ZoneOffset.UTC).minusHours(10));
      BatchContactsEvent event = batchEvent(Set.of("+254700000001"));

      when(contactProperties.getExpiryHours()).thenReturn(24);
      when(contactProperties.getStaleThresholdHours()).thenReturn(-1);
      when(contactRepository.findAllByPhoneNumberInAndOrganizationId(any(), eq(ORGANIZATION_ID)))
          .thenReturn(List.of(existing));
      when(contactRepository.bulkUpdateExpiresAt(any(), any())).thenReturn(1);

      contactService.processContacts(event);

      verify(contactRepository).bulkUpdateExpiresAt(any(), any());
      verify(contactRepository, never()).saveAll(any());

      verify(outboxService)
          .save(
              eq(DomainEvents.Contact.CREATED),
              resolvedEventCaptor.capture(),
              eq(RoutingKeys.IAM_CONTACT_CONFIGURATION));

      assertThat(resolvedEventCaptor.getValue().phoneNumberToContactId())
          .containsEntry("+254700000001", staleId.toString());
    }

    @Test
    @DisplayName("should handle mixed contacts correctly")
    void shouldHandleMixedContactsCorrectly() {
      UUID claimedId = UUID.randomUUID();
      UUID staleId = UUID.randomUUID();
      UUID freshId = UUID.randomUUID();

      Contact claimed = claimedContact(claimedId, OffsetDateTime.now(ZoneOffset.UTC).plusHours(24));
      Contact stale =
          unresolvedContact(
              staleId, "+254700000002", OffsetDateTime.now(ZoneOffset.UTC).minusHours(10));
      Contact fresh =
          unresolvedContact(
              freshId, "+254700000003", OffsetDateTime.now(ZoneOffset.UTC).plusHours(10));
      Contact inserted = savedContact(UUID.randomUUID(), "+254700000004");

      BatchContactsEvent event =
          batchEvent(Set.of("+254700000001", "+254700000002", "+254700000003", "+254700000004"));

      when(contactProperties.getExpiryHours()).thenReturn(24);
      when(contactProperties.getStaleThresholdHours()).thenReturn(-1);
      when(contactRepository.findAllByPhoneNumberInAndOrganizationId(any(), eq(ORGANIZATION_ID)))
          .thenReturn(List.of(claimed, stale, fresh));
      when(contactRepository.bulkUpdateExpiresAt(any(), any())).thenReturn(1);
      when(contactRepository.saveAll(any())).thenReturn(List.of(inserted));

      contactService.processContacts(event);

      verify(contactRepository).bulkUpdateExpiresAt(any(), any());
      verify(contactRepository).saveAll(contactsCaptor.capture());
      assertThat(contactsCaptor.getValue()).hasSize(1);

      verify(outboxService)
          .save(
              eq(DomainEvents.Contact.CREATED),
              resolvedEventCaptor.capture(),
              eq(RoutingKeys.IAM_CONTACT_CONFIGURATION));

      assertThat(resolvedEventCaptor.getValue().phoneNumberToContactId())
          .containsEntry("+254700000001", claimedId.toString())
          .containsEntry("+254700000002", staleId.toString())
          .containsEntry("+254700000003", freshId.toString())
          .containsEntry("+254700000004", inserted.getId().toString());
    }

    @Test
    @DisplayName("should emit contacts resolved event")
    void shouldEmitContactsResolvedEvent() {
      Contact saved = savedContact(UUID.randomUUID(), "+254700000001");
      BatchContactsEvent event = batchEvent(Set.of("+254700000001"));

      when(contactProperties.getExpiryHours()).thenReturn(24);
      when(contactProperties.getStaleThresholdHours()).thenReturn(-24);
      when(contactRepository.findAllByPhoneNumberInAndOrganizationId(any(), eq(ORGANIZATION_ID)))
          .thenReturn(List.of());
      when(contactRepository.saveAll(any())).thenReturn(List.of(saved));

      contactService.processContacts(event);

      verify(outboxService)
          .save(
              eq(DomainEvents.Contact.CREATED),
              resolvedEventCaptor.capture(),
              eq(RoutingKeys.IAM_CONTACT_CONFIGURATION));

      assertThat(resolvedEventCaptor.getValue().documentId()).isEqualTo(DOCUMENT_ID);
      assertThat(resolvedEventCaptor.getValue().phoneNumberToContactId())
          .containsEntry("+254700000001", saved.getId().toString());
    }
  }

  @Nested
  @DisplayName("search")
  class Search {

    @Test
    @DisplayName("should return paginated contacts")
    void shouldReturnPaginatedContacts() {
      ContactFilter filter = mock(ContactFilter.class);
      Contact contact = mock(Contact.class);
      ContactResponse response = mock(ContactResponse.class);
      Specification<Contact> specification = (root, query, cb) -> cb.conjunction();
      Pageable pageable = mock(Pageable.class);
      Page<Contact> page = new PageImpl<>(List.of(contact));

      try (MockedStatic<ContactSpecifications> mockedSpecifications =
              mockStatic(ContactSpecifications.class);
          MockedStatic<FilterUtilities> mockedFilterUtilities = mockStatic(FilterUtilities.class)) {
        mockedSpecifications
            .when(() -> ContactSpecifications.buildSpecification(filter))
            .thenReturn(specification);
        mockedFilterUtilities
            .when(
                () ->
                    FilterUtilities.buildPageable(
                        filter, ContactSpecifications.ALLOWED_SORT_FIELDS))
            .thenReturn(pageable);

        when(contactRepository.findAll(specification, pageable)).thenReturn(page);
        when(contactMapper.toResponse(contact)).thenReturn(response);

        PaginatedResponse<ContactResponse> result = contactService.search(filter);

        assertThat(result.data()).containsExactly(response);
        verify(contactRepository).findAll(specification, pageable);
        verify(contactMapper).toResponse(contact);
      }
    }

    @Test
    @DisplayName("should return empty page")
    void shouldReturnEmptyPage() {
      ContactFilter filter = mock(ContactFilter.class);
      Specification<Contact> specification = (root, query, cb) -> cb.conjunction();
      Pageable pageable = mock(Pageable.class);
      Page<Contact> page = new PageImpl<>(List.of());

      try (MockedStatic<ContactSpecifications> mockedSpecifications =
              mockStatic(ContactSpecifications.class);
          MockedStatic<FilterUtilities> mockedFilterUtilities = mockStatic(FilterUtilities.class)) {
        mockedSpecifications
            .when(() -> ContactSpecifications.buildSpecification(filter))
            .thenReturn(specification);
        mockedFilterUtilities
            .when(
                () ->
                    FilterUtilities.buildPageable(
                        filter, ContactSpecifications.ALLOWED_SORT_FIELDS))
            .thenReturn(pageable);

        when(contactRepository.findAll(specification, pageable)).thenReturn(page);

        PaginatedResponse<ContactResponse> result = contactService.search(filter);

        assertThat(result.data()).isEmpty();
      }
    }

    @Test
    @DisplayName("should build specification and pageable")
    void shouldBuildSpecificationAndPageable() {
      ContactFilter filter = mock(ContactFilter.class);
      Specification<Contact> specification = (root, query, cb) -> cb.conjunction();
      Pageable pageable = mock(Pageable.class);
      Page<Contact> page = new PageImpl<>(List.of());

      try (MockedStatic<ContactSpecifications> mockedSpecifications =
              mockStatic(ContactSpecifications.class);
          MockedStatic<FilterUtilities> mockedFilterUtilities = mockStatic(FilterUtilities.class)) {
        mockedSpecifications
            .when(() -> ContactSpecifications.buildSpecification(filter))
            .thenReturn(specification);
        mockedFilterUtilities
            .when(
                () ->
                    FilterUtilities.buildPageable(
                        filter, ContactSpecifications.ALLOWED_SORT_FIELDS))
            .thenReturn(pageable);

        when(contactRepository.findAll(specification, pageable)).thenReturn(page);

        contactService.search(filter);

        mockedSpecifications.verify(() -> ContactSpecifications.buildSpecification(filter));
        mockedFilterUtilities.verify(
            () -> FilterUtilities.buildPageable(filter, ContactSpecifications.ALLOWED_SORT_FIELDS));
      }
    }
  }
}
