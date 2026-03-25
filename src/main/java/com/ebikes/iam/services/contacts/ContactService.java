package com.ebikes.iam.services.contacts;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.iam.configurations.properties.ContactProperties;
import com.ebikes.iam.constants.EventConstants;
import com.ebikes.iam.constants.EventConstants.EventTypes;
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
import com.ebikes.iam.enums.ContactStatus;
import com.ebikes.iam.enums.ResponseCode;
import com.ebikes.iam.exceptions.ResourceNotFoundException;
import com.ebikes.iam.mappers.ContactMapper;
import com.ebikes.iam.services.events.OutboxService;
import com.ebikes.iam.support.audit.AuditContext;
import com.ebikes.iam.support.audit.AuditMetadataBuilder;
import com.ebikes.iam.support.audit.AuditTemplate;
import com.ebikes.iam.support.database.FilterUtilities;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class ContactService {

  private static final String ENTITY_TYPE = "CONTACT";

  private final AuditTemplate auditTemplate;
  private final ContactMapper contactMapper;
  private final ContactProperties contactProperties;
  private final ContactRepository contactRepository;
  private final OutboxService outboxService;

  @Transactional
  public void claim(UUID contactId, UserExtension userExtension) {
    Contact contact =
        contactRepository
            .findById(contactId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        ResponseCode.RESOURCE_NOT_FOUND, "Contact not found"));

    AuditContext context =
        new AuditContext(
            contactId,
            ENTITY_TYPE,
            EventTypes.IAM.CONTACT_CLAIMED,
            AuditMetadataBuilder.forContact(contact),
            contact.getOrganizationId(),
            RoutingKeys.IAM_CONTACT_CONFIGURATION);

    auditTemplate.execute(
        context,
        () -> {
          contact.claim(userExtension);
          contactRepository.save(contact);
        });

    log.info("Contact claimed - contactId={} userExtensionId={}", contactId, userExtension.getId());
  }

  @Transactional
  public void processContacts(BatchContactsEvent event) {
    log.info(
        "Processing manifest contacts - documentId={} count={}",
        event.documentId(),
        event.phoneNumbers().size());

    List<String> phoneNumbers = new ArrayList<>(event.phoneNumbers());
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    OffsetDateTime newExpiresAt = now.plusHours(contactProperties.getExpiryHours());
    OffsetDateTime staleThreshold = now.plusHours(contactProperties.getStaleThresholdHours());

    List<Contact> existing =
        contactRepository.findAllByPhoneNumberInAndOrganizationId(
            phoneNumbers, event.organizationId());

    Map<String, Contact> existingByPhone =
        existing.stream().collect(Collectors.toMap(Contact::getPhoneNumber, c -> c));

    Map<String, String> responseMap = new HashMap<>();
    List<UUID> staleIds = new ArrayList<>();
    List<Contact> toInsert = new ArrayList<>();

    for (String phoneNumber : phoneNumbers) {
      Contact existingContact = existingByPhone.get(phoneNumber);

      if (existingContact != null) {
        if (existingContact.getStatus() == ContactStatus.CLAIMED) {
          responseMap.put(phoneNumber, existingContact.getId().toString());
        } else if (existingContact.getStatus() == ContactStatus.UNRESOLVED) {
          if (existingContact.getExpiresAt().isBefore(staleThreshold)) {
            staleIds.add(existingContact.getId());
          }
          responseMap.put(phoneNumber, existingContact.getId().toString());
        }
      }

      if (!responseMap.containsKey(phoneNumber)) {
        toInsert.add(
            Contact.builder()
                .branchId(event.branchId())
                .expiresAt(newExpiresAt)
                .organizationId(event.organizationId())
                .phoneNumber(phoneNumber)
                .sourceReference(event.documentId())
                .sourceType(event.sourceType())
                .build());
      }
    }

    if (!staleIds.isEmpty()) {
      int updated = contactRepository.bulkUpdateExpiresAt(staleIds, newExpiresAt);
      log.debug("Bulk updated expiry for {} stale contacts", updated);
    }

    if (!toInsert.isEmpty()) {
      List<Contact> saved = contactRepository.saveAll(toInsert);
      saved.forEach(c -> responseMap.put(c.getPhoneNumber(), c.getId().toString()));
      log.debug("Inserted {} new contacts", saved.size());
    }

    log.info(
        "Manifest contacts processed - documentId={} resolved={} staleRefreshed={} inserted={}",
        event.documentId(),
        responseMap.size(),
        staleIds.size(),
        toInsert.size());

    outboxService.save(
        EventTypes.IAM.CONTACT_CREATED,
        new ContactsResolvedEvent(
            event.documentId(), responseMap, EventConstants.EventSource.serviceReference()),
        RoutingKeys.IAM_CONTACT_CONFIGURATION);
  }

  @Transactional(readOnly = true)
  public PaginatedResponse<ContactResponse> search(ContactFilter filter) {
    Specification<Contact> spec = ContactSpecifications.buildSpecification(filter);
    Pageable pageable =
        FilterUtilities.buildPageable(filter, ContactSpecifications.ALLOWED_SORT_FIELDS);
    Page<ContactResponse> page =
        contactRepository.findAll(spec, pageable).map(contactMapper::toResponse);
    return PaginatedResponse.from("Contacts retrieved successfully.", page);
  }
}
