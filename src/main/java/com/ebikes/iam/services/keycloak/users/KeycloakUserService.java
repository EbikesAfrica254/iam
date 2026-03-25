package com.ebikes.iam.services.keycloak.users;

import com.ebikes.iam.configurations.properties.KeycloakProperties;
import com.ebikes.iam.dtos.requests.users.UpdateUserExtensionRequest;
import com.ebikes.iam.enums.ResponseCode;
import com.ebikes.iam.exceptions.ExternalServiceException;
import com.ebikes.iam.mappers.KeycloakUserMapper;
import com.ebikes.iam.support.keycloak.KeycloakResponseUtilities;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RequiredArgsConstructor
@Service
@Slf4j
public class KeycloakUserService {

  private static final String KEYCLOAK_ENDPOINT = "keycloak://users";
  private static final String UPDATE_PASSWORD_ACTION = "UPDATE_PASSWORD";
  private static final String VERIFY_EMAIL_ACTION = "VERIFY_EMAIL";

  private final Keycloak keycloakAdminClient;
  private final KeycloakProperties keycloakProperties;
  private final KeycloakUserMapper keycloakUserMapper;

  public String createUser(String email, String firstName, String lastName, String username) {
    UserRepresentation userRepresentation =
        keycloakUserMapper.toUserRepresentation(email, firstName, lastName, username);

    try (Response response = getUsersResource().create(userRepresentation)) {
      if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
        throw new ExternalServiceException(
            KEYCLOAK_ENDPOINT,
            response.getStatusInfo().getReasonPhrase(),
            ResponseCode.fromHttpStatus(response.getStatus()));
      }

      String keycloakUserId = KeycloakResponseUtilities.extractIdFromLocation(response);
      log.info("Keycloak user created - email={} keycloakUserId={}", email, keycloakUserId);
      return keycloakUserId;
    }
  }

  public void deleteUser(String keycloakUserId) {
    getUsersResource().get(keycloakUserId).remove();
    log.info("Keycloak user deleted - keycloakUserId={}", keycloakUserId);
  }

  public void disableUser(String keycloakUserId) {
    toggleUserEnabledStatus(false, keycloakUserId);
  }

  public void enableUser(String keycloakUserId) {
    toggleUserEnabledStatus(true, keycloakUserId);
  }

  public boolean isEmailRegistered(String email) {
    return !getUsersResource().searchByEmail(email, true).isEmpty();
  }

  public void resetPassword(String keycloakUserId, String password, boolean temporary) {
    UserResource userResource = getUserResource(keycloakUserId);

    CredentialRepresentation credential = new CredentialRepresentation();
    credential.setType(CredentialRepresentation.PASSWORD);
    credential.setValue(password);
    credential.setTemporary(temporary);

    userResource.resetPassword(credential);

    if (temporary) {
      ensureUpdatePasswordAction(userResource);
    }

    log.info("Password reset - keycloakUserId={} temporary={}", keycloakUserId, temporary);
  }

  public void updateUser(String keycloakUserId, UpdateUserExtensionRequest request) {
    UserResource userResource = getUserResource(keycloakUserId);
    UserRepresentation user = userResource.toRepresentation();
    keycloakUserMapper.updateUserRepresentation(request, user);
    userResource.update(user);
    log.info("User updated - keycloakUserId={}", keycloakUserId);
  }

  public void updateUserAttributes(String keycloakUserId, Map<String, String> attributes) {
    UserResource userResource = getUserResource(keycloakUserId);
    UserRepresentation user = userResource.toRepresentation();

    if (user.getAttributes() == null) {
      user.setAttributes(new HashMap<>());
    }

    attributes.forEach((key, value) -> user.getAttributes().put(key, List.of(value)));

    try {
      userResource.update(user);
    } catch (Exception e) {
      log.error("Error updating user attributes: {}", e.getMessage());
      throw new ExternalServiceException(
          "keycloak.localhost",
          "Error updating user attributes",
          ResponseCode.EXTERNAL_SERVICE_ERROR,
          e);
    }
    log.info(
        "Updated user attributes - keycloakUserId={} attributeKeys={}",
        keycloakUserId,
        attributes.keySet());
  }

  public void verifyEmail(String keycloakUserId) {
    UserResource userResource = getUserResource(keycloakUserId);
    UserRepresentation user = userResource.toRepresentation();

    if (Boolean.TRUE.equals(user.isEmailVerified())) {
      log.debug("Email already verified - keycloakUserId={}", keycloakUserId);
      return;
    }

    user.setEmailVerified(true);
    removeRequiredAction(user);
    userResource.update(user);

    log.info("Email verified - keycloakUserId={}", keycloakUserId);
  }

  public void verifyPhone(String keycloakUserId) {
    UserResource userResource = getUserResource(keycloakUserId);
    UserRepresentation user = userResource.toRepresentation();

    String isPhoneNumberVerified = user.getAttributes().get("phoneNumberVerified").getFirst();
    if (Objects.equals(isPhoneNumberVerified, "true")) {
      log.debug("Phone number already verified - keycloakUserId={}", keycloakUserId);
      return;
    }

    user.getAttributes().put("phoneNumberVerified", List.of("true"));
    userResource.update(user);
  }

  private void ensureUpdatePasswordAction(UserResource userResource) {
    UserRepresentation user = userResource.toRepresentation();
    List<String> requiredActions =
        user.getRequiredActions() != null ? user.getRequiredActions() : new ArrayList<>();

    if (!requiredActions.contains(UPDATE_PASSWORD_ACTION)) {
      requiredActions.add(UPDATE_PASSWORD_ACTION);
      user.setRequiredActions(requiredActions);
      userResource.update(user);
    }
  }

  private UserResource getUserResource(String keycloakUserId) {
    return realmResource().users().get(keycloakUserId);
  }

  private UsersResource getUsersResource() {
    return realmResource().users();
  }

  private RealmResource realmResource() {
    return keycloakAdminClient.realm(keycloakProperties.getRealm());
  }

  private void removeRequiredAction(UserRepresentation user) {
    List<String> requiredActions = user.getRequiredActions();
    if (requiredActions != null
        && requiredActions.contains(KeycloakUserService.VERIFY_EMAIL_ACTION)) {
      requiredActions.remove(KeycloakUserService.VERIFY_EMAIL_ACTION);
      user.setRequiredActions(requiredActions);
    }
  }

  private void toggleUserEnabledStatus(boolean enabled, String keycloakUserId) {
    UserResource userResource = getUserResource(keycloakUserId);
    UserRepresentation user = userResource.toRepresentation();

    if (Boolean.TRUE.equals(user.isEnabled()) == enabled) {
      return;
    }

    user.setEnabled(enabled);
    userResource.update(user);
    log.info("User {} - keycloakUserId={}", enabled ? "enabled" : "disabled", keycloakUserId);
  }
}
