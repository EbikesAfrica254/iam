package com.ebikes.iam.adapters.keycloak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.iam.configurations.properties.KeycloakProperties;
import com.ebikes.iam.dtos.requests.users.UpdateUserExtensionRequest;
import com.ebikes.iam.exceptions.ExternalServiceException;
import com.ebikes.iam.mappers.KeycloakUserMapper;

@DisplayName("KeycloakUserAdapter")
@ExtendWith(MockitoExtension.class)
class KeycloakUserAdapterTest {

  private static final String REALM = "test-realm";
  private static final String KEYCLOAK_USER_ID = UUID.randomUUID().toString();

  @Mock private Keycloak keycloakAdminClient;
  @Mock private KeycloakProperties keycloakProperties;
  @Mock private KeycloakUserMapper keycloakUserMapper;
  @Mock private RealmResource realmResource;
  @Mock private UsersResource usersResource;
  @Mock private UserResource userResource;
  @Mock private Response response;

  @Captor private ArgumentCaptor<UserRepresentation> userRepCaptor;

  @InjectMocks private KeycloakUserAdapter adapter;

  @BeforeEach
  void setUp() {
    when(keycloakProperties.getRealm()).thenReturn(REALM);
    when(keycloakAdminClient.realm(REALM)).thenReturn(realmResource);
    when(realmResource.users()).thenReturn(usersResource);
  }

  @Nested
  @DisplayName("createUser")
  class CreateUser {

    @Test
    @DisplayName("should return the extracted keycloak user id from location header")
    void shouldReturnExtractedKeycloakUserId() throws Exception {
      String expectedId = UUID.randomUUID().toString();
      URI location = new URI("https://keycloak/admin/realms/test/users/" + expectedId);
      when(keycloakUserMapper.toUserRepresentation(any(), any(), any(), any()))
          .thenReturn(new UserRepresentation());
      when(usersResource.create(any())).thenReturn(response);
      when(response.getLocation()).thenReturn(location);

      String result = adapter.createUser("test@test.com", "First", "Last", "username");

      assertThat(result).isEqualTo(expectedId);
    }

    @Test
    @DisplayName("should wrap ProcessingException as ExternalServiceException")
    void shouldWrapProcessingException() {
      when(keycloakUserMapper.toUserRepresentation(any(), any(), any(), any()))
          .thenReturn(new UserRepresentation());
      when(usersResource.create(any())).thenThrow(new ProcessingException("connection failed"));

      assertThatThrownBy(() -> adapter.createUser("test@test.com", "First", "Last", "username"))
          .isInstanceOf(ExternalServiceException.class);
    }
  }

  @Nested
  @DisplayName("deleteUser")
  class DeleteUser {

    @BeforeEach
    void setUp() {
      when(usersResource.get(KEYCLOAK_USER_ID)).thenReturn(userResource);
    }

    @Test
    @DisplayName("should call remove on the user resource")
    void shouldCallRemoveOnUserResource() {
      adapter.deleteUser(KEYCLOAK_USER_ID);

      verify(userResource).remove();
    }

    @Test
    @DisplayName("should wrap ProcessingException as ExternalServiceException")
    void shouldWrapProcessingException() {
      when(usersResource.get(KEYCLOAK_USER_ID)).thenThrow(new ProcessingException("error"));

      assertThatThrownBy(() -> adapter.deleteUser(KEYCLOAK_USER_ID))
          .isInstanceOf(ExternalServiceException.class);
    }
  }

  @Nested
  @DisplayName("enableUser / disableUser")
  class ToggleUserStatus {

    @BeforeEach
    void setUp() {
      when(usersResource.get(KEYCLOAK_USER_ID)).thenReturn(userResource);
    }

    @Test
    @DisplayName("should enable a disabled user")
    void shouldEnableDisabledUser() {
      UserRepresentation user = new UserRepresentation();
      user.setEnabled(false);
      when(userResource.toRepresentation()).thenReturn(user);

      adapter.enableUser(KEYCLOAK_USER_ID);

      verify(userResource).update(userRepCaptor.capture());
      assertThat(userRepCaptor.getValue().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("should skip update when user is already enabled")
    void shouldSkipWhenAlreadyEnabled() {
      UserRepresentation user = new UserRepresentation();
      user.setEnabled(true);
      when(userResource.toRepresentation()).thenReturn(user);

      adapter.enableUser(KEYCLOAK_USER_ID);

      verify(userResource, never()).update(any());
    }

    @Test
    @DisplayName("should disable an enabled user")
    void shouldDisableEnabledUser() {
      UserRepresentation user = new UserRepresentation();
      user.setEnabled(true);
      when(userResource.toRepresentation()).thenReturn(user);

      adapter.disableUser(KEYCLOAK_USER_ID);

      verify(userResource).update(userRepCaptor.capture());
      assertThat(userRepCaptor.getValue().isEnabled()).isFalse();
    }

    @Test
    @DisplayName("should skip update when user is already disabled")
    void shouldSkipWhenAlreadyDisabled() {
      UserRepresentation user = new UserRepresentation();
      user.setEnabled(false);
      when(userResource.toRepresentation()).thenReturn(user);

      adapter.disableUser(KEYCLOAK_USER_ID);

      verify(userResource, never()).update(any());
    }

    @Test
    @DisplayName("should wrap ProcessingException as ExternalServiceException")
    void shouldWrapProcessingException() {
      when(usersResource.get(KEYCLOAK_USER_ID)).thenThrow(new ProcessingException("error"));

      assertThatThrownBy(() -> adapter.enableUser(KEYCLOAK_USER_ID))
          .isInstanceOf(ExternalServiceException.class);
    }
  }

  @Nested
  @DisplayName("isEmailRegistered")
  class IsEmailRegistered {

    @Test
    @DisplayName("should return true when email exists in Keycloak")
    void shouldReturnTrueWhenEmailExists() {
      when(usersResource.searchByEmail("test@test.com", true))
          .thenReturn(List.of(new UserRepresentation()));

      assertThat(adapter.isEmailRegistered("test@test.com")).isTrue();
    }

    @Test
    @DisplayName("should return false when email does not exist in Keycloak")
    void shouldReturnFalseWhenEmailDoesNotExist() {
      when(usersResource.searchByEmail("test@test.com", true)).thenReturn(List.of());

      assertThat(adapter.isEmailRegistered("test@test.com")).isFalse();
    }

    @Test
    @DisplayName("should wrap ProcessingException as ExternalServiceException")
    void shouldWrapProcessingException() {
      when(usersResource.searchByEmail(anyString(), any()))
          .thenThrow(new ProcessingException("error"));

      assertThatThrownBy(() -> adapter.isEmailRegistered("test@test.com"))
          .isInstanceOf(ExternalServiceException.class);
    }
  }

  @Nested
  @DisplayName("resetPassword")
  class ResetPassword {

    @BeforeEach
    void setUp() {
      when(usersResource.get(KEYCLOAK_USER_ID)).thenReturn(userResource);
    }

    @Test
    @DisplayName("should reset password with non-temporary credential")
    void shouldResetPasswordNonTemporary() {
      adapter.resetPassword(KEYCLOAK_USER_ID, "newPassword", false);

      verify(userResource).resetPassword(any());
      verify(userResource, never()).toRepresentation();
    }

    @Test
    @DisplayName("should add UPDATE_PASSWORD required action when password is temporary")
    void shouldAddUpdatePasswordActionWhenTemporary() {
      UserRepresentation user = new UserRepresentation();
      user.setRequiredActions(new ArrayList<>());
      when(userResource.toRepresentation()).thenReturn(user);

      adapter.resetPassword(KEYCLOAK_USER_ID, "tempPassword", true);

      verify(userResource).update(userRepCaptor.capture());
      assertThat(userRepCaptor.getValue().getRequiredActions()).contains("UPDATE_PASSWORD");
    }

    @Test
    @DisplayName("should not add UPDATE_PASSWORD action if already present")
    void shouldNotDuplicateUpdatePasswordAction() {
      UserRepresentation user = new UserRepresentation();
      user.setRequiredActions(new ArrayList<>(List.of("UPDATE_PASSWORD")));
      when(userResource.toRepresentation()).thenReturn(user);

      adapter.resetPassword(KEYCLOAK_USER_ID, "tempPassword", true);

      verify(userResource, never()).update(any());
    }

    @Test
    @DisplayName("should wrap ProcessingException as ExternalServiceException")
    void shouldWrapProcessingException() {
      when(usersResource.get(KEYCLOAK_USER_ID)).thenThrow(new ProcessingException("error"));

      assertThatThrownBy(() -> adapter.resetPassword(KEYCLOAK_USER_ID, "password", false))
          .isInstanceOf(ExternalServiceException.class);
    }
  }

  @Nested
  @DisplayName("updateUser")
  class UpdateUser {

    @Test
    @DisplayName("should map request onto user representation and update")
    void shouldMapAndUpdateUserRepresentation() {
      when(usersResource.get(KEYCLOAK_USER_ID)).thenReturn(userResource);
      when(userResource.toRepresentation()).thenReturn(new UserRepresentation());
      UpdateUserExtensionRequest request =
          new UpdateUserExtensionRequest(
              "new@email.com", true, "NewFirst", "NewLast", null, null, false, null);

      adapter.updateUser(KEYCLOAK_USER_ID, request);

      verify(keycloakUserMapper).updateUserRepresentation(any(), any());
      verify(userResource).update(any());
    }

    @Test
    @DisplayName("should wrap ProcessingException as ExternalServiceException")
    void shouldWrapProcessingException() {
      when(usersResource.get(KEYCLOAK_USER_ID)).thenThrow(new ProcessingException("error"));

      var request =
          new UpdateUserExtensionRequest(null, false, null, null, null, null, false, null);
      assertThatThrownBy(() -> adapter.updateUser(KEYCLOAK_USER_ID, request))
          .isInstanceOf(ExternalServiceException.class);
    }
  }

  @Nested
  @DisplayName("updateUserAttributes")
  class UpdateUserAttributes {

    @BeforeEach
    void setUp() {
      when(usersResource.get(KEYCLOAK_USER_ID)).thenReturn(userResource);
    }

    @Test
    @DisplayName("should initialise attributes map when user has none and apply new attributes")
    void shouldInitialiseAttributesMapAndApply() {
      UserRepresentation user = new UserRepresentation();
      when(userResource.toRepresentation()).thenReturn(user);

      adapter.updateUserAttributes(KEYCLOAK_USER_ID, Map.of("active_organization", "org-1"));

      verify(userResource).update(userRepCaptor.capture());
      assertThat(userRepCaptor.getValue().getAttributes()).containsKey("active_organization");
    }

    @Test
    @DisplayName("should merge new attributes with existing ones")
    void shouldMergeWithExistingAttributes() {
      UserRepresentation user = new UserRepresentation();
      Map<String, List<String>> existing = new HashMap<>();
      existing.put("existing_key", List.of("existing_value"));
      user.setAttributes(existing);
      when(userResource.toRepresentation()).thenReturn(user);

      adapter.updateUserAttributes(KEYCLOAK_USER_ID, Map.of("new_key", "new_value"));

      verify(userResource).update(userRepCaptor.capture());
      assertThat(userRepCaptor.getValue().getAttributes())
          .containsKey("existing_key")
          .containsKey("new_key");
    }

    @Test
    @DisplayName("should wrap ProcessingException as ExternalServiceException")
    void shouldWrapProcessingException() {
      when(usersResource.get(KEYCLOAK_USER_ID)).thenThrow(new ProcessingException("error"));

      var attributes = Map.of("key", "value");
      assertThatThrownBy(() -> adapter.updateUserAttributes(KEYCLOAK_USER_ID, attributes))
          .isInstanceOf(ExternalServiceException.class);
    }
  }

  @Nested
  @DisplayName("verifyEmail")
  class VerifyEmail {

    @BeforeEach
    void setUp() {
      when(usersResource.get(KEYCLOAK_USER_ID)).thenReturn(userResource);
    }

    @Test
    @DisplayName("should set emailVerified and remove VERIFY_EMAIL required action")
    void shouldSetEmailVerifiedAndRemoveAction() {
      UserRepresentation user = new UserRepresentation();
      user.setEmailVerified(false);
      user.setRequiredActions(new ArrayList<>(List.of("VERIFY_EMAIL")));
      when(userResource.toRepresentation()).thenReturn(user);

      adapter.verifyEmail(KEYCLOAK_USER_ID);

      verify(userResource).update(userRepCaptor.capture());
      assertThat(userRepCaptor.getValue().isEmailVerified()).isTrue();
      assertThat(userRepCaptor.getValue().getRequiredActions()).doesNotContain("VERIFY_EMAIL");
    }

    @Test
    @DisplayName("should skip update when email is already verified")
    void shouldSkipWhenEmailAlreadyVerified() {
      UserRepresentation user = new UserRepresentation();
      user.setEmailVerified(true);
      when(userResource.toRepresentation()).thenReturn(user);

      adapter.verifyEmail(KEYCLOAK_USER_ID);

      verify(userResource, never()).update(any());
    }

    @Test
    @DisplayName("should wrap ProcessingException as ExternalServiceException")
    void shouldWrapProcessingException() {
      when(usersResource.get(KEYCLOAK_USER_ID)).thenThrow(new ProcessingException("error"));

      assertThatThrownBy(() -> adapter.verifyEmail(KEYCLOAK_USER_ID))
          .isInstanceOf(ExternalServiceException.class);
    }
  }

  // ─── verifyPhone ──────────────────────────────────────────────────────────

  @Nested
  @DisplayName("verifyPhone")
  class VerifyPhone {

    @BeforeEach
    void setUp() {
      when(usersResource.get(KEYCLOAK_USER_ID)).thenReturn(userResource);
    }

    @Test
    @DisplayName("should set phoneNumberVerified attribute to true")
    void shouldSetPhoneNumberVerifiedAttribute() {
      UserRepresentation user = new UserRepresentation();
      Map<String, List<String>> attributes = new HashMap<>();
      attributes.put("phoneNumberVerified", List.of("false"));
      user.setAttributes(attributes);
      when(userResource.toRepresentation()).thenReturn(user);

      adapter.verifyPhone(KEYCLOAK_USER_ID);

      verify(userResource).update(userRepCaptor.capture());
      assertThat(userRepCaptor.getValue().getAttributes().get("phoneNumberVerified"))
          .containsExactly("true");
    }

    @Test
    @DisplayName("should skip update when phone is already verified")
    void shouldSkipWhenPhoneAlreadyVerified() {
      UserRepresentation user = new UserRepresentation();
      Map<String, List<String>> attributes = new HashMap<>();
      attributes.put("phoneNumberVerified", List.of("true"));
      user.setAttributes(attributes);
      when(userResource.toRepresentation()).thenReturn(user);

      adapter.verifyPhone(KEYCLOAK_USER_ID);

      verify(userResource, never()).update(any());
    }

    @Test
    @DisplayName("should wrap ProcessingException as ExternalServiceException")
    void shouldWrapProcessingException() {
      when(usersResource.get(KEYCLOAK_USER_ID)).thenThrow(new ProcessingException("error"));

      assertThatThrownBy(() -> adapter.verifyPhone(KEYCLOAK_USER_ID))
          .isInstanceOf(ExternalServiceException.class);
    }
  }
}
