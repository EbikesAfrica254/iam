package com.ebikes.iam.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ebikes.iam.dtos.responses.api.PaginatedResponse;
import com.ebikes.iam.services.contacts.ContactService;
import com.ebikes.iam.services.users.UserExtensionService;
import com.ebikes.iam.support.fixtures.UserExtensionFixtures;
import com.ebikes.iam.support.infrastructure.AbstractControllerTest;

@DisplayName("ContactController")
@WebMvcTest(ContactController.class)
class ContactControllerTest extends AbstractControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ContactService contactService;
  @MockitoBean private UserExtensionService userExtensionService;

  @Nested
  @DisplayName("POST /contacts/{id}/claim")
  class Claim {

    @Test
    @DisplayName("should return 200 when contact is claimed successfully")
    void shouldReturn200WhenContactIsClaimedSuccessfully() throws Exception {
      when(userExtensionService.findUserExtensionByKeycloakUserId(any()))
          .thenReturn(UserExtensionFixtures.active());

      mockMvc
          .perform(post("/contacts/{id}/claim", UUID.randomUUID()).with(authenticatedJwt()))
          .andExpect(status().isOk());

      verify(userExtensionService).findUserExtensionByKeycloakUserId(any());
      verify(contactService).claim(any(), any());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(post("/contacts/{id}/claim", UUID.randomUUID()).with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("GET /contacts")
  class Search {

    @Test
    @DisplayName("should return 200 with paginated results")
    void shouldReturn200WithPaginatedResults() throws Exception {
      when(contactService.search(any()))
          .thenReturn(PaginatedResponse.from("Contacts retrieved successfully.", Page.empty()));

      mockMvc.perform(get("/contacts").with(authenticatedJwt())).andExpect(status().isOk());

      verify(contactService).search(any());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc.perform(get("/contacts").with(anonymous())).andExpect(status().isUnauthorized());
    }
  }
}
