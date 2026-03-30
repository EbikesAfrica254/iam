package com.ebikes.iam.services.users.provisioning;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.iam.database.entities.UserExtension;
import com.ebikes.iam.dtos.requests.memberships.CreateMembershipRequest;
import com.ebikes.iam.dtos.requests.users.CreateUserRequest;
import com.ebikes.iam.publishers.UserConfigurationEventPublisher;
import com.ebikes.iam.services.users.UserExtensionService;
import com.ebikes.iam.services.users.membership.MembershipService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class PersistenceService {

  private final MembershipService membershipService;
  private final UserConfigurationEventPublisher userConfigurationEventPublisher;
  private final UserExtensionService userExtensionService;

  @Transactional
  public void persist(
      String keycloakUserId,
      String groupPath,
      String organizationId,
      CreateMembershipRequest membershipRequest,
      CreateUserRequest userRequest) {

    UserExtension extension =
        userExtensionService.create(keycloakUserId, organizationId, userRequest);
    membershipService.createRecord(keycloakUserId, groupPath, membershipRequest, extension);
    userConfigurationEventPublisher.publishRequest(extension, organizationId);
  }
}
