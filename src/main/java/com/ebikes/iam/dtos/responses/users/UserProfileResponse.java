package com.ebikes.iam.dtos.responses.users;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.ebikes.iam.dtos.responses.memberships.MembershipResponse;
import com.ebikes.iam.enums.UserStatus;

public record UserProfileResponse(
    MembershipResponse activeMembership,
    String countryCode,
    OffsetDateTime createdAt,
    String email,
    boolean emailVerified,
    String firstName,
    UUID id,
    String lastName,
    List<MembershipResponse> memberships,
    String phoneNumber,
    boolean phoneNumberVerified,
    UserStatus status,
    String username)
    implements Serializable {}
