package com.ebikes.iam.dtos.internal;

import com.ebikes.iam.database.entities.UserExtension;

public record UserProvisionedApplicationEvent(
        String organizationId, String organizationName, UserExtension userExtension) {
}
