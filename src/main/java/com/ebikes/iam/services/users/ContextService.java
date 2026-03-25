package com.ebikes.iam.services.users;

import com.ebikes.iam.constants.ApplicationConstants;
import com.ebikes.iam.constants.EventConstants.EventTypes;
import com.ebikes.iam.constants.EventConstants.RoutingKeys;
import com.ebikes.iam.database.entities.Membership;
import com.ebikes.iam.enums.ResponseCode;
import com.ebikes.iam.exceptions.ResourceNotFoundException;
import com.ebikes.iam.services.keycloak.users.KeycloakUserService;
import com.ebikes.iam.support.audit.AuditContext;
import com.ebikes.iam.support.audit.AuditMetadataBuilder;
import com.ebikes.iam.support.audit.AuditTemplate;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
@Slf4j
@Validated
public class ContextService {

    private static final String MEMBERSHIP = "MEMBERSHIP";

    private final AuditTemplate auditTemplate;
    private final KeycloakUserService keycloakUserService;
    private final MembershipService membershipService;

    @Transactional(readOnly = true)
    public Membership getActiveMembership(
            String activeBranchId, @NotBlank String keycloakUserId, String activeOrganizationId) {
        return resolveMembershipOrThrow(activeBranchId, keycloakUserId, activeOrganizationId);
    }

    @Transactional(readOnly = true)
    public List<Membership> getSwitchableMemberships(@NotBlank String keycloakUserId) {
        return membershipService.findMembershipsByKeycloakUserId(keycloakUserId);
    }

    @Transactional
    public void switchActiveMembership(
            String branchId, String keycloakUserId, String organizationId) {
        Membership membership = resolveMembershipOrThrow(branchId, keycloakUserId, organizationId);

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
                        EventTypes.IAM.CONTEXT_SWITCHED,
                        AuditMetadataBuilder.forMembership(membership),
                        organizationId,
                        RoutingKeys.IAM_CONTEXT_AUDIT);

        auditTemplate.execute(
                context, () -> keycloakUserService.updateUserAttributes(keycloakUserId, attributes));

        log.info(
                "User switched membership: keycloakUserId={}, organizationId={}, branchId={}, roles={}",
                keycloakUserId,
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
                                            ? "organization " + organizationId + " and branch " + branchId
                                            : "organization " + organizationId;
                            return new ResourceNotFoundException(
                                    ResponseCode.RESOURCE_NOT_FOUND, "User is not a member of " + scope);
                        });
    }
}
