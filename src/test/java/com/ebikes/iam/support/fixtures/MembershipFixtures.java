package com.ebikes.iam.support.fixtures;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.ebikes.iam.database.entities.Membership;
import com.ebikes.iam.database.entities.UserExtension;
import com.ebikes.iam.enums.UserRole;

public final class MembershipFixtures {

  private MembershipFixtures() {}

  public static Membership forUser(UserExtension userExtension) {
    return base(userExtension).isPrimary(true).build();
  }

  public static Membership forUser(UserExtension userExtension, UserRole... roles) {
    return base(userExtension).isPrimary(true).roles(toNameSet(roles)).build();
  }

  public static Membership secondary(UserExtension userExtension) {
    return base(userExtension).isPrimary(false).build();
  }

  public static Membership withBranch(UserExtension userExtension, String branchId) {
    return base(userExtension).branchId(branchId).isPrimary(false).build();
  }

  private static Membership.MembershipBuilder<?, ?> base(UserExtension userExtension) {
    String organizationId = UUID.randomUUID().toString();
    return Membership.builder()
        .id(UUID.randomUUID())
        .userExtension(userExtension)
        .keycloakUserId(userExtension.getKeycloakUserId())
        .organizationId(organizationId)
        .keycloakGroupPath("/" + organizationId)
        .roles(toNameSet(UserRole.ORGANIZATION_ADMIN))
        .isPrimary(false);
  }

  private static Set<String> toNameSet(UserRole... roles) {
    Set<String> result = new HashSet<>();
    for (UserRole role : roles) {
      result.add(role.name());
    }
    return result;
  }
}
