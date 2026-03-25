package com.ebikes.iam.controllers;

import com.ebikes.iam.database.entities.Membership;
import com.ebikes.iam.dtos.requests.context.SwitchContextRequest;
import com.ebikes.iam.dtos.responses.api.SuccessResponse;
import com.ebikes.iam.dtos.responses.context.ContextResponse;
import com.ebikes.iam.dtos.responses.memberships.MembershipResponse;
import com.ebikes.iam.mappers.MembershipMapper;
import com.ebikes.iam.services.users.ContextService;
import com.ebikes.iam.support.context.ExecutionContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/contexts")
@RestController
public class ContextController {

  private final ContextService contextService;
  private final MembershipMapper membershipMapper;

  @GetMapping("/available")
  public ResponseEntity<SuccessResponse<List<MembershipResponse>>> getAvailableContexts() {
    String keycloakUserId = ExecutionContext.getUserId();

    List<MembershipResponse> response =
        contextService.getSwitchableMemberships(keycloakUserId).stream()
            .map(membershipMapper::toResponse)
            .toList();

    return ResponseEntity.ok(SuccessResponse.of(response));
  }

  @GetMapping("/current")
  public ResponseEntity<SuccessResponse<ContextResponse>> getCurrentContext() {
    String keycloakUserId = ExecutionContext.getUserId();
    String activeOrganizationId = ExecutionContext.getActiveOrganization();
    String activeBranchId = ExecutionContext.getActiveBranch();

    Membership membership =
        contextService.getActiveMembership(activeBranchId, keycloakUserId, activeOrganizationId);

    ContextResponse response =
        new ContextResponse(activeOrganizationId, activeBranchId, membership.getRoles());

    return ResponseEntity.ok(SuccessResponse.of(response));
  }

  @PostMapping("/switch")
  public ResponseEntity<SuccessResponse<Void>> switchContext(
      @Valid @RequestBody SwitchContextRequest request) {

    String keycloakUserId = ExecutionContext.getUserId();

    contextService.switchActiveMembership(
        request.branchId(), keycloakUserId, request.organizationId());

    return ResponseEntity.ok(
        SuccessResponse.of(
            null,
            "Tenant context switched successfully. Please refresh your authentication token to"
                + " receive updated claims."));
  }
}
