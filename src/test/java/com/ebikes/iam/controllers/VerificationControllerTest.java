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

@DisplayName("VerificationController")
@WebMvcTest(VerificationController.class)
class VerificationControllerTest extends AbstractControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private VerificationService verificationService;

  @Nested
  @DisplayName("POST /verification/account/activate")
  class CompleteAccountActivation {

    @Test
    @DisplayName("should return 200 when request is valid")
    void shouldReturn200WhenRequestIsValid() throws Exception {
      mockMvc
          .perform(
              post("/verification/account/activate")
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          VerificationRequestFixtures.completeAccountActivation())))
          .andExpect(status().isOk());

      verify(verificationService).completeAccountActivation(any(), any());
    }

    @Test
    @DisplayName("should return 400 when password is blank")
    void shouldReturn400WhenPasswordIsBlank() throws Exception {
      mockMvc
          .perform(
              post("/verification/account/activate")
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"password\": \"\", \"token\": \"some-token\"}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should return 400 when token is blank")
    void shouldReturn400WhenTokenIsBlank() throws Exception {
      mockMvc
          .perform(
              post("/verification/account/activate")
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"password\": \"validPassword1\", \"token\": \"\"}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should be accessible without authentication")
    void shouldBeAccessibleWithoutAuthentication() throws Exception {
      mockMvc
          .perform(
              post("/verification/account/activate")
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          VerificationRequestFixtures.completeAccountActivation())))
          .andExpect(status().isOk());
    }
  }

  @Nested
  @DisplayName("POST /verification/email/complete")
  class CompleteEmailVerification {

    @Test
    @DisplayName("should return 200 when request is valid")
    void shouldReturn200WhenRequestIsValid() throws Exception {
      mockMvc
          .perform(
              post("/verification/email/complete")
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          VerificationRequestFixtures.completeEmailVerification())))
          .andExpect(status().isOk());

      verify(verificationService).completeEmailVerification(any());
    }

    @Test
    @DisplayName("should return 400 when code is blank")
    void shouldReturn400WhenCodeIsBlank() throws Exception {
      mockMvc
          .perform(
              post("/verification/email/complete")
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"code\": \"\"}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should be accessible without authentication")
    void shouldBeAccessibleWithoutAuthentication() throws Exception {
      mockMvc
          .perform(
              post("/verification/email/complete")
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          VerificationRequestFixtures.completeEmailVerification())))
          .andExpect(status().isOk());
    }
  }

  @Nested
  @DisplayName("POST /verification/phone/complete")
  class CompletePhoneVerification {

    @Test
    @DisplayName("should return 200 when request is valid")
    void shouldReturn200WhenRequestIsValid() throws Exception {
      mockMvc
          .perform(
              post("/verification/phone/complete")
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          VerificationRequestFixtures.completePhoneVerification())))
          .andExpect(status().isOk());

      verify(verificationService).completePhoneNumberVerification(any());
    }

    @Test
    @DisplayName("should return 400 when code is blank")
    void shouldReturn400WhenCodeIsBlank() throws Exception {
      mockMvc
          .perform(
              post("/verification/phone/complete")
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"code\": \"\"}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should be accessible without authentication")
    void shouldBeAccessibleWithoutAuthentication() throws Exception {
      mockMvc
          .perform(
              post("/verification/phone/complete")
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          VerificationRequestFixtures.completePhoneVerification())))
          .andExpect(status().isOk());
    }
  }

  @Nested
  @DisplayName("POST /verification/email/request")
  class RequestEmailVerification {

    @Test
    @DisplayName("should return 200 when request is valid")
    void shouldReturn200WhenRequestIsValid() throws Exception {
      mockMvc
          .perform(
              post("/verification/email/request")
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          VerificationRequestFixtures.emailVerification())))
          .andExpect(status().isOk());

      verify(verificationService).requestEmailVerification(any());
    }

    @Test
    @DisplayName("should return 400 when email is blank")
    void shouldReturn400WhenEmailIsBlank() throws Exception {
      mockMvc
          .perform(
              post("/verification/email/request")
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"email\": \"\"}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should be accessible without authentication")
    void shouldBeAccessibleWithoutAuthentication() throws Exception {
      mockMvc
          .perform(
              post("/verification/email/request")
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          VerificationRequestFixtures.emailVerification())))
          .andExpect(status().isOk());
    }
  }

  @Nested
  @DisplayName("POST /verification/phone/request")
  class RequestPhoneVerification {

    @Test
    @DisplayName("should return 200 when request is valid")
    void shouldReturn200WhenRequestIsValid() throws Exception {
      mockMvc
          .perform(
              post("/verification/phone/request")
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          VerificationRequestFixtures.phoneVerification())))
          .andExpect(status().isOk());

      verify(verificationService).requestPhoneVerification(any());
    }

    @Test
    @DisplayName("should return 400 when phone number is blank")
    void shouldReturn400WhenPhoneNumberIsBlank() throws Exception {
      mockMvc
          .perform(
              post("/verification/phone/request")
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"phoneNumber\": \"\"}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should be accessible without authentication")
    void shouldBeAccessibleWithoutAuthentication() throws Exception {
      mockMvc
          .perform(
              post("/verification/phone/request")
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          VerificationRequestFixtures.phoneVerification())))
          .andExpect(status().isOk());
    }
  }
}
