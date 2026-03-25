package com.ebikes.iam.services.users;

import com.ebikes.iam.constants.EventConstants.EventTypes;
import com.ebikes.iam.constants.EventConstants.RoutingKeys;
import com.ebikes.iam.database.entities.Membership;
import com.ebikes.iam.database.entities.UserExtension;
import com.ebikes.iam.database.repositories.UserExtensionRepository;
import com.ebikes.iam.database.specifications.UserExtensionSpecifications;
import com.ebikes.iam.dtos.requests.filters.UserExtensionFilter;
import com.ebikes.iam.dtos.requests.users.CreateUserRequest;
import com.ebikes.iam.dtos.requests.users.UpdateUserExtensionRequest;
import com.ebikes.iam.dtos.responses.users.UserExtensionDetailResponse;
import com.ebikes.iam.dtos.responses.users.UserExtensionSummaryResponse;
import com.ebikes.iam.dtos.responses.users.UserProfileResponse;
import com.ebikes.iam.enums.ResponseCode;
import com.ebikes.iam.enums.UserStatus;
import com.ebikes.iam.exceptions.ResourceNotFoundException;
import com.ebikes.iam.exceptions.ValidationException;
import com.ebikes.iam.mappers.UserExtensionMapper;
import com.ebikes.iam.services.keycloak.users.KeycloakUserService;
import com.ebikes.iam.support.audit.AuditContext;
import com.ebikes.iam.support.audit.AuditMetadataBuilder;
import com.ebikes.iam.support.audit.AuditTemplate;
import com.ebikes.iam.support.context.ExecutionContext;
import com.ebikes.iam.support.database.FilterUtilities;
import com.ebikes.iam.support.security.RBACUtilities;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserExtensionService {

  private static final String USER_EXTENSION = "USER_EXTENSION";

  private final AuditTemplate auditTemplate;
  private final KeycloakUserService keycloakUserService;
  private final UserExtensionMapper mapper;
  private final UserExtensionRepository repository;

  @Transactional
  public void activate(UserExtension userExtension) {
    userExtension.activate();
    repository.save(userExtension);
  }

  @Transactional
  public UserExtension create(
      String keycloakUserId, String organizationId, CreateUserRequest request) {
    UserExtension userExtension =
        UserExtension.builder()
            .branchId(request.branchId())
            .countryCode(request.countryCode())
            .email(request.email())
            .firstName(request.firstName())
            .keycloakUserId(keycloakUserId)
            .lastName(request.lastName())
            .organizationId(organizationId)
            .phoneNumber(request.phoneNumber())
            .status(UserStatus.INACTIVE)
            .username(request.username())
            .build();

    userExtension = repository.save(userExtension);

    log.info(
        "User extension created: userId={}, keycloakUserId={}",
        userExtension.getId(),
        keycloakUserId);

    return userExtension;
  }

  @Transactional
  public void delete(UUID id) {
    UserExtension userExtension = findNonDeletedUser(id);

    AuditContext context =
        new AuditContext(
            userExtension.getId(),
            USER_EXTENSION,
            EventTypes.IAM.ACCOUNT_DELETED,
            AuditMetadataBuilder.forUserExtension(userExtension),
            userExtension.getOrganizationId(),
            RoutingKeys.IAM_ACCOUNT_AUDIT);

    auditTemplate.execute(
        context,
        () -> {
          keycloakUserService.disableUser(userExtension.getKeycloakUserId());
          userExtension.delete();
          repository.save(userExtension);
        });

    log.info("User deleted: userId={}, keycloakUserId={}", id, userExtension.getKeycloakUserId());
  }

  @Transactional
  public void deprovision(UUID id) {
    UserExtension userExtension = findNonDeletedUser(id);
    String keycloakUserId = userExtension.getKeycloakUserId();

    AuditContext context =
        new AuditContext(
            userExtension.getId(),
            USER_EXTENSION,
            EventTypes.IAM.USER_DEPROVISIONED,
            AuditMetadataBuilder.forUserExtension(userExtension),
            userExtension.getOrganizationId(),
            RoutingKeys.IAM_USER_AUDIT);

    auditTemplate.execute(
        context,
        () -> {
          keycloakUserService.deleteUser(keycloakUserId);
          userExtension.delete();
          repository.save(userExtension);
        });

    log.info(
        "User deprovisioned: userId={}, keycloakUserId={}", userExtension.getId(), keycloakUserId);
  }

  @Transactional(readOnly = true)
  public UserExtensionDetailResponse findById(UUID id) {
    UserExtension userExtension = findUserExtensionById(id);
    if (userExtension.isDeleted()
        && !RBACUtilities.hasAdminRoleFromNames(ExecutionContext.getRoles())) {
      throw new ResourceNotFoundException(
          ResponseCode.RESOURCE_NOT_FOUND, "User with ID '" + id + "' does not exist");
    }

    return mapper.toDetailResponse(userExtension);
  }

  @Transactional(readOnly = true)
  public Optional<UserExtension> findUserExtensionByEmail(String email) {
    return repository.findByEmail(email);
  }

  @Transactional(readOnly = true)
  public UserExtension findUserExtensionByKeycloakUserId(String keycloakUserId) {
    return repository
        .findByKeycloakUserId(keycloakUserId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ResponseCode.RESOURCE_NOT_FOUND,
                    "User with Keycloak ID '" + keycloakUserId + "' does not exist"));
  }

  @Transactional(readOnly = true)
  public Optional<UserExtension> findUserExtensionByPhoneNumber(String phoneNumber) {
    return repository.findByPhoneNumber(phoneNumber);
  }

  @Transactional(readOnly = true)
  public UserProfileResponse me() {
    String keycloakUserId = ExecutionContext.getUserId();
    String activeOrganizationId = ExecutionContext.getActiveOrganization();

    UserExtension user =
        repository
            .findByKeycloakUserIdWithMemberships(keycloakUserId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        ResponseCode.RESOURCE_NOT_FOUND, "User not found"));

    Membership activeMembership =
        findActiveMembership(
                user.getMemberships(), activeOrganizationId, ExecutionContext.getActiveBranch())
            .orElseThrow(() -> new IllegalStateException("No active membership found for user"));

    return mapper.toProfileResponse(user, activeMembership, List.copyOf(user.getMemberships()));
  }

  @Transactional
  public void restore(UUID id) {
    UserExtension userExtension = findUserExtensionById(id);

    if (!userExtension.isDeleted()) {
      throw new ValidationException(
          ResponseCode.INVALID_STATE, "User is not deleted", "id", id.toString());
    }

    AuditContext context =
        new AuditContext(
            userExtension.getId(),
            USER_EXTENSION,
            EventTypes.IAM.ACCOUNT_RESTORED,
            AuditMetadataBuilder.forUserExtension(userExtension),
            userExtension.getOrganizationId(),
            RoutingKeys.IAM_ACCOUNT_AUDIT);

    auditTemplate.execute(
        context,
        () -> {
          userExtension.restore();
          repository.save(userExtension);
        });

    log.info("User restored: userId={}, keycloakUserId={}", id, userExtension.getKeycloakUserId());
  }

  @Transactional(readOnly = true)
  public Page<UserExtensionSummaryResponse> search(UserExtensionFilter filter) {
    Specification<UserExtension> spec = UserExtensionSpecifications.buildSpecification(filter);
    Pageable pageable =
        FilterUtilities.buildPageable(filter, UserExtensionSpecifications.ALLOWED_SORT_FIELDS);
    return repository.findAll(spec, pageable).map(mapper::toSummaryResponse);
  }

  @Transactional
  public UserExtensionDetailResponse update(UUID id, UpdateUserExtensionRequest request) {
    UserExtension userExtension = findNonDeletedUser(id);

    AuditContext context =
        new AuditContext(
            userExtension.getId(),
            USER_EXTENSION,
            EventTypes.IAM.ACCOUNT_UPDATED,
            AuditMetadataBuilder.forUserExtension(userExtension),
            userExtension.getOrganizationId(),
            RoutingKeys.IAM_ACCOUNT_AUDIT);

    UserExtensionDetailResponse response =
        auditTemplate.execute(
            context,
            () -> {
              keycloakUserService.updateUser(userExtension.getKeycloakUserId(), request);
              userExtension.update(request);
              return mapper.toDetailResponse(repository.save(userExtension));
            });

    log.info("User updated: userId={}, keycloakUserId={}", id, userExtension.getKeycloakUserId());

    return response;
  }

  @Transactional
  public void updateStatus(UUID id, UserStatus newStatus) {
    UserExtension userExtension = findNonDeletedUser(id);
    UserStatus oldStatus = userExtension.getStatus();

    AuditContext context =
        new AuditContext(
            userExtension.getId(),
            USER_EXTENSION,
            EventTypes.IAM.ACCOUNT_STATUS_CHANGED,
            AuditMetadataBuilder.forUserExtension(
                userExtension, Map.of("previousStatus", oldStatus.name())),
            userExtension.getOrganizationId(),
            RoutingKeys.IAM_ACCOUNT_AUDIT);

    auditTemplate.execute(
        context,
        () -> {
          userExtension.updateStatus(newStatus);
          repository.save(userExtension);
        });

    log.info(
        "User status updated: userId={}, oldStatus={}, newStatus={}", id, oldStatus, newStatus);
  }

  @Transactional
  public void verifyEmail(UserExtension userExtension) {
    userExtension.verifyEmail();
    repository.save(userExtension);
  }

  @Transactional
  public void verifyPhone(UserExtension userExtension) {
    userExtension.verifyPhone();
    repository.save(userExtension);
  }

  private Optional<Membership> findActiveMembership(
      Set<Membership> memberships, String organizationId, String branchId) {
    return memberships.stream()
        .filter(m -> Objects.equals(m.getOrganizationId(), organizationId))
        .filter(
            m ->
                (branchId == null || branchId.isBlank())
                    ? m.getBranchId() == null
                    : Objects.equals(m.getBranchId(), branchId))
        .findFirst();
  }

  private UserExtension findNonDeletedUser(UUID id) {
    UserExtension userExtension = findUserExtensionById(id);

    if (userExtension.isDeleted()) {
      throw new ResourceNotFoundException(
          ResponseCode.RESOURCE_NOT_FOUND, "User with ID '" + id + "' does not exist");
    }

    return userExtension;
  }

  public UserExtension findUserExtensionById(UUID id) {
    return repository
        .findById(id)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ResponseCode.RESOURCE_NOT_FOUND, "User with ID '" + id + "' does not exist"));
  }
}
