package com.ebikes.iam.support.fixtures;

import com.ebikes.iam.dtos.requests.passwords.CompletePasswordResetRequest;
import com.ebikes.iam.dtos.requests.passwords.PasswordResetRequest;
import com.ebikes.iam.dtos.requests.verification.CompleteAccountActivationRequest;
import com.ebikes.iam.dtos.requests.verification.CompleteEmailVerificationRequest;
import com.ebikes.iam.dtos.requests.verification.CompletePhoneVerificationRequest;
import com.ebikes.iam.dtos.requests.verification.EmailVerificationRequest;
import com.ebikes.iam.dtos.requests.verification.PhoneVerificationRequest;

import net.datafaker.Faker;

public final class VerificationRequestFixtures {

  private static final Faker FAKER = new Faker();

  private VerificationRequestFixtures() {}

  public static CompleteAccountActivationRequest completeAccountActivation() {
    return new CompleteAccountActivationRequest(FAKER.credentials().password(8, 20), uuid());
  }

  public static CompleteEmailVerificationRequest completeEmailVerification() {
    return new CompleteEmailVerificationRequest(sixDigitCode());
  }

  public static CompletePhoneVerificationRequest completePhoneVerification() {
    return new CompletePhoneVerificationRequest(sixDigitCode());
  }

  public static CompletePasswordResetRequest completePasswordReset() {
    return new CompletePasswordResetRequest(uuid(), FAKER.credentials().password(8, 20));
  }

  public static EmailVerificationRequest emailVerification() {
    return new EmailVerificationRequest(FAKER.internet().emailAddress());
  }

  public static PasswordResetRequest passwordReset() {
    return new PasswordResetRequest(FAKER.internet().emailAddress());
  }

  public static PhoneVerificationRequest phoneVerification() {
    return new PhoneVerificationRequest("+254" + FAKER.number().digits(9));
  }

  private static String sixDigitCode() {
    return FAKER.number().digits(6);
  }

  private static String uuid() {
    return java.util.UUID.randomUUID().toString();
  }
}
