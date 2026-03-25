package com.ebikes.iam.controllers;

import com.ebikes.iam.dtos.requests.verification.CompleteAccountActivationRequest;
import com.ebikes.iam.dtos.requests.verification.CompleteEmailVerificationRequest;
import com.ebikes.iam.dtos.requests.verification.CompletePhoneVerificationRequest;
import com.ebikes.iam.dtos.requests.verification.EmailVerificationRequest;
import com.ebikes.iam.dtos.requests.verification.PhoneVerificationRequest;
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
@RequestMapping("/verification")
@RestController
@Slf4j
public class VerificationController {

  private final VerificationService verificationService;

  @PostMapping("/account/activate")
  public ResponseEntity<SuccessResponse<Void>> completeAccountActivation(
      @Valid @RequestBody CompleteAccountActivationRequest request) {
    log.info("Completing account activation");
    verificationService.completeAccountActivation(request.password(), request.token());
    return ResponseEntity.ok(
        SuccessResponse.of(null, "Email verified and password set successfully."));
  }

  @PostMapping("/email/complete")
  public ResponseEntity<SuccessResponse<Void>> completeEmailVerification(
      @Valid @RequestBody CompleteEmailVerificationRequest request) {
    log.info("Completing email verification");
    verificationService.completeEmailVerification(request.code());
    return ResponseEntity.ok(SuccessResponse.of(null, "Email OTP verified successfully."));
  }

  @PostMapping("/phone/complete")
  public ResponseEntity<SuccessResponse<Void>> completePhoneVerification(
      @Valid @RequestBody CompletePhoneVerificationRequest request) {
    log.info("Completing phone verification");
    verificationService.completePhoneNumberVerification(request.code());
    return ResponseEntity.ok(SuccessResponse.of(null, "Phone verified successfully."));
  }

  @PostMapping("/email/request")
  public ResponseEntity<SuccessResponse<Void>> requestEmailVerification(
      @Valid @RequestBody EmailVerificationRequest request) {
    log.debug("Requesting email verification");
    verificationService.requestEmailVerification(request.email());
    return ResponseEntity.ok(SuccessResponse.of(null, "Verification email sent successfully."));
  }

  @PostMapping("/phone/request")
  public ResponseEntity<SuccessResponse<Void>> requestPhoneVerification(
      @Valid @RequestBody PhoneVerificationRequest request) {
    log.debug("Requesting phone verification");
    verificationService.requestPhoneVerification(request.phoneNumber());
    return ResponseEntity.ok(SuccessResponse.of(null, "Verification code sent successfully."));
  }
}
