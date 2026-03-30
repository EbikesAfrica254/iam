package com.ebikes.iam.services.verification;

import org.springframework.stereotype.Component;

import com.ebikes.iam.configurations.properties.ClientProperties;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Component
@Slf4j
public class VerificationLinkBuilder {

  private ClientProperties clientProperties;
  private static final String TOKEN_PARAM = "?token=";

  public String buildAccountActivationLink(String token) {
    String link =
        clientProperties.getBaseUrl()
            + clientProperties.getVerification().getAccountActivation().getPath()
            + TOKEN_PARAM
            + token;
    log.debug("Built account activation link");
    return link;
  }

  public String buildEmailVerificationLink(String token) {
    String link =
        clientProperties.getBaseUrl()
            + clientProperties.getVerification().getEmail().getPath()
            + TOKEN_PARAM
            + token;
    log.debug("Built email verification link");
    return link;
  }

  public String buildPasswordResetLink(String token) {
    String link =
        clientProperties.getBaseUrl()
            + clientProperties.getVerification().getPasswordReset().getPath()
            + TOKEN_PARAM
            + token;
    log.debug("Built password reset link");
    return link;
  }
}
