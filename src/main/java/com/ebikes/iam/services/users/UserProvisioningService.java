package com.ebikes.iam.services.users;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.iam.configurations.properties.KeycloakProperties;
import com.ebikes.iam.constants.ApplicationConstants;
import com.ebikes.iam.constants.EventConstants.EventTypes;
import com.ebikes.iam.constants.EventConstants.RoutingKeys;
import com.ebikes.iam.database.entities.UserExtension;
import com.ebikes.iam.dtos.internal.UserProvisionedApplicationEvent;
import com.ebikes.iam.dtos.requests.memberships.CreateMembershipRequest;
import com.ebikes.iam.dtos.requests.users.CreateUserRequest;
import com.ebikes.iam.dtos.requests.users.SignupRequest;
import com.ebikes.iam.enums.ResponseCode;
import com.ebikes.iam.enums.UserRole;
import com.ebikes.iam.exceptions.DuplicateResourceException;
import com.ebikes.iam.exceptions.ValidationException;
import com.ebikes.iam.mappers.MembershipMapper;
import com.ebikes.iam.publishers.UserConfigurationEventPublisher;
import com.ebikes.iam.services.keycloak.users.KeycloakUserService;
import com.ebikes.iam.support.audit.AuditContext;
import com.ebikes.iam.support.audit.AuditTemplate;
import com.ebikes.iam.support.context.ExecutionContext;
import com.ebikes.iam.support.security.RBACUtilities;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserProvisioningService {

  private final ApplicationEventPublisher applicationEventPublisher;
  private final AuditTemplate auditTemplate;
  private final KeycloakProperties keycloakProperties;
  private final KeycloakUserService keycloakUserService;
  private final MembershipMapper membershipMapper;
  private final MembershipService membershipService;
  private final UserConfigurationEventPublisher userConfigurationEventPublisher;
  private final UserExtensionService userExtensionService;

  @Transactional
  public void provisionSignup(SignupRequest request) {
    doProvision(
        new CreateUserRequest(
            null,
            null,
            request.countryCode(),
            request.email(),
            request.firstName(),
            true,
            request.lastName(),
            null,
            null,
            request.phoneNumber(),
            Set.of(UserRole.CUSTOMER),
            request.username()));
  }

  @Transactional
  public void provisionUser(CreateUserRequest request) {
    doProvision(request);
  }

  private void doProvision(CreateUserRequest request) {
    validateEmailNotRegistered(request.email());

    ProvisioningContext context = buildProvisioningContext(request);

    AuditContext auditContext =
        new AuditContext(
            null,
            "USER_EXTENSION",
            EventTypes.IAM.USER_PROVISIONED,
            null,
            context.organizationId(),
            RoutingKeys.IAM_USER_AUDIT);

    UserExtension userExtension =
        auditTemplate.execute(
            auditContext,
            () -> {
              String keycloakUserId = createKeycloakUser(context, request);
              UserExtension extension =
                  userExtensionService.create(keycloakUserId, context.organizationId(), request);
              createMembership(context, keycloakUserId);
              userConfigurationEventPublisher.publishUserProvisioned(
                  extension, context.organizationId(), context.branchId());
              return extension;
            });

    log.info(
        "User provisioned: userId={}, keycloakUserId={}",
        userExtension.getId(),
        userExtension.getKeycloakUserId());

    applicationEventPublisher.publishEvent(
        new UserProvisionedApplicationEvent(
            context.organizationId(), context.organization().organizationName(), userExtension));
  }

  private ProvisioningContext buildProvisioningContext(CreateUserRequest request) {
    Set<UserRole> roles = resolveRoles(request);
    Organization organization = resolveOrganization(request);
    Branch branch = resolveBranch(request, roles);
    return new ProvisioningContext(organization, branch, request.isPrimary(), roles);
  }

  private String createKeycloakUser(ProvisioningContext context, CreateUserRequest request) {
    String keycloakUserId =
        keycloakUserService.createUser(
            request.email(), request.firstName(), request.lastName(), request.username());

    Map<String, String> attributes = new HashMap<>();
    attributes.put(
        ApplicationConstants.Keycloak.ACTIVE_ORGANIZATION_ATTRIBUTE, context.organizationId());
    attributes.put(
        ApplicationConstants.Keycloak.ACTIVE_ORGANIZATION_ROLES_ATTRIBUTE, context.rolesCsv());
    attributes.put(ApplicationConstants.Keycloak.PHONE_NUMBER_ATTRIBUTE, request.phoneNumber());
    attributes.put(ApplicationConstants.Keycloak.PHONE_NUMBER_VERIFIED_ATTRIBUTE, "false");

    if (context.branchId() != null) {
      attributes.put(ApplicationConstants.Keycloak.ACTIVE_BRANCH_ATTRIBUTE, context.branchId());
    }

    keycloakUserService.updateUserAttributes(keycloakUserId, attributes);

    return keycloakUserId;
  }

  private void createMembership(ProvisioningContext context, String keycloakUserId) {
    CreateMembershipRequest membershipRequest =
        membershipMapper.toRequest(
            context.organizationId(),
            context.organization().organizationName(),
            context.branchId(),
            context.branchName(),
            context.isPrimary(),
            context.roles());
    membershipService.create(keycloakUserId, membershipRequest);
  }

  private Branch resolveBranch(CreateUserRequest request, Set<UserRole> targetRoles) {
    if (request.branchId() != null && !request.branchId().isBlank()) {
      return new Branch(request.branchId(), request.branchName());
    }

    boolean hasOrganizationLevelRole =
        targetRoles.stream().anyMatch(RBACUtilities::isOrganizationRole);
    boolean hasBranchLevelRole = targetRoles.stream().anyMatch(RBACUtilities::isBranchRole);

    if (hasOrganizationLevelRole && !hasBranchLevelRole) {
      return null;
    }

    if (hasBranchLevelRole) {
      String creatorBranch = ExecutionContext.getActiveBranch();
      if (creatorBranch != null && !creatorBranch.isBlank()) {
        return new Branch(creatorBranch, request.branchName());
      }
      throw new ValidationException(
          ResponseCode.INVALID_ARGUMENTS,
          "Branch ID is required for branch-level roles",
          "branchId",
          "null");
    }

    return null;
  }

  private Organization resolveOrganization(CreateUserRequest request) {
    if (request.organizationId() != null && !request.organizationId().isBlank()) {
      return new Organization(request.organizationId(), request.organizationName());
    }
    return new Organization(
        keycloakProperties.getBaseOrganizationId(), keycloakProperties.getBaseOrganizationName());
  }

  private Set<UserRole> resolveRoles(CreateUserRequest request) {
    if (request.roles() != null && !request.roles().isEmpty()) {
      return request.roles();
    }
    throw new ValidationException(
        ResponseCode.INVALID_ARGUMENTS, "At least one role must be specified", "roles", "null");
  }

  private void validateEmailNotRegistered(String email) {
    if (keycloakUserService.isEmailRegistered(email)) {
      throw new DuplicateResourceException(
          ResponseCode.DUPLICATE_RESOURCE, "Email already registered");
    }
  }

  private record Branch(String branchId, String branchName) {}

  private record Organization(String organizationId, String organizationName) {}

  private record ProvisioningContext(
      Organization organization, Branch branch, Boolean isPrimary, Set<UserRole> roles) {

    public String branchId() {
      return branch != null ? branch.branchId() : null;
    }

    public String branchName() {
      return branch != null ? branch.branchName() : null;
    }

    public String organizationId() {
      return organization.organizationId();
    }

    public String rolesCsv() {
      return UserRole.toCommaSeparated(roles);
    }
  }
}
