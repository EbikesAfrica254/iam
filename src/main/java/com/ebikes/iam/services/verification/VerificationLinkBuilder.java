package com.ebikes.iam.services.verification;

import org.springframework.stereotype.Component;

import com.ebikes.iam.configurations.properties.WebClientProperties;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Component
@Slf4j
public class VerificationLinkBuilder {

    private WebClientProperties webClientProperties;

    public String buildEmailVerificationLink(String token) {
        String link =
                webClientProperties.getBaseUrl()
                        + webClientProperties.getVerification().getEmail().getPath()
                        + "?token="
                        + token;
        log.debug("Built email verification link");
        return link;
    }

    public String buildPasswordResetLink(String token) {
        String link =
                webClientProperties.getBaseUrl()
                        + webClientProperties.getVerification().getPasswordReset().getPath()
                        + "?token="
                        + token;
        log.debug("Built password reset link");
        return link;
    }
}
