package com.ebikes.iam.controllers;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ebikes.iam.dtos.requests.filters.ContactFilter;
import com.ebikes.iam.dtos.responses.api.PaginatedResponse;
import com.ebikes.iam.dtos.responses.api.SuccessResponse;
import com.ebikes.iam.dtos.responses.contacts.ContactResponse;
import com.ebikes.iam.services.contacts.ContactService;
import com.ebikes.iam.services.users.UserExtensionService;
import com.ebikes.iam.support.context.ExecutionContext;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RequestMapping("/contacts")
@RestController
public class ContactController {

  private final ContactService contactService;
  private final UserExtensionService userExtensionService;

  @PostMapping("/{id}/claim")
  public ResponseEntity<SuccessResponse<Void>> claim(@PathVariable UUID id) {
    contactService.claim(
        id, userExtensionService.findUserExtensionByKeycloakUserId(ExecutionContext.getUserId()));
    return ResponseEntity.ok(SuccessResponse.of(null, "Contact claimed successfully"));
  }

  @GetMapping
  public ResponseEntity<PaginatedResponse<ContactResponse>> search(
      @ModelAttribute ContactFilter filter) {
    return ResponseEntity.ok(contactService.search(filter));
  }
}
