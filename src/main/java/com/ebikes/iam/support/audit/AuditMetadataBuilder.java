package com.ebikes.iam.support.audit;

import com.ebikes.iam.database.entities.Contact;
import com.ebikes.iam.database.entities.Membership;
import com.ebikes.iam.database.entities.UserExtension;
import lombok.experimental.UtilityClass;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class AuditMetadataBuilder {

    public static Map<String, String> forContact(Contact contact) {
        return Map.of(
                "organizationId", contact.getOrganizationId(),
                "sourceReference", contact.getSourceReference(),
                "sourceType", contact.getSourceType().name(),
                "status", contact.getStatus().name());
    }

    public static Map<String, String> forMembership(Membership membership) {
        return Map.of(
                "branchId", membership.getBranchId() != null ? membership.getBranchId() : "",
                "branchName", membership.getBranchName() != null ? membership.getBranchName() : "",
                "isPrimary", String.valueOf(membership.getIsPrimary()),
                "organizationName", membership.getOrganizationName(),
                "roles", String.join(",", membership.getRoles()));
    }

    public static Map<String, String> forMembership(
            Membership membership, Map<String, String> extra) {
        Map<String, String> metadata = new HashMap<>(forMembership(membership));
        metadata.putAll(extra);
        return Collections.unmodifiableMap(metadata);
    }

    public static Map<String, String> forUserExtension(UserExtension userExtension) {
        return Map.of(
                "countryCode", userExtension.getCountryCode(),
                "emailVerified", String.valueOf(userExtension.isEmailVerified()),
                "phoneNumberVerified", String.valueOf(userExtension.isPhoneNumberVerified()),
                "status", userExtension.getStatus().name(),
                "username", userExtension.getUsername());
    }

    public static Map<String, String> forUserExtension(
            UserExtension userExtension, Map<String, String> extra) {
        Map<String, String> metadata = new HashMap<>(forUserExtension(userExtension));
        metadata.putAll(extra);
        return Collections.unmodifiableMap(metadata);
    }
}
