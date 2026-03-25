package com.ebikes.iam.controllers;

import com.ebikes.iam.dtos.requests.passwords.CompletePasswordResetRequest;
import com.ebikes.iam.dtos.requests.passwords.PasswordResetRequest;
import com.ebikes.iam.dtos.responses.api.SuccessResponse;
import com.ebikes.iam.services.verification.VerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/password-reset")
@RestController
@Slf4j
public class PasswordResetController {

    private final VerificationService verificationService;

    @PostMapping("/complete")
    public ResponseEntity<SuccessResponse<Void>> completePasswordReset(
            @Valid @RequestBody CompletePasswordResetRequest request) {
        verificationService.completePasswordReset(request.newPassword(), request.token());
        return ResponseEntity.ok(SuccessResponse.of(null, "Password reset successfully"));
    }

    @PostMapping
    public ResponseEntity<SuccessResponse<Void>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request) {
        log.debug("Password reset requested");
        verificationService.requestPasswordReset(request.email());
        return ResponseEntity.ok(
                SuccessResponse.of(
                        null, "If an account exists with this email, a password reset link has been sent."));
    }
}
