package com.ebikes.iam.dtos.events.incoming;

import com.ebikes.iam.enums.ContactSourceType;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

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
