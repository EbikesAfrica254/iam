package com.ebikes.iam.services.users.context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.ebikes.iam.adapters.keycloak.KeycloakUserAdapter;
import com.ebikes.iam.constants.ApplicationConstants;
import com.ebikes.iam.constants.EventConstants.AuditEvents;
import com.ebikes.iam.constants.EventConstants.DomainEvents;
import com.ebikes.iam.database.entities.Membership;
import com.ebikes.iam.dtos.responses.context.ContextResponse;
import com.ebikes.iam.dtos.responses.memberships.MembershipResponse;
import com.ebikes.iam.enums.ResponseCode;
import com.ebikes.iam.exceptions.ResourceNotFoundException;
import com.ebikes.iam.services.users.membership.MembershipService;
import com.ebikes.iam.support.audit.AuditContext;
import com.ebikes.iam.support.audit.AuditMetadataBuilder;
import com.ebikes.iam.support.audit.AuditTemplate;
import com.ebikes.iam.support.context.ExecutionContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
@Validated
public class ContextService {

  private static final String MEMBERSHIP = "MEMBERSHIP";

  private final AuditTemplate auditTemplate;
  private final KeycloakUserAdapter keycloakUserAdapter;
  private final MembershipService membershipService;

  @Transactional(readOnly = true)
  public ContextResponse getCurrentContext() {
    if (!(ExecutionContext.get() instanceof ExecutionContext.UserContext ctx)) {
      throw new IllegalStateException("getCurrentContext() called outside of user context");
    }
    Membership membership =
        resolveMembershipOrThrow(ctx.activeBranch(), ctx.userId(), ctx.activeOrganization());
    return new ContextResponse(ctx.activeBranch(), ctx.activeOrganization(), membership.getRoles());
  }

  @Transactional(readOnly = true)
  public List<MembershipResponse> getSwitchableMemberships() {
    if (!(ExecutionContext.get() instanceof ExecutionContext.UserContext ctx)) {
      throw new IllegalStateException("getSwitchableMemberships() called outside of user context");
    }
    return membershipService.findMembershipsByKeycloakUserId(ctx.userId());
  }

  @Transactional
  public void switchActiveMembership(String branchId, String organizationId) {
    if (!(ExecutionContext.get() instanceof ExecutionContext.UserContext ctx)) {
      throw new IllegalStateException("switchActiveMembership() called outside of user context");
    }
    Membership membership = resolveMembershipOrThrow(branchId, ctx.userId(), organizationId);

    Map<String, String> attributes = new HashMap<>();
    attributes.put(ApplicationConstants.Keycloak.ACTIVE_ORGANIZATION_ATTRIBUTE, organizationId);
    attributes.put(
        ApplicationConstants.Keycloak.ACTIVE_ORGANIZATION_ROLES_ATTRIBUTE,
        String.join(",", membership.getRoles()));

    if (branchId != null) {
      attributes.put(ApplicationConstants.Keycloak.ACTIVE_BRANCH_ATTRIBUTE, branchId);
    }

    AuditContext context =
        new AuditContext(
            membership.getId(),
            MEMBERSHIP,
            DomainEvents.Context.SWITCHED,
            AuditMetadataBuilder.forMembership(membership),
            organizationId,
            AuditEvents.CONTEXT);

    auditTemplate.execute(
        context, () -> keycloakUserAdapter.updateUserAttributes(ctx.userId(), attributes));

    log.info(
        "User switched membership: keycloakUserId={}, organizationId={}, branchId={}, roles={}",
        ctx.userId(),
        organizationId,
        branchId != null ? branchId : "none",
        String.join(",", membership.getRoles()));
  }

  private Membership resolveMembershipOrThrow(
      String branchId, String keycloakUserId, String organizationId) {
    return membershipService
        .findMembershipInScope(branchId, keycloakUserId, organizationId)
        .orElseThrow(
            () -> {
              String scope =
                  branchId != null
                      ? "organizations " + organizationId + " and branch " + branchId
                      : "organizations " + organizationId;
              return new ResourceNotFoundException(
                  ResponseCode.RESOURCE_NOT_FOUND, "User is not a member of " + scope);
            });
  }
}
