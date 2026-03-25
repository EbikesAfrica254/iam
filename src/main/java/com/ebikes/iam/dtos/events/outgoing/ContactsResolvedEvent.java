package com.ebikes.iam.dtos.events.outgoing;

import java.io.Serializable;
import java.util.Map;

public record ContactsResolvedEvent(
        String documentId, Map<String, String> phoneNumberToContactId, String serviceReference)
        implements Serializable {
    public ContactsResolvedEvent {
        phoneNumberToContactId = Map.copyOf(phoneNumberToContactId);
    }
}
