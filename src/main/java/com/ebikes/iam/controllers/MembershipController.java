package com.ebikes.iam.controllers;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ebikes.iam.dtos.requests.memberships.CreateMembershipRequest;
import com.ebikes.iam.dtos.requests.memberships.UpdateMembershipRolesRequest;
import com.ebikes.iam.dtos.responses.api.SuccessResponse;
import com.ebikes.iam.dtos.responses.memberships.MembershipResponse;
import com.ebikes.iam.services.users.membership.MembershipService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RequestMapping("/memberships/{keycloakUserId}")
@RestController
public class MembershipController {

  private final MembershipService membershipService;

  @PostMapping
  public ResponseEntity<SuccessResponse<MembershipResponse>> create(
      @PathVariable String keycloakUserId, @Valid @RequestBody CreateMembershipRequest request) {

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            SuccessResponse.of(
                membershipService.create(keycloakUserId, request),
                "Membership created successfully"));
  }

  @GetMapping
  public ResponseEntity<SuccessResponse<List<MembershipResponse>>> list(
      @PathVariable String keycloakUserId) {

    return ResponseEntity.ok(
        SuccessResponse.of(membershipService.findMembershipsByKeycloakUserId(keycloakUserId)));
  }

  @DeleteMapping
  public ResponseEntity<SuccessResponse<Void>> remove(
      @PathVariable String keycloakUserId,
      @RequestParam String organizationId,
      @RequestParam(required = false) String branchId) {

    membershipService.removeMembership(branchId, keycloakUserId, organizationId);
    return ResponseEntity.ok(SuccessResponse.of(null, "Membership removed successfully"));
  }

  @DeleteMapping("/organizations/{organizationId}")
  public ResponseEntity<SuccessResponse<Void>> removeFromOrganization(
      @PathVariable String keycloakUserId, @PathVariable String organizationId) {

    membershipService.removeUserFromOrganization(keycloakUserId, organizationId);
    return ResponseEntity.ok(
        SuccessResponse.of(null, "User removed from organizations successfully"));
  }

  @PutMapping("/primary")
  public ResponseEntity<SuccessResponse<Void>> setPrimary(
      @PathVariable String keycloakUserId, @RequestParam String organizationId) {

    membershipService.setPrimaryMembership(keycloakUserId, organizationId);
    return ResponseEntity.ok(SuccessResponse.of(null, "Primary membership set successfully"));
  }

  @PatchMapping("/roles")
  public ResponseEntity<SuccessResponse<Void>> updateRoles(
      @PathVariable String keycloakUserId,
      @RequestParam String organizationId,
      @RequestParam(required = false) String branchId,
      @Valid @RequestBody UpdateMembershipRolesRequest request) {

    membershipService.setRoles(branchId, keycloakUserId, organizationId, request.roles());
    return ResponseEntity.ok(SuccessResponse.of(null, "Membership roles updated successfully"));
  }
}
