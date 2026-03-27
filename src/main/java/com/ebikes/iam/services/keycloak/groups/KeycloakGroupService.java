package com.ebikes.iam.services.keycloak.groups;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.GroupsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.springframework.stereotype.Service;

import com.ebikes.iam.configurations.properties.KeycloakProperties;
import com.ebikes.iam.enums.ResponseCode;
import com.ebikes.iam.exceptions.ExternalServiceException;
import com.ebikes.iam.exceptions.ResourceNotFoundException;
import com.ebikes.iam.support.keycloak.GroupPathUtilities;
import com.ebikes.iam.support.keycloak.KeycloakResponseUtilities;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class KeycloakGroupService {

  private static final String KEYCLOAK_ENDPOINT = "keycloak://groups";

  private final Keycloak keycloakAdminClient;
  private final KeycloakProperties keycloakProperties;

  public void addUserToGroup(String groupPath, String userId) {
    String groupId = getGroupIdOrThrow(groupPath);
    getUserResource(userId).joinGroup(groupId);
    log.info("User added to group - groupId={} groupPath={} userId={}", groupId, groupPath, userId);
  }

  public String create(String organizationId, String organizationName) {
    String groupPath = GroupPathUtilities.generate(organizationId);
    Optional<GroupRepresentation> existing = findGroupByPath(groupPath);
    if (existing.isPresent()) {
      return existing.get().getId();
    }

    String groupName = groupPath.substring(1);
    GroupRepresentation group = new GroupRepresentation();
    group.setName(groupName);

    Map<String, List<String>> attributes = new HashMap<>();
    attributes.put("display_name", List.of(organizationName));
    attributes.put("organization_type", List.of("customer"));
    group.setAttributes(attributes);

    try (Response response = getGroupsResource().add(group)) {
      if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
        throw new ExternalServiceException(
            KEYCLOAK_ENDPOINT,
            response.getStatusInfo().getReasonPhrase(),
            ResponseCode.fromHttpStatus(response.getStatus()));
      }

      String groupId = KeycloakResponseUtilities.extractIdFromLocation(response);
      log.info("Created group - groupId={} groupPath={}", groupId, groupPath);
      return groupId;
    }
  }

  public String getGroupIdOrThrow(String groupPath) {
    return findGroupByPath(groupPath)
        .map(GroupRepresentation::getId)
        .orElseThrow(() -> notFoundGroup(groupPath));
  }

  public void removeUserFromGroup(String groupPath, String userId) {
    String groupId = getGroupIdOrThrow(groupPath);
    getUserResource(userId).leaveGroup(groupId);
    log.info(
        "User removed from group - groupId={} groupPath={} userId={}", groupId, groupPath, userId);
  }

  private Optional<GroupRepresentation> findGroupByPath(String groupPath) {
    return searchInGroupHierarchy(getGroupsResource().groups(), groupPath);
  }

  private GroupsResource getGroupsResource() {
    return realmResource().groups();
  }

  private UserResource getUserResource(String userId) {
    return realmResource().users().get(userId);
  }

  private RealmResource realmResource() {
    return keycloakAdminClient.realm(keycloakProperties.getRealm());
  }

  private ResourceNotFoundException notFoundGroup(String groupPath) {
    return new ResourceNotFoundException(
        ResponseCode.RESOURCE_NOT_FOUND, "Group not found: " + groupPath);
  }

  private Optional<GroupRepresentation> searchInGroupHierarchy(
      List<GroupRepresentation> groups, String targetPath) {
    for (GroupRepresentation group : groups) {
      if (targetPath.equals(group.getPath())) {
        return Optional.of(group);
      }
      if (group.getSubGroups() != null && !group.getSubGroups().isEmpty()) {
        Optional<GroupRepresentation> found =
            searchInGroupHierarchy(group.getSubGroups(), targetPath);
        if (found.isPresent()) {
          return found;
        }
      }
    }
    return Optional.empty();
  }
}
