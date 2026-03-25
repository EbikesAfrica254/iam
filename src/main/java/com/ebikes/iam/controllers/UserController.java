package com.ebikes.iam.controllers;

import com.ebikes.iam.dtos.requests.filters.UserExtensionFilter;
import com.ebikes.iam.dtos.requests.users.CreateUserRequest;
import com.ebikes.iam.dtos.requests.users.SignupRequest;
import com.ebikes.iam.dtos.requests.users.UpdateUserExtensionRequest;
import com.ebikes.iam.dtos.responses.api.PaginatedResponse;
import com.ebikes.iam.dtos.responses.api.SuccessResponse;
import com.ebikes.iam.dtos.responses.users.UserExtensionDetailResponse;
import com.ebikes.iam.dtos.responses.users.UserExtensionSummaryResponse;
import com.ebikes.iam.dtos.responses.users.UserProfileResponse;
import com.ebikes.iam.enums.UserStatus;
import com.ebikes.iam.services.users.UserCreationAuthorizationService;
import com.ebikes.iam.services.users.UserExtensionService;
import com.ebikes.iam.services.users.UserProvisioningService;
import com.ebikes.iam.support.context.ExecutionContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RequiredArgsConstructor
@RequestMapping("/users")
@RestController
public class UserController {

    private final UserCreationAuthorizationService userCreationAuthorizationService;
    private final UserExtensionService userExtensionService;
    private final UserProvisioningService userProvisioningService;

    @PostMapping
    public ResponseEntity<SuccessResponse<Void>> create(
            @Valid @RequestBody CreateUserRequest request) {
        userCreationAuthorizationService.authorize(
                ExecutionContext.getUserId(),
                request.branchId(),
                request.organizationId(),
                request.roles());
        userProvisioningService.provisionUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SuccessResponse.of(null, "User created successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<SuccessResponse<Void>> delete(@PathVariable UUID id) {
        userExtensionService.delete(id);
        return ResponseEntity.ok(SuccessResponse.of(null, "User deleted successfully"));
    }

    @DeleteMapping("/{id}/deprovision")
    public ResponseEntity<SuccessResponse<Void>> deprovision(@PathVariable UUID id) {
        userExtensionService.deprovision(id);
        return ResponseEntity.ok(SuccessResponse.of(null, "User deprovisioned successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SuccessResponse<UserExtensionDetailResponse>> findById(
            @PathVariable UUID id) {
        UserExtensionDetailResponse user = userExtensionService.findById(id);
        return ResponseEntity.ok(SuccessResponse.of(user));
    }

    @GetMapping("/me")
    public ResponseEntity<SuccessResponse<UserProfileResponse>> me() {
        return ResponseEntity.ok(SuccessResponse.of(userExtensionService.me()));
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<SuccessResponse<Void>> restore(@PathVariable UUID id) {
        userExtensionService.restore(id);
        return ResponseEntity.ok(SuccessResponse.of(null, "User restored successfully"));
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<UserExtensionSummaryResponse>> search(
            @ModelAttribute UserExtensionFilter filter) {
        Page<UserExtensionSummaryResponse> page = userExtensionService.search(filter);
        return ResponseEntity.ok(PaginatedResponse.from("Users retrieved successfully.", page));
    }

    @PostMapping("/signup")
    public ResponseEntity<SuccessResponse<Void>> signup(@Valid @RequestBody SignupRequest request) {
        userProvisioningService.provisionSignup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        SuccessResponse.of(
                                null, "Registration successful. Please check your email for verification."));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SuccessResponse<UserExtensionDetailResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateUserExtensionRequest request) {
        UserExtensionDetailResponse user = userExtensionService.update(id, request);
        return ResponseEntity.ok(SuccessResponse.of(user, "User updated successfully"));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<SuccessResponse<Void>> updateStatus(
            @PathVariable UUID id, @RequestParam UserStatus status) {
        userExtensionService.updateStatus(id, status);
        return ResponseEntity.ok(SuccessResponse.of(null, "User status updated successfully"));
    }
}
