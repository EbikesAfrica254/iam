package com.ebikes.iam.controllers;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ebikes.iam.dtos.requests.context.SwitchContextRequest;
import com.ebikes.iam.dtos.responses.api.SuccessResponse;
import com.ebikes.iam.dtos.responses.context.ContextResponse;
import com.ebikes.iam.dtos.responses.memberships.MembershipResponse;
import com.ebikes.iam.services.users.context.ContextService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RequestMapping("/contexts")
@RestController
public class ContextController {

  private final ContextService contextService;

  @GetMapping("/available")
  public ResponseEntity<SuccessResponse<List<MembershipResponse>>> getAvailableContexts() {
    return ResponseEntity.ok(SuccessResponse.of(contextService.getSwitchableMemberships()));
  }

  @GetMapping("/current")
  public ResponseEntity<SuccessResponse<ContextResponse>> getCurrentContext() {
    return ResponseEntity.ok(SuccessResponse.of(contextService.getCurrentContext()));
  }

  @PostMapping("/switch")
  public ResponseEntity<SuccessResponse<Void>> switchContext(
      @Valid @RequestBody SwitchContextRequest request) {
    contextService.switchActiveMembership(request.branchId(), request.organizationId());
    return ResponseEntity.ok(
        SuccessResponse.of(
            null,
            "Tenant context switched successfully. Please refresh your authentication token to"
                + " receive updated claims."));
  }
}
