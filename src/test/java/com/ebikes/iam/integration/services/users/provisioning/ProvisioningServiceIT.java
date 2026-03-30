package com.ebikes.iam.integration.services.users.provisioning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.ebikes.iam.adapters.keycloak.KeycloakGroupAdapter;
import com.ebikes.iam.adapters.keycloak.KeycloakUserAdapter;
import com.ebikes.iam.configurations.properties.KeycloakProperties;
import com.ebikes.iam.database.entities.Membership;
import com.ebikes.iam.database.entities.UserExtension;
import com.ebikes.iam.database.repositories.MembershipRepository;
import com.ebikes.iam.database.repositories.OutboxRepository;
import com.ebikes.iam.database.repositories.UserExtensionRepository;
import com.ebikes.iam.dtos.requests.users.CreateUserRequest;
import com.ebikes.iam.dtos.requests.users.SignupRequest;
import com.ebikes.iam.enums.UserRole;
import com.ebikes.iam.publishers.UserConfigurationEventPublisher;
import com.ebikes.iam.services.events.OutboxService;
import com.ebikes.iam.services.users.provisioning.AuthorizationService;
import com.ebikes.iam.services.users.provisioning.ProvisioningService;
import com.ebikes.iam.support.context.ExecutionContext;
import com.ebikes.iam.support.fixtures.UserRequestFixtures;
import com.ebikes.iam.support.infrastructure.AbstractIntegrationTest;

class ProvisioningServiceIT extends AbstractIntegrationTest {

  private static final String CREATOR_USER_ID = UUID.randomUUID().toString();
  private static final String ORGANIZATION_ID = UUID.randomUUID().toString();
  private static final String KEYCLOAK_USER_ID = UUID.randomUUID().toString();

  @Autowired private ProvisioningService provisioningService;
  @Autowired private OutboxService outboxService;
  @Autowired private OutboxRepository outboxRepository;
  @Autowired private MembershipRepository membershipRepository;
  @Autowired private UserExtensionRepository userExtensionRepository;

  @MockitoBean private AuthorizationService authorizationService;
  @MockitoBean private KeycloakGroupAdapter keycloakGroupAdapter;
  @MockitoBean private KeycloakUserAdapter keycloakUserAdapter;
  @MockitoBean private UserConfigurationEventPublisher userConfigurationEventPublisher;
  @Autowired private KeycloakProperties keycloakProperties;

  @BeforeEach
  void setUp() {
    outboxRepository.deleteAll();
    membershipRepository.deleteAll();
    userExtensionRepository.deleteAll();

    ExecutionContext.set(
        CREATOR_USER_ID,
        ORGANIZATION_ID,
        null,
        "creator@ebikes.test",
        Set.of(),
        "+254700000001",
        Set.of());

    when(keycloakUserAdapter.isEmailRegistered(anyString())).thenReturn(false);
    when(keycloakUserAdapter.createUser(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(KEYCLOAK_USER_ID);

    doAnswer(
            invocation -> {
              UserExtension extension = invocation.getArgument(0);
              String organizationId = invocation.getArgument(1);
              outboxService.save(
                  "iam.user.configuration.requested",
                  new UserConfigPayload(extension.getId().toString(), organizationId),
                  "iam.user.configuration.requested");
              return null;
            })
        .when(userConfigurationEventPublisher)
        .publishRequest(any(UserExtension.class), anyString());
  }

  @AfterEach
  void tearDown() {
    ExecutionContext.clear();
  }

  @Test
  @DisplayName("should persist user extension, membership and outbox")
  void shouldPersistUserExtensionMembershipAndOutboxWhenProvisionSucceeds() {
    CreateUserRequest request = UserRequestFixtures.createUser(ORGANIZATION_ID);

    provisioningService.provisionUser(request);

    assertThat(userExtensionRepository.count()).isEqualTo(1);
    assertThat(membershipRepository.count()).isEqualTo(1);
    assertThat(outboxRepository.count()).isEqualTo(3);

    UserExtension savedUser = userExtensionRepository.findAll().getFirst();
    assertThat(savedUser.getEmail()).isEqualTo(request.email());
    assertThat(savedUser.getOrganizationId()).isEqualTo(ORGANIZATION_ID);

    verify(keycloakUserAdapter)
        .createUser(request.email(), request.firstName(), request.lastName(), request.username());
    verify(keycloakUserAdapter).updateUserAttributes(anyString(), anyMap());
    verify(keycloakGroupAdapter).addUserToGroup(anyString(), eq(KEYCLOAK_USER_ID));
  }

  @Test
  @DisplayName("should compensate and persist no local state")
  void shouldCompensateAndPersistNoLocalStateWhenFailureOccursBeforePersistence() {
    CreateUserRequest request = UserRequestFixtures.createUser(ORGANIZATION_ID);

    doThrow(new RuntimeException("group failure"))
        .when(keycloakGroupAdapter)
        .addUserToGroup(anyString(), anyString());

    assertThatThrownBy(() -> provisioningService.provisionUser(request))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("group failure");

    assertThat(userExtensionRepository.count()).isZero();
    assertThat(membershipRepository.count()).isZero();
    assertThat(outboxRepository.count()).isZero();

    verify(keycloakUserAdapter).deleteUser(KEYCLOAK_USER_ID);
  }

  @Test
  @DisplayName("should compensate and rollback all local state")
  void shouldCompensateAndRollbackAllLocalStateWhenFailureOccursInsidePersistence() {
    CreateUserRequest request = UserRequestFixtures.createUser(ORGANIZATION_ID);

    doAnswer(
            invocation -> {
              UserExtension extension = invocation.getArgument(0);
              String organizationId = invocation.getArgument(1);
              outboxService.save(
                  "iam.user.configuration.requested",
                  new UserConfigPayload(extension.getId().toString(), organizationId),
                  "iam.user.configuration.requested");
              throw new RuntimeException("publish failure");
            })
        .when(userConfigurationEventPublisher)
        .publishRequest(any(UserExtension.class), anyString());

    assertThatThrownBy(() -> provisioningService.provisionUser(request))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("publish failure");

    assertThat(userExtensionRepository.count()).isZero();
    assertThat(membershipRepository.count()).isZero();
    assertThat(outboxRepository.count()).isZero();

    verify(keycloakUserAdapter).deleteUser(KEYCLOAK_USER_ID);
  }

  @Test
  @DisplayName("should persist customer primary user in base organization")
  void shouldPersistCustomerPrimaryUserInBaseOrganization() {
    SignupRequest request = UserRequestFixtures.signup();

    provisioningService.provisionSignup(request);

    assertThat(userExtensionRepository.count()).isEqualTo(1);
    assertThat(membershipRepository.count()).isEqualTo(1);
    assertThat(outboxRepository.count()).isEqualTo(3);

    UserExtension savedUser = userExtensionRepository.findAll().getFirst();
    Membership membership = membershipRepository.findAll().getFirst();

    assertThat(savedUser.getOrganizationId()).isEqualTo(keycloakProperties.getBaseOrganizationId());
    assertThat(membership.getOrganizationId())
        .isEqualTo(keycloakProperties.getBaseOrganizationId());
    assertThat(membership.getIsPrimary()).isTrue();
    assertThat(membership.getRoles()).containsExactly(UserRole.CUSTOMER.name());
  }

  private record UserConfigPayload(String userId, String organizationId) {}
}
