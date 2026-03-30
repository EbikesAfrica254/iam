package com.ebikes.iam.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ebikes.iam.dtos.responses.context.ContextResponse;
import com.ebikes.iam.services.users.context.ContextService;
import com.ebikes.iam.support.infrastructure.AbstractControllerTest;

@DisplayName("ContextController")
@WebMvcTest(ContextController.class)
class ContextControllerTest extends AbstractControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ContextService contextService;

  @Nested
  @DisplayName("GET /contexts/available")
  class GetAvailableContexts {

    @Test
    @DisplayName("should return 200 with available contexts")
    void shouldReturn200WithAvailableContexts() throws Exception {
      when(contextService.getSwitchableMemberships()).thenReturn(List.of());

      mockMvc
          .perform(get("/contexts/available").with(authenticatedJwt()))
          .andExpect(status().isOk());

      verify(contextService).getSwitchableMemberships();
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(get("/contexts/available").with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("GET /contexts/current")
  class GetCurrentContext {

    @Test
    @DisplayName("should return 200 with current context")
    void shouldReturn200WithCurrentContext() throws Exception {
      when(contextService.getCurrentContext())
          .thenReturn(
              new ContextResponse("branch-id", "organization-id", Set.of("BRANCH_OPERATOR")));

      mockMvc.perform(get("/contexts/current").with(authenticatedJwt())).andExpect(status().isOk());

      verify(contextService).getCurrentContext();
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(get("/contexts/current").with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("POST /contexts/switch")
  class SwitchContext {

    @Test
    @DisplayName("should return 200 when request is valid")
    void shouldReturn200WhenRequestIsValid() throws Exception {
      mockMvc
          .perform(
              post("/contexts/switch")
                  .with(authenticatedJwt())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      """
                      {
                        "organizationId": "%s"
                      }
                      """
                          .formatted(UUID.randomUUID())))
          .andExpect(status().isOk());

      verify(contextService).switchActiveMembership(any(), any());
    }

    @Test
    @DisplayName("should return 400 when organizationId is blank")
    void shouldReturn400WhenOrganizationIdIsBlank() throws Exception {
      mockMvc
          .perform(
              post("/contexts/switch")
                  .with(authenticatedJwt())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"organizationId\": \"\"}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              post("/contexts/switch")
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      """
                      {
                        "organizationId": "%s"
                      }
                      """
                          .formatted(UUID.randomUUID())))
          .andExpect(status().isUnauthorized());
    }
  }
}
