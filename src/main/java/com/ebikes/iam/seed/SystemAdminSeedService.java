package com.ebikes.iam.seed;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.ebikes.iam.adapters.keycloak.KeycloakGroupAdapter;
import com.ebikes.iam.adapters.keycloak.KeycloakUserAdapter;
import com.ebikes.iam.configurations.properties.KeycloakProperties;
import com.ebikes.iam.configurations.properties.SeedProperties;
import com.ebikes.iam.constants.ApplicationConstants.Keycloak;
import com.ebikes.iam.database.entities.UserExtension;
import com.ebikes.iam.dtos.requests.memberships.CreateMembershipRequest;
import com.ebikes.iam.dtos.requests.users.CreateUserRequest;
import com.ebikes.iam.enums.UserRole;
import com.ebikes.iam.services.users.UserExtensionService;
import com.ebikes.iam.services.users.membership.MembershipService;
import com.ebikes.iam.support.context.ExecutionContext;
import com.ebikes.iam.support.keycloak.GroupPathUtilities;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class SystemAdminSeedService {

  private final KeycloakGroupAdapter keycloakGroupAdapter;
  private final KeycloakUserAdapter keycloakUserAdapter;
  private final KeycloakProperties keycloakProperties;
  private final MembershipService membershipService;
  private final SeedProperties seedProperties;
  private final UserExtensionService userExtensionService;

  public void seedIfAbsent() {
    if (keycloakUserAdapter.isEmailRegistered(seedProperties.getEmail())) {
      log.info("System admin already exists - skipping seed");
      return;
    }

    String organizationId = keycloakProperties.getBaseOrganizationId();
    String groupPath = GroupPathUtilities.generate(organizationId);
    String keycloakUserId = null;

    try {
      ExecutionContext.setSystem();

      keycloakUserId =
          keycloakUserAdapter.createUser(
              seedProperties.getEmail(),
              seedProperties.getFirstName(),
              seedProperties.getLastName(),
              seedProperties.getUsername());

      Map<String, String> attributes = new HashMap<>();
      attributes.put(Keycloak.ACTIVE_ORGANIZATION_ATTRIBUTE, organizationId);
      attributes.put(Keycloak.ACTIVE_ORGANIZATION_ROLES_ATTRIBUTE, UserRole.SYSTEM_ADMIN.name());
      attributes.put(Keycloak.PHONE_NUMBER_ATTRIBUTE, seedProperties.getPhoneNumber());
      attributes.put(Keycloak.PHONE_NUMBER_VERIFIED_ATTRIBUTE, "false");
      keycloakUserAdapter.updateUserAttributes(keycloakUserId, attributes);

      keycloakUserAdapter.resetPassword(keycloakUserId, seedProperties.getPassword(), true);

      keycloakGroupAdapter.addUserToGroup(groupPath, keycloakUserId);

      CreateUserRequest userRequest =
          new CreateUserRequest(
              null,
              "KE",
              seedProperties.getEmail(),
              seedProperties.getFirstName(),
              true,
              seedProperties.getLastName(),
              organizationId,
              seedProperties.getPhoneNumber(),
              Set.of(UserRole.SYSTEM_ADMIN),
              seedProperties.getUsername());

      UserExtension extension =
          userExtensionService.create(keycloakUserId, organizationId, userRequest);

      keycloakUserAdapter.enableUser(keycloakUserId);
      userExtensionService.activate(extension);

      membershipService.createRecord(
          keycloakUserId,
          groupPath,
          new CreateMembershipRequest(null, true, organizationId, Set.of(UserRole.SYSTEM_ADMIN)),
          extension);

      log.info("System admin seeded successfully - keycloakUserId={}", keycloakUserId);

    } catch (Exception e) {
      if (keycloakUserId != null) {
        try {
          keycloakUserAdapter.deleteUser(keycloakUserId);
          log.warn(
              "Compensated: Keycloak system admin deleted after seed failure - keycloakUserId={}",
              keycloakUserId);
        } catch (Exception ex) {
          log.error("Compensation failed - keycloakUserId={} may be orphaned", keycloakUserId, ex);
        }
      }
      throw e;
    } finally {
      ExecutionContext.clear();
    }
  }
}
