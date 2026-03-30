package com.ebikes.iam.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ebikes.iam.services.verification.VerificationService;
import com.ebikes.iam.support.fixtures.VerificationRequestFixtures;
import com.ebikes.iam.support.infrastructure.AbstractControllerTest;

import tools.jackson.databind.ObjectMapper;

@DisplayName("PasswordResetController")
@WebMvcTest(PasswordResetController.class)
class PasswordResetControllerTest extends AbstractControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private VerificationService verificationService;

  @Nested
  @DisplayName("POST /password-reset")
  class RequestPasswordReset {

    @Test
    @DisplayName("should return 200 when request is valid")
    void shouldReturn200WhenRequestIsValid() throws Exception {
      mockMvc
          .perform(
              post("/password-reset")
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(VerificationRequestFixtures.passwordReset())))
          .andExpect(status().isOk());

      verify(verificationService).requestPasswordReset(any());
    }

    @Test
    @DisplayName("should return 400 when email is blank")
    void shouldReturn400WhenEmailIsBlank() throws Exception {
      mockMvc
          .perform(
              post("/password-reset")
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"email\": \"\"}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should return 400 when email format is invalid")
    void shouldReturn400WhenEmailFormatIsInvalid() throws Exception {
      mockMvc
          .perform(
              post("/password-reset")
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"email\": \"not-an-email\"}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should be accessible without authentication")
    void shouldBeAccessibleWithoutAuthentication() throws Exception {
      mockMvc
          .perform(
              post("/password-reset")
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(VerificationRequestFixtures.passwordReset())))
          .andExpect(status().isOk());
    }
  }

  @Nested
  @DisplayName("POST /password-reset/complete")
  class CompletePasswordReset {

    @Test
    @DisplayName("should return 200 when request is valid")
    void shouldReturn200WhenRequestIsValid() throws Exception {
      mockMvc
          .perform(
              post("/password-reset/complete")
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          VerificationRequestFixtures.completePasswordReset())))
          .andExpect(status().isOk());

      verify(verificationService).completePasswordReset(any(), any());
    }

    @Test
    @DisplayName("should return 400 when token is blank")
    void shouldReturn400WhenTokenIsBlank() throws Exception {
      mockMvc
          .perform(
              post("/password-reset/complete")
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"token\": \"\", \"newPassword\": \"validPass1\"}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should return 400 when password is blank")
    void shouldReturn400WhenPasswordIsBlank() throws Exception {
      mockMvc
          .perform(
              post("/password-reset/complete")
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"token\": \"some-token\", \"newPassword\": \"\"}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should return 400 when password is too short")
    void shouldReturn400WhenPasswordIsTooShort() throws Exception {
      mockMvc
          .perform(
              post("/password-reset/complete")
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"token\": \"some-token\", \"newPassword\": \"short\"}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should be accessible without authentication")
    void shouldBeAccessibleWithoutAuthentication() throws Exception {
      mockMvc
          .perform(
              post("/password-reset/complete")
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          VerificationRequestFixtures.completePasswordReset())))
          .andExpect(status().isOk());
    }
  }
}
