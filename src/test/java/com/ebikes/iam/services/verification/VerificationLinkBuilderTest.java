package com.ebikes.iam.services.verification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.iam.configurations.properties.ClientProperties;
import com.ebikes.iam.configurations.properties.ClientProperties.ChannelConfiguration;
import com.ebikes.iam.configurations.properties.ClientProperties.VerificationConfiguration;

@DisplayName("VerificationLinkBuilder")
@ExtendWith(MockitoExtension.class)
class VerificationLinkBuilderTest {

  private static final String BASE_URL = "https://app.ebikes.test";
  private static final String TOKEN = "test-token-abc123";

  @Mock private ClientProperties clientProperties;
  @Mock private VerificationConfiguration verificationConfiguration;

  @InjectMocks private VerificationLinkBuilder verificationLinkBuilder;

  @BeforeEach
  void setUp() {
    when(clientProperties.getBaseUrl()).thenReturn(BASE_URL);
    when(clientProperties.getVerification()).thenReturn(verificationConfiguration);
  }

  @Nested
  @DisplayName("buildAccountActivationLink")
  class BuildAccountActivationLink {

    @Mock private ChannelConfiguration accountActivationConfig;

    @BeforeEach
    void setUp() {
      when(verificationConfiguration.getAccountActivation()).thenReturn(accountActivationConfig);
      when(accountActivationConfig.getPath()).thenReturn("/activate");
    }

    @Test
    @DisplayName("should prefix the base URL")
    void shouldPrefixBaseUrl() {
      String link = verificationLinkBuilder.buildAccountActivationLink(TOKEN);
      assertThat(link).startsWith(BASE_URL);
    }

    @Test
    @DisplayName("should use the account activation path")
    void shouldUseAccountActivationPath() {
      String link = verificationLinkBuilder.buildAccountActivationLink(TOKEN);
      assertThat(link).contains("/activate");
    }

    @Test
    @DisplayName("should append the token as a query parameter")
    void shouldAppendTokenAsQueryParameter() {
      String link = verificationLinkBuilder.buildAccountActivationLink(TOKEN);
      assertThat(link).endsWith("?token=" + TOKEN);
    }
  }

  @Nested
  @DisplayName("buildEmailVerificationLink")
  class BuildEmailVerificationLink {

    @Mock private ChannelConfiguration emailConfig;

    @BeforeEach
    void setUp() {
      when(verificationConfiguration.getEmail()).thenReturn(emailConfig);
      when(emailConfig.getPath()).thenReturn("/verify");
    }

    @Test
    @DisplayName("should prefix the base URL")
    void shouldPrefixBaseUrl() {
      String link = verificationLinkBuilder.buildEmailVerificationLink(TOKEN);
      assertThat(link).startsWith(BASE_URL);
    }

    @Test
    @DisplayName("should use the email verification path")
    void shouldUseEmailVerificationPath() {
      String link = verificationLinkBuilder.buildEmailVerificationLink(TOKEN);
      assertThat(link).contains("/verify");
    }

    @Test
    @DisplayName("should append the token as a query parameter")
    void shouldAppendTokenAsQueryParameter() {
      String link = verificationLinkBuilder.buildEmailVerificationLink(TOKEN);
      assertThat(link).endsWith("?token=" + TOKEN);
    }
  }

  @Nested
  @DisplayName("buildPasswordResetLink")
  class BuildPasswordResetLink {

    @Mock private ChannelConfiguration passwordResetConfig;

    @BeforeEach
    void setUp() {
      when(verificationConfiguration.getPasswordReset()).thenReturn(passwordResetConfig);
      when(passwordResetConfig.getPath()).thenReturn("/reset-password");
    }

    @Test
    @DisplayName("should prefix the base URL")
    void shouldPrefixBaseUrl() {
      String link = verificationLinkBuilder.buildPasswordResetLink(TOKEN);
      assertThat(link).startsWith(BASE_URL);
    }

    @Test
    @DisplayName("should use the password reset path")
    void shouldUsePasswordResetPath() {
      String link = verificationLinkBuilder.buildPasswordResetLink(TOKEN);
      assertThat(link).contains("/reset-password");
    }

    @Test
    @DisplayName("should append the token as a query parameter")
    void shouldAppendTokenAsQueryParameter() {
      String link = verificationLinkBuilder.buildPasswordResetLink(TOKEN);
      assertThat(link).endsWith("?token=" + TOKEN);
    }
  }
}
