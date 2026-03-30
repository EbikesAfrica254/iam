package com.ebikes.iam.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ebikes.iam.services.users.membership.MembershipService;
import com.ebikes.iam.support.infrastructure.AbstractControllerTest;

@DisplayName("MembershipController")
@WebMvcTest(MembershipController.class)
class MembershipControllerTest extends AbstractControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private MembershipService membershipService;

  private static final String KEYCLOAK_USER_ID = UUID.randomUUID().toString();
  private static final String ORGANIZATION_ID = UUID.randomUUID().toString();

  @Nested
  @DisplayName("POST /memberships/{keycloakUserId}")
  class Create {

    @Test
    @DisplayName("should return 201 when request is valid")
    void shouldReturn201WhenRequestIsValid() throws Exception {
      when(membershipService.create(any(), any())).thenReturn(null);

      mockMvc
          .perform(
              post("/memberships/{keycloakUserId}", KEYCLOAK_USER_ID)
                  .with(authenticatedJwt())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      """
                      {
                        "organizationId": "%s",
                        "isPrimary": true,
                        "roles": ["CUSTOMER"]
                      }
                      """
                          .formatted(ORGANIZATION_ID)))
          .andExpect(status().isCreated());

      verify(membershipService).create(any(), any());
    }

    @Test
    @DisplayName("should return 400 when organizationId is blank")
    void shouldReturn400WhenOrganizationIdIsBlank() throws Exception {
      mockMvc
          .perform(
              post("/memberships/{keycloakUserId}", KEYCLOAK_USER_ID)
                  .with(authenticatedJwt())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      """
                      {
                        "organizationId": "",
                        "isPrimary": true,
                        "roles": ["CUSTOMER"]
                      }
                      """))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should return 400 when roles are empty")
    void shouldReturn400WhenRolesAreEmpty() throws Exception {
      mockMvc
          .perform(
              post("/memberships/{keycloakUserId}", KEYCLOAK_USER_ID)
                  .with(authenticatedJwt())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      """
                      {
                        "organizationId": "%s",
                        "isPrimary": true,
                        "roles": []
                      }
                      """
                          .formatted(ORGANIZATION_ID)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              post("/memberships/{keycloakUserId}", KEYCLOAK_USER_ID)
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      """
                      {
                        "organizationId": "%s",
                        "isPrimary": true,
                        "roles": ["CUSTOMER"]
                      }
                      """
                          .formatted(ORGANIZATION_ID)))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("GET /memberships/{keycloakUserId}")
  class ListMemberships {

    @Test
    @DisplayName("should return 200 with memberships")
    void shouldReturn200WithMemberships() throws Exception {
      when(membershipService.findMembershipsByKeycloakUserId(any())).thenReturn(List.of());

      mockMvc
          .perform(get("/memberships/{keycloakUserId}", KEYCLOAK_USER_ID).with(authenticatedJwt()))
          .andExpect(status().isOk());

      verify(membershipService).findMembershipsByKeycloakUserId(any());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(get("/memberships/{keycloakUserId}", KEYCLOAK_USER_ID).with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("DELETE /memberships/{keycloakUserId}")
  class Remove {

    @Test
    @DisplayName("should return 200 when membership exists")
    void shouldReturn200WhenMembershipExists() throws Exception {
      mockMvc
          .perform(
              delete("/memberships/{keycloakUserId}", KEYCLOAK_USER_ID)
                  .with(authenticatedJwt())
                  .param("organizationId", ORGANIZATION_ID))
          .andExpect(status().isOk());

      verify(membershipService).removeMembership(any(), any(), any());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              delete("/memberships/{keycloakUserId}", KEYCLOAK_USER_ID)
                  .with(anonymous())
                  .param("organizationId", ORGANIZATION_ID))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("DELETE /memberships/{keycloakUserId}/organizations/{organizationId}")
  class RemoveFromOrganization {

    @Test
    @DisplayName("should return 200 when user exists in organization")
    void shouldReturn200WhenUserExistsInOrganization() throws Exception {
      mockMvc
          .perform(
              delete(
                      "/memberships/{keycloakUserId}/organizations/{organizationId}",
                      KEYCLOAK_USER_ID,
                      ORGANIZATION_ID)
                  .with(authenticatedJwt()))
          .andExpect(status().isOk());

      verify(membershipService).removeUserFromOrganization(any(), any());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              delete(
                      "/memberships/{keycloakUserId}/organizations/{organizationId}",
                      KEYCLOAK_USER_ID,
                      ORGANIZATION_ID)
                  .with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("PUT /memberships/{keycloakUserId}/primary")
  class SetPrimary {

    @Test
    @DisplayName("should return 200 when request is valid")
    void shouldReturn200WhenRequestIsValid() throws Exception {
      mockMvc
          .perform(
              put("/memberships/{keycloakUserId}/primary", KEYCLOAK_USER_ID)
                  .with(authenticatedJwt())
                  .param("organizationId", ORGANIZATION_ID))
          .andExpect(status().isOk());

      verify(membershipService).setPrimaryMembership(any(), any());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              put("/memberships/{keycloakUserId}/primary", KEYCLOAK_USER_ID)
                  .with(anonymous())
                  .param("organizationId", ORGANIZATION_ID))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("PATCH /memberships/{keycloakUserId}/roles")
  class UpdateRoles {

    @Test
    @DisplayName("should return 200 when request is valid")
    void shouldReturn200WhenRequestIsValid() throws Exception {
      mockMvc
          .perform(
              patch("/memberships/{keycloakUserId}/roles", KEYCLOAK_USER_ID)
                  .with(authenticatedJwt())
                  .param("organizationId", ORGANIZATION_ID)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"roles\": [\"CUSTOMER\"]}"))
          .andExpect(status().isOk());

      verify(membershipService).setRoles(any(), any(), any(), any());
    }

    @Test
    @DisplayName("should return 400 when roles are empty")
    void shouldReturn400WhenRolesAreEmpty() throws Exception {
      mockMvc
          .perform(
              patch("/memberships/{keycloakUserId}/roles", KEYCLOAK_USER_ID)
                  .with(authenticatedJwt())
                  .param("organizationId", ORGANIZATION_ID)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"roles\": []}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              patch("/memberships/{keycloakUserId}/roles", KEYCLOAK_USER_ID)
                  .with(anonymous())
                  .param("organizationId", ORGANIZATION_ID)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"roles\": [\"CUSTOMER\"]}"))
          .andExpect(status().isUnauthorized());
    }
  }
}
