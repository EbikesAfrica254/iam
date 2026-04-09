package com.ebikes.iam.dtos.responses.users;

import java.util.UUID;

public record UserExtensionReference(
    UUID id, String keycloakUserId, String firstName, String lastName, String username) {}
