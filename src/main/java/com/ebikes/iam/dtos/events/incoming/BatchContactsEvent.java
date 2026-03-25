package com.ebikes.iam.dtos.events.incoming;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import com.ebikes.iam.enums.ContactSourceType;

public record BatchContactsEvent(
    String branchId,
    String documentId,
    String organizationId,
    Set<String> phoneNumbers,
    String serviceReference,
    ContactSourceType sourceType)
    implements Serializable {
  public BatchContactsEvent {
    phoneNumbers = Collections.unmodifiableSet(phoneNumbers);
  }
}
