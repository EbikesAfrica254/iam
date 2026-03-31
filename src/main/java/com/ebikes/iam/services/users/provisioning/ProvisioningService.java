package com.ebikes.iam.services.users.provisioning;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.ebikes.iam.adapters.keycloak.KeycloakGroupAdapter;
import com.ebikes.iam.adapters.keycloak.KeycloakUserAdapter;
import com.ebikes.iam.configurations.properties.KeycloakProperties;
import com.ebikes.iam.constants.ApplicationConstants.Keycloak;
import com.ebikes.iam.dtos.requests.memberships.CreateMembershipRequest;
import com.ebikes.iam.dtos.requests.users.CreateUserRequest;
import com.ebikes.iam.dtos.requests.users.SignupRequest;
import com.ebikes.iam.enums.ResponseCode;
import com.ebikes.iam.enums.UserRole;
import com.ebikes.iam.exceptions.DuplicateResourceException;
import com.ebikes.iam.exceptions.ValidationException;
import com.ebikes.iam.support.context.ExecutionContext;
import com.ebikes.iam.support.keycloak.GroupPathUtilities;
import com.ebikes.iam.support.security.RBACUtilities;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class ProvisioningService {

  private final AuthorizationService authorizationService;
  private final KeycloakGroupAdapter keycloakGroupAdapter;
  private final KeycloakUserAdapter keycloakUserAdapter;
  private final KeycloakProperties keycloakProperties;
  private final PersistenceService persistenceService;

  public void provisionUser(CreateUserRequest request) {
    provision(request);
  }

  public void provisionSignup(SignupRequest request) {
    provision(
        new CreateUserRequest(
            null,
            request.countryCode(),
            request.email(),
            request.firstName(),
            true,
            request.lastName(),
            null,
            request.phoneNumber(),
            Set.of(UserRole.CUSTOMER),
            request.username()));
  }

  private ProvisioningContext buildProvisioningContext(CreateUserRequest request) {
    Set<UserRole> roles = resolveRoles(request);
    String organizationId = resolveOrganizationId(request);
    String branchId = resolveBranchId(request, roles);
    return new ProvisioningContext(organizationId, branchId, request.isPrimary(), roles);
  }

  private void compensate(String keycloakUserId) {
    try {
      keycloakUserAdapter.deleteUser(keycloakUserId);
      log.warn("Compensated: Keycloak user deleted - keycloakUserId={}", keycloakUserId);
    } catch (Exception e) {
      log.error("Compensation failed - keycloakUserId={} may be orphaned", keycloakUserId, e);
    }
  }

  private String createKeycloakUser(ProvisioningContext context, CreateUserRequest request) {
    String keycloakUserId =
        keycloakUserAdapter.createUser(
            request.email(), request.firstName(), request.lastName(), request.username());

    Map<String, String> attributes = new HashMap<>();
    attributes.put(Keycloak.ACTIVE_ORGANIZATION_ATTRIBUTE, context.organizationId());
    attributes.put(Keycloak.ACTIVE_ORGANIZATION_ROLES_ATTRIBUTE, context.rolesCsv());
    attributes.put(Keycloak.PHONE_NUMBER_ATTRIBUTE, request.phoneNumber());
    attributes.put(Keycloak.PHONE_NUMBER_VERIFIED_ATTRIBUTE, "false");

    if (context.branchId() != null) {
      attributes.put(Keycloak.ACTIVE_BRANCH_ATTRIBUTE, context.branchId());
    }

    keycloakUserAdapter.updateUserAttributes(keycloakUserId, attributes);

    return keycloakUserId;
  }

  private void provision(CreateUserRequest request) {
    authorizationService.authorize(request.branchId(), request.organizationId(), request.roles());

    validateEmailNotRegistered(request.email());

    ProvisioningContext context = buildProvisioningContext(request);
    String groupPath = GroupPathUtilities.generate(context.organizationId());

    String keycloakUserId = null;
    try {
      keycloakUserId = createKeycloakUser(context, request);
      keycloakGroupAdapter.addUserToGroup(groupPath, keycloakUserId);

      CreateMembershipRequest membershipRequest =
          new CreateMembershipRequest(
              context.branchId(), context.isPrimary(), context.organizationId(), context.roles());
      persistenceService.persist(
          keycloakUserId, groupPath, context.organizationId, membershipRequest, request);

    } catch (Exception e) {
      if (keycloakUserId != null) {
        compensate(keycloakUserId);
      }
      throw e;
    }

    log.info("User provisioned: keycloakUserId={}", keycloakUserId);
  }

  private String resolveBranchId(CreateUserRequest request, Set<UserRole> targetRoles) {
    if (request.branchId() != null && !request.branchId().isBlank()) {
      return request.branchId();
    }

    boolean hasOrganizationLevelRole =
        targetRoles.stream().anyMatch(RBACUtilities::isOrganizationRole);
    boolean hasBranchLevelRole = targetRoles.stream().anyMatch(RBACUtilities::isBranchRole);

    if (hasOrganizationLevelRole && !hasBranchLevelRole) {
      return null;
    }

    if (hasBranchLevelRole) {
      String creatorBranch =
          switch (ExecutionContext.get()) {
            case ExecutionContext.UserContext uc -> uc.activeBranch();
            case ExecutionContext.SystemContext ignored -> null;
          };
      if (creatorBranch != null && !creatorBranch.isBlank()) {
        return creatorBranch;
      }
      throw new ValidationException(
          ResponseCode.INVALID_ARGUMENTS,
          "Branch ID is required for branch-level roles",
          "branchId",
          "null");
    }

    return null;
  }

  private String resolveOrganizationId(CreateUserRequest request) {
    if (request.organizationId() != null && !request.organizationId().isBlank()) {
      return request.organizationId();
    }
    return keycloakProperties.getBaseOrganizationId();
  }

  private Set<UserRole> resolveRoles(CreateUserRequest request) {
    if (request.roles() != null && !request.roles().isEmpty()) {
      return request.roles();
    }
    throw new ValidationException(
        ResponseCode.INVALID_ARGUMENTS, "At least one role must be specified", "roles", "null");
  }

  private void validateEmailNotRegistered(String email) {
    if (keycloakUserAdapter.isEmailRegistered(email)) {
      throw new DuplicateResourceException(
          ResponseCode.DUPLICATE_RESOURCE, "Email already registered");
    }
  }

  private record ProvisioningContext(
      String organizationId, String branchId, Boolean isPrimary, Set<UserRole> roles) {

    public String rolesCsv() {
      return UserRole.toCommaSeparated(roles);
    }
  }
}
