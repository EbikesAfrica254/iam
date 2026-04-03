package com.ebikes.iam.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ebikes.iam.services.users.UserExtensionService;
import com.ebikes.iam.services.users.provisioning.ProvisioningService;
import com.ebikes.iam.support.fixtures.SecurityFixtures;
import com.ebikes.iam.support.fixtures.UserExtensionResponseFixtures;
import com.ebikes.iam.support.fixtures.UserRequestFixtures;
import com.ebikes.iam.support.infrastructure.AbstractControllerTest;

import tools.jackson.databind.ObjectMapper;

@DisplayName("UserController")
@WebMvcTest(UserController.class)
class UserControllerTest extends AbstractControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private UserExtensionService userExtensionService;
  @MockitoBean private ProvisioningService provisioningService;

  @Nested
  @DisplayName("POST /users")
  class CreateUser {

    @Test
    @DisplayName("should return 201 when request is valid")
    void shouldReturn201WhenRequestIsValid() throws Exception {
      mockMvc
          .perform(
              post("/users")
                  .with(SecurityFixtures.authenticatedJwt())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(UserRequestFixtures.createUser())))
          .andExpect(status().isCreated());

      verify(provisioningService).provisionUser(any());
    }

    @Test
    @DisplayName("should return 400 when email is blank")
    void shouldReturn400WhenEmailIsBlank() throws Exception {
      mockMvc
          .perform(
              post("/users")
                  .with(SecurityFixtures.authenticatedJwt())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          UserRequestFixtures.createUserWithBlankEmail())))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should return 400 when roles are empty")
    void shouldReturn400WhenRolesAreEmpty() throws Exception {
      mockMvc
          .perform(
              post("/users")
                  .with(SecurityFixtures.authenticatedJwt())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          UserRequestFixtures.createUserWithEmptyRoles())))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              post("/users")
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(UserRequestFixtures.createUser())))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("POST /users/signup")
  class Signup {

    @Test
    @DisplayName("should return 201 when request is valid")
    void shouldReturn201WhenRequestIsValid() throws Exception {
      mockMvc
          .perform(
              post("/users/signup")
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(UserRequestFixtures.signup())))
          .andExpect(status().isCreated());

      verify(provisioningService).provisionSignup(any());
    }

    @Test
    @DisplayName("should return 400 when email is blank")
    void shouldReturn400WhenEmailIsBlank() throws Exception {
      mockMvc
          .perform(
              post("/users/signup")
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(UserRequestFixtures.signupWithBlankEmail())))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should return 400 when phone number is blank")
    void shouldReturn400WhenPhoneNumberIsBlank() throws Exception {
      mockMvc
          .perform(
              post("/users/signup")
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          UserRequestFixtures.signupWithBlankPhoneNumber())))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should be accessible without authentication")
    void shouldBeAccessibleWithoutAuthentication() throws Exception {
      mockMvc
          .perform(
              post("/users/signup")
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(UserRequestFixtures.signup())))
          .andExpect(status().isCreated());
    }
  }

  @Nested
  @DisplayName("GET /users/{id}")
  class FindById {

    @Test
    @DisplayName("should return 200 with user detail")
    void shouldReturn200WithUserDetail() throws Exception {
      when(userExtensionService.findById(any())).thenReturn(UserExtensionResponseFixtures.detail());

      mockMvc
          .perform(get("/users/{id}", UUID.randomUUID()).with(SecurityFixtures.authenticatedJwt()))
          .andExpect(status().isOk());

      verify(userExtensionService).findById(any());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(get("/users/{id}", UUID.randomUUID()).with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("GET /users/me")
  class Me {

    @Test
    @DisplayName("should return 200 with user profile")
    void shouldReturn200WithUserProfile() throws Exception {
      when(userExtensionService.me()).thenReturn(UserExtensionResponseFixtures.profile());

      mockMvc
          .perform(get("/users/me").with(SecurityFixtures.authenticatedJwt()))
          .andExpect(status().isOk());

      verify(userExtensionService).me();
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc.perform(get("/users/me").with(anonymous())).andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("GET /users")
  class Search {

    @Test
    @DisplayName("should return 200 with paginated results")
    void shouldReturn200WithPaginatedResults() throws Exception {
      when(userExtensionService.search(any())).thenReturn(Page.empty());

      mockMvc
          .perform(get("/users").with(SecurityFixtures.authenticatedJwt()))
          .andExpect(status().isOk());

      verify(userExtensionService).search(any());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc.perform(get("/users").with(anonymous())).andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("PUT /users/{id}")
  class Update {

    @Test
    @DisplayName("should return 200 when request is valid")
    void shouldReturn200WhenRequestIsValid() throws Exception {
      when(userExtensionService.update(any(), any()))
          .thenReturn(UserExtensionResponseFixtures.detail());

      mockMvc
          .perform(
              put("/users/{id}", UUID.randomUUID())
                  .with(SecurityFixtures.authenticatedJwt())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(UserRequestFixtures.updateUser())))
          .andExpect(status().isOk());

      verify(userExtensionService).update(any(), any());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              put("/users/{id}", UUID.randomUUID())
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(UserRequestFixtures.updateUser())))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("PUT /users/{id}/status")
  class UpdateStatus {

    @Test
    @DisplayName("should return 200 when status is valid")
    void shouldReturn200WhenStatusIsValid() throws Exception {
      mockMvc
          .perform(
              put("/users/{id}/status", UUID.randomUUID())
                  .with(SecurityFixtures.authenticatedJwt())
                  .param("status", "ACTIVE"))
          .andExpect(status().isOk());

      verify(userExtensionService).updateStatus(any(), any());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              put("/users/{id}/status", UUID.randomUUID())
                  .with(anonymous())
                  .param("status", "ACTIVE"))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("DELETE /users/{id}")
  class Delete {

    @Test
    @DisplayName("should return 200 when user exists")
    void shouldReturn200WhenUserExists() throws Exception {
      mockMvc
          .perform(
              delete("/users/{id}", UUID.randomUUID()).with(SecurityFixtures.authenticatedJwt()))
          .andExpect(status().isOk());

      verify(userExtensionService).delete(any());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(delete("/users/{id}", UUID.randomUUID()).with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("DELETE /users/{id}/deprovision")
  class Deprovision {

    @Test
    @DisplayName("should return 200 when user exists")
    void shouldReturn200WhenUserExists() throws Exception {
      mockMvc
          .perform(
              delete("/users/{id}/deprovision", UUID.randomUUID())
                  .with(SecurityFixtures.authenticatedJwt()))
          .andExpect(status().isOk());

      verify(userExtensionService).deprovision(any());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(delete("/users/{id}/deprovision", UUID.randomUUID()).with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("POST /users/{id}/restore")
  class Restore {

    @Test
    @DisplayName("should return 200 when user exists")
    void shouldReturn200WhenUserExists() throws Exception {
      mockMvc
          .perform(
              post("/users/{id}/restore", UUID.randomUUID())
                  .with(SecurityFixtures.authenticatedJwt()))
          .andExpect(status().isOk());

      verify(userExtensionService).restore(any());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(post("/users/{id}/restore", UUID.randomUUID()).with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }
}
