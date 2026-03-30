package com.ebikes.iam.services.users.membership;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.ebikes.iam.adapters.keycloak.KeycloakGroupAdapter;
import com.ebikes.iam.configurations.properties.KeycloakProperties;
import com.ebikes.iam.constants.EventConstants.AuditEvents;
import com.ebikes.iam.constants.EventConstants.DomainEvents;
import com.ebikes.iam.database.entities.Membership;
import com.ebikes.iam.database.entities.UserExtension;
import com.ebikes.iam.database.repositories.MembershipRepository;
import com.ebikes.iam.dtos.requests.memberships.CreateMembershipRequest;
import com.ebikes.iam.dtos.responses.memberships.MembershipResponse;
import com.ebikes.iam.enums.ResponseCode;
import com.ebikes.iam.enums.UserRole;
import com.ebikes.iam.exceptions.BusinessRuleException;
import com.ebikes.iam.exceptions.ResourceNotFoundException;
import com.ebikes.iam.exceptions.ValidationException;
import com.ebikes.iam.mappers.MembershipEnricher;
import com.ebikes.iam.services.users.UserExtensionService;
import com.ebikes.iam.support.audit.AuditContext;
import com.ebikes.iam.support.audit.AuditMetadataBuilder;
import com.ebikes.iam.support.audit.AuditTemplate;
import com.ebikes.iam.support.keycloak.GroupPathUtilities;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
@Validated
public class MembershipService {

  private static final String MEMBERSHIP = "MEMBERSHIP";

  private final AuditTemplate auditTemplate;
  private final KeycloakGroupAdapter keycloakGroupAdapter;
  private final KeycloakProperties keycloakProperties;
  private final MembershipEnricher membershipEnricher;
  private final MembershipRepository repository;
  private final UserExtensionService userExtensionService;

  @Transactional
  public MembershipResponse create(String keycloakUserId, CreateMembershipRequest request) {
    UserExtension userExtension =
        userExtensionService.findUserExtensionByKeycloakUserId(keycloakUserId);

    String keycloakGroupPath = GroupPathUtilities.generate(request.organizationId());

    keycloakGroupAdapter.addUserToGroup(keycloakGroupPath, keycloakUserId);

    Membership membership =
        this.createRecord(keycloakUserId, keycloakGroupPath, request, userExtension);

    log.info(
        "Membership created: keycloakUserId={}, organizationId={}, branchId={}, isPrimary={}",
        keycloakUserId,
        request.organizationId(),
        request.branchId() != null ? request.branchId() : "none",
        request.isPrimary());

    return membershipEnricher.enrich(List.of(membership)).getFirst();
  }

  @Transactional(readOnly = true)
  public List<MembershipResponse> findMembershipsByKeycloakUserId(String keycloakUserId) {
    return membershipEnricher.enrich(repository.findByKeycloakUserId(keycloakUserId));
  }

  @Transactional(readOnly = true)
  public Optional<Membership> findMembershipInScope(
      String branchId, String keycloakUserId, String organizationId) {
    return queryMembershipInScope(branchId, keycloakUserId, organizationId);
  }

  @Transactional
  public void removeMembership(String branchId, String keycloakUserId, String organizationId) {
    if (organizationId.equals(keycloakProperties.getBaseOrganizationId())) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE, "Cannot remove membership from base organizations");
    }

    Membership membership = findMembershipOrThrow(branchId, keycloakUserId, organizationId);

    AuditContext context =
        new AuditContext(
            membership.getId(),
            MEMBERSHIP,
            DomainEvents.Membership.REMOVED,
            AuditMetadataBuilder.forMembership(membership),
            organizationId,
            AuditEvents.MEMBERSHIP);

    auditTemplate.execute(
        context,
        () -> {
          repository.delete(membership);

          boolean hasRemainingMemberships =
              repository.existsByKeycloakUserIdAndOrganizationIdAndBranchIdIsNull(
                  keycloakUserId, organizationId);

          if (!hasRemainingMemberships) {
            String keycloakGroupPath = GroupPathUtilities.generate(organizationId);
            keycloakGroupAdapter.removeUserFromGroup(keycloakGroupPath, keycloakUserId);
            log.info(
                "Removed from Keycloak group: keycloakUserId={}, organizationId={}",
                keycloakUserId,
                organizationId);
          }

          log.info(
              "Membership removed: keycloakUserId={}, organizationId={}, branchId={},"
                  + " hasRemainingMemberships={}",
              keycloakUserId,
              organizationId,
              branchId != null ? branchId : "none",
              hasRemainingMemberships);
        });
  }

  @Transactional
  public void removeUserFromOrganization(String keycloakUserId, String organizationId) {
    if (organizationId.equals(keycloakProperties.getBaseOrganizationId())) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE, "Cannot remove user from base organizations");
    }

    List<Membership> memberships =
        repository.findAllByKeycloakUserIdAndOrganizationId(keycloakUserId, organizationId);

    if (memberships.isEmpty()) {
      throw new ResourceNotFoundException(
          ResponseCode.RESOURCE_NOT_FOUND, "User membership not found");
    }

    Membership representative = memberships.getFirst();
    String keycloakGroupPath = GroupPathUtilities.generate(organizationId);

    AuditContext context =
        new AuditContext(
            representative.getId(),
            MEMBERSHIP,
            DomainEvents.Membership.ORGANIZATION_REMOVED,
            AuditMetadataBuilder.forMembership(
                representative, Map.of("membershipCount", String.valueOf(memberships.size()))),
            organizationId,
            AuditEvents.MEMBERSHIP);

    auditTemplate.execute(
        context,
        () -> {
          keycloakGroupAdapter.removeUserFromGroup(keycloakGroupPath, keycloakUserId);
          repository.deleteAll(memberships);
        });

    log.info(
        "User removed from organizations: keycloakUserId={}, organizationId={}, membershipCount={}",
        keycloakUserId,
        organizationId,
        memberships.size());
  }

  @Transactional
  public void setPrimaryMembership(String keycloakUserId, String organizationId) {
    Membership membership = findMembershipOrThrow(null, keycloakUserId, organizationId);

    AuditContext context =
        new AuditContext(
            membership.getId(),
            MEMBERSHIP,
            DomainEvents.Membership.PRIMARY_CHANGED,
            AuditMetadataBuilder.forMembership(membership),
            organizationId,
            AuditEvents.MEMBERSHIP);

    auditTemplate.execute(
        context,
        () -> {
          clearPrimaryMembership(keycloakUserId);
          membership.markAsPrimary();
          repository.save(membership);
        });

    log.info(
        "Primary membership set: keycloakUserId={}, organizationId={}",
        keycloakUserId,
        organizationId);
  }

  @Transactional
  public void setRoles(
      String branchId, String keycloakUserId, String organizationId, Set<UserRole> roles) {
    Membership membership = findMembershipOrThrow(branchId, keycloakUserId, organizationId);

    Set<String> roleNames = roles.stream().map(UserRole::name).collect(Collectors.toSet());

    AuditContext context =
        new AuditContext(
            membership.getId(),
            MEMBERSHIP,
            DomainEvents.Membership.UPDATED,
            AuditMetadataBuilder.forMembership(membership),
            organizationId,
            AuditEvents.MEMBERSHIP);

    auditTemplate.execute(
        context,
        () -> {
          membership.updateRoles(roleNames);
          repository.save(membership);
        });

    log.info(
        "Roles updated: keycloakUserId={}, organizationId={}, branchId={}, roles={}",
        keycloakUserId,
        organizationId,
        branchId != null ? branchId : "none",
        String.join(",", roleNames));
  }

  public Membership createRecord(
      String keycloakUserId,
      String keycloakGroupPath,
      CreateMembershipRequest request,
      UserExtension userExtension) {

    validateNoDuplicateMembership(keycloakUserId, request);

    if (Boolean.TRUE.equals(request.isPrimary())) {
      clearPrimaryMembership(keycloakUserId);
    }

    Set<String> roles = request.roles().stream().map(UserRole::name).collect(Collectors.toSet());

    AuditContext context =
        new AuditContext(
            userExtension.getId(),
            MEMBERSHIP,
            DomainEvents.Membership.CREATED,
            null,
            request.organizationId(),
            AuditEvents.MEMBERSHIP);

    return auditTemplate.execute(
        context,
        () ->
            repository.save(
                Membership.builder()
                    .branchId(request.branchId())
                    .isPrimary(request.isPrimary() != null && request.isPrimary())
                    .keycloakGroupPath(keycloakGroupPath)
                    .keycloakUserId(keycloakUserId)
                    .organizationId(request.organizationId())
                    .roles(roles)
                    .userExtension(userExtension)
                    .build()));
  }

  private void clearPrimaryMembership(String keycloakUserId) {
    List<Membership> primaryMemberships =
        repository.findByIsPrimaryAndKeycloakUserId(true, keycloakUserId);

    if (primaryMemberships.size() > 1) {
      log.warn(
          "Data integrity violation: {} primary memberships found for keycloakUserId={}."
              + " Clearing all before reassignment.",
          primaryMemberships.size(),
          keycloakUserId);
    }

    if (!primaryMemberships.isEmpty()) {
      primaryMemberships.forEach(Membership::clearPrimary);
      repository.saveAll(primaryMemberships);
    }
  }

  private Membership findMembershipOrThrow(
      String branchId, String keycloakUserId, String organizationId) {
    return queryMembershipInScope(branchId, keycloakUserId, organizationId)
        .orElseThrow(
            () ->
                branchId != null
                    ? new ResourceNotFoundException(
                        ResponseCode.RESOURCE_NOT_FOUND,
                        "Membership not found for organizations and branch")
                    : new ResourceNotFoundException(
                        ResponseCode.RESOURCE_NOT_FOUND,
                        "Organization-level membership not found"));
  }

  private Optional<Membership> queryMembershipInScope(
      String branchId, String keycloakUserId, String organizationId) {
    return branchId != null
        ? repository.findByKeycloakUserIdAndOrganizationIdAndBranchId(
            keycloakUserId, organizationId, branchId)
        : repository.findByKeycloakUserIdAndOrganizationIdAndBranchIdIsNull(
            keycloakUserId, organizationId);
  }

  private void validateNoDuplicateMembership(
      String keycloakUserId, CreateMembershipRequest request) {
    if (request.branchId() != null) {
      if (repository.existsByBranchIdAndKeycloakUserIdAndOrganizationId(
          request.branchId(), keycloakUserId, request.organizationId())) {
        throw new ValidationException(
            ResponseCode.INVALID_ARGUMENTS,
            "Branch membership already exists",
            "branchId",
            request.branchId());
      }
    } else {
      if (repository.existsByKeycloakUserIdAndOrganizationIdAndBranchIdIsNull(
          keycloakUserId, request.organizationId())) {
        throw new ValidationException(
            ResponseCode.INVALID_ARGUMENTS,
            "User already has organizations-level membership",
            "organizationId",
            request.organizationId());
      }
    }
  }
}
