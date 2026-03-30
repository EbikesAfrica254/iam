package com.ebikes.iam.adapters.keycloak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.GroupsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.iam.configurations.properties.KeycloakProperties;
import com.ebikes.iam.exceptions.ExternalServiceException;
import com.ebikes.iam.exceptions.ResourceNotFoundException;
import com.ebikes.iam.support.keycloak.GroupPathUtilities;

@DisplayName("KeycloakGroupAdapter")
@ExtendWith(MockitoExtension.class)
class KeycloakGroupAdapterTest {

  private static final String REALM = "test-realm";
  private static final String USER_ID = UUID.randomUUID().toString();
  private static final String ORGANIZATION_ID = UUID.randomUUID().toString();
  private static final String GROUP_PATH = GroupPathUtilities.generate(ORGANIZATION_ID);
  private static final String GROUP_ID = UUID.randomUUID().toString();

  @Mock private Keycloak keycloakAdminClient;
  @Mock private KeycloakProperties keycloakProperties;
  @Mock private RealmResource realmResource;
  @Mock private GroupsResource groupsResource;
  @Mock private UsersResource usersResource;
  @Mock private UserResource userResource;

  @InjectMocks private KeycloakGroupAdapter adapter;

  @BeforeEach
  void setUp() {
    when(keycloakProperties.getRealm()).thenReturn(REALM);
    when(keycloakAdminClient.realm(REALM)).thenReturn(realmResource);
    when(realmResource.groups()).thenReturn(groupsResource);
  }

  @Nested
  @DisplayName("addUserToGroup")
  class AddUserToGroup {

    @Test
    @DisplayName("should join user to the group")
    void shouldJoinUserToGroup() {
      when(realmResource.users()).thenReturn(usersResource);
      when(usersResource.get(USER_ID)).thenReturn(userResource);
      GroupRepresentation group = groupRepresentation(GROUP_ID, GROUP_PATH);
      when(groupsResource.groups()).thenReturn(List.of(group));

      adapter.addUserToGroup(GROUP_PATH, USER_ID);

      verify(userResource).joinGroup(GROUP_ID);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when group does not exist")
    void shouldThrowWhenGroupNotFound() {
      when(groupsResource.groups()).thenReturn(List.of());

      assertThatThrownBy(() -> adapter.addUserToGroup(GROUP_PATH, USER_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should wrap ProcessingException as ExternalServiceException")
    void shouldWrapProcessingException() {
      when(groupsResource.groups()).thenThrow(new ProcessingException("error"));

      assertThatThrownBy(() -> adapter.addUserToGroup(GROUP_PATH, USER_ID))
          .isInstanceOf(ExternalServiceException.class);
    }
  }

  @Nested
  @DisplayName("create")
  class Create {

    @Test
    @DisplayName("should return existing group id when group already exists")
    void shouldReturnExistingGroupIdWhenAlreadyExists() {
      GroupRepresentation existing = groupRepresentation(GROUP_ID, GROUP_PATH);
      when(groupsResource.groups()).thenReturn(List.of(existing));

      String result = adapter.create(ORGANIZATION_ID, "Test Org");

      assertThat(result).isEqualTo(GROUP_ID);
      //noinspection resource
      verify(groupsResource, never()).add(any());
    }

    @Test
    @DisplayName("should create group and return new id when group does not exist")
    void shouldCreateGroupAndReturnNewId() throws Exception {
      String newGroupId = UUID.randomUUID().toString();
      URI location = new URI("https://keycloak/admin/realms/test/groups/" + newGroupId);
      when(groupsResource.groups()).thenReturn(List.of());
      try (Response r = Response.created(location).build()) {
        when(groupsResource.add(any())).thenReturn(r);

        String result = adapter.create(ORGANIZATION_ID, "Test Org");

        assertThat(result).isEqualTo(newGroupId);
      }
      //noinspection resource
      verify(groupsResource).add(any());
    }

    @Test
    @DisplayName("should wrap ProcessingException as ExternalServiceException")
    void shouldWrapProcessingException() {
      when(groupsResource.groups()).thenThrow(new ProcessingException("error"));

      assertThatThrownBy(() -> adapter.create(ORGANIZATION_ID, "Test Org"))
          .isInstanceOf(ExternalServiceException.class);
    }
  }

  @Nested
  @DisplayName("getGroupIdOrThrow")
  class GetGroupIdOrThrow {

    @Test
    @DisplayName("should return group id when group exists")
    void shouldReturnGroupIdWhenFound() {
      GroupRepresentation group = groupRepresentation(GROUP_ID, GROUP_PATH);
      when(groupsResource.groups()).thenReturn(List.of(group));

      String result = adapter.getGroupIdOrThrow(GROUP_PATH);

      assertThat(result).isEqualTo(GROUP_ID);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when group does not exist")
    void shouldThrowWhenGroupNotFound() {
      when(groupsResource.groups()).thenReturn(List.of());

      assertThatThrownBy(() -> adapter.getGroupIdOrThrow(GROUP_PATH))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("removeUserFromGroup")
  class RemoveUserFromGroup {

    @Test
    @DisplayName("should leave user from the group")
    void shouldLeaveUserFromGroup() {
      when(realmResource.users()).thenReturn(usersResource);
      when(usersResource.get(USER_ID)).thenReturn(userResource);
      GroupRepresentation group = groupRepresentation(GROUP_ID, GROUP_PATH);
      when(groupsResource.groups()).thenReturn(List.of(group));

      adapter.removeUserFromGroup(GROUP_PATH, USER_ID);

      verify(userResource).leaveGroup(GROUP_ID);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when group does not exist")
    void shouldThrowWhenGroupNotFound() {
      when(groupsResource.groups()).thenReturn(List.of());

      assertThatThrownBy(() -> adapter.removeUserFromGroup(GROUP_PATH, USER_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should wrap ProcessingException as ExternalServiceException")
    void shouldWrapProcessingException() {
      when(groupsResource.groups()).thenThrow(new ProcessingException("error"));

      assertThatThrownBy(() -> adapter.removeUserFromGroup(GROUP_PATH, USER_ID))
          .isInstanceOf(ExternalServiceException.class);
    }
  }

  @Nested
  @DisplayName("searchInGroupHierarchy (via getGroupIdOrThrow)")
  class SearchInGroupHierarchy {

    @Test
    @DisplayName("should find group at the top level")
    void shouldFindGroupAtTopLevel() {
      GroupRepresentation group = groupRepresentation(GROUP_ID, GROUP_PATH);
      when(groupsResource.groups()).thenReturn(List.of(group));

      String result = adapter.getGroupIdOrThrow(GROUP_PATH);

      assertThat(result).isEqualTo(GROUP_ID);
    }

    @Test
    @DisplayName("should find group nested within a subgroup")
    void shouldFindGroupInSubgroup() {
      String subGroupId = UUID.randomUUID().toString();
      String subGroupPath = GROUP_PATH + "/sub";

      GroupRepresentation subGroup = groupRepresentation(subGroupId, subGroupPath);
      GroupRepresentation parent = groupRepresentation(GROUP_ID, GROUP_PATH);
      parent.setSubGroups(List.of(subGroup));
      when(groupsResource.groups()).thenReturn(List.of(parent));

      String result = adapter.getGroupIdOrThrow(subGroupPath);

      assertThat(result).isEqualTo(subGroupId);
    }

    @Test
    @DisplayName("should return empty when group is not found in hierarchy")
    void shouldReturnEmptyWhenNotFound() {
      when(groupsResource.groups()).thenReturn(List.of());

      assertThatThrownBy(() -> adapter.getGroupIdOrThrow("/nonexistent"))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  private GroupRepresentation groupRepresentation(String id, String path) {
    GroupRepresentation group = new GroupRepresentation();
    group.setId(id);
    group.setPath(path);
    return group;
  }
}
