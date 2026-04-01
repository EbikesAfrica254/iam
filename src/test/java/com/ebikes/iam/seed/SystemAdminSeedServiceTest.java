package com.ebikes.iam.seed;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.iam.adapters.keycloak.KeycloakGroupAdapter;
import com.ebikes.iam.adapters.keycloak.KeycloakUserAdapter;
import com.ebikes.iam.configurations.properties.KeycloakProperties;
import com.ebikes.iam.configurations.properties.SeedProperties;
import com.ebikes.iam.constants.ApplicationConstants;
import com.ebikes.iam.database.entities.UserExtension;
import com.ebikes.iam.dtos.requests.memberships.CreateMembershipRequest;
import com.ebikes.iam.dtos.requests.users.CreateUserRequest;
import com.ebikes.iam.services.users.UserExtensionService;
import com.ebikes.iam.services.users.membership.MembershipService;
import com.ebikes.iam.support.context.ExecutionContext;
import com.ebikes.iam.support.fixtures.UserExtensionFixtures;

import net.datafaker.Faker;

@ExtendWith(MockitoExtension.class)
class SystemAdminSeedServiceTest {

  private static final Faker FAKER = new Faker();

  @Mock private KeycloakGroupAdapter keycloakGroupAdapter;
  @Mock private KeycloakUserAdapter keycloakUserAdapter;
  @Mock private KeycloakProperties keycloakProperties;
  @Mock private MembershipService membershipService;
  @Mock private SeedProperties seedProperties;
  @Mock private UserExtensionService userExtensionService;

  @InjectMocks private SystemAdminSeedService service;

  @AfterEach
  void clearContext() {
    ExecutionContext.clear();
  }

  @Nested
  class WhenAdminAlreadyExists {

    @BeforeEach
    void setUp() {
      when(seedProperties.getEmail()).thenReturn(FAKER.internet().emailAddress());
      when(keycloakUserAdapter.isEmailRegistered(anyString())).thenReturn(true);
    }

    @Test
    @DisplayName("skips all provisioning steps when admin user already exists")
    void seedIfAbsentSkipsAllProvisioning() {
      service.seedIfAbsent();

      verify(keycloakUserAdapter, never())
          .createUser(anyString(), anyString(), anyString(), anyString());
      verify(keycloakGroupAdapter, never()).addUserToGroup(anyString(), anyString());
      verify(userExtensionService, never()).create(anyString(), anyString(), any());
      verify(membershipService, never()).createRecord(anyString(), anyString(), any(), any());
    }

    @Test
    @DisplayName("leaves execution context clear even when admin already exists")
    void seedIfAbsentLeavesExecutionContextClear() {
      service.seedIfAbsent();

      assertThatThrownBy(ExecutionContext::get).isInstanceOf(IllegalStateException.class);
    }
  }

  @Nested
  class WhenAdminIsAbsent {

    private static final String KEYCLOAK_USER_ID = "kc-" + ApplicationConstants.SYSTEM_ID;
    private static final String ORGANIZATION_ID = ApplicationConstants.SYSTEM_ID;

    private UserExtension extension;

    @BeforeEach
    void setUp() {
      extension = UserExtensionFixtures.active();

      when(seedProperties.getEmail()).thenReturn(FAKER.internet().emailAddress());
      when(seedProperties.getFirstName()).thenReturn(FAKER.name().firstName());
      when(seedProperties.getLastName()).thenReturn(FAKER.name().lastName());
      when(seedProperties.getUsername()).thenReturn(FAKER.credentials().username());
      when(seedProperties.getPassword()).thenReturn(FAKER.credentials().password());
      when(seedProperties.getPhoneNumber()).thenReturn("+254" + FAKER.number().digits(9));

      when(keycloakUserAdapter.isEmailRegistered(anyString())).thenReturn(false);
      when(keycloakProperties.getBaseOrganizationId()).thenReturn(ORGANIZATION_ID);
      when(keycloakUserAdapter.createUser(anyString(), anyString(), anyString(), anyString()))
          .thenReturn(KEYCLOAK_USER_ID);
      when(userExtensionService.create(anyString(), anyString(), any(CreateUserRequest.class)))
          .thenReturn(extension);
    }

    @Test
    @DisplayName("creates keycloak user with provided seed properties when admin is absent")
    void seedIfAbsentCreatesKeycloakUser() {
      service.seedIfAbsent();

      verify(keycloakUserAdapter)
          .createUser(
              seedProperties.getEmail(),
              seedProperties.getFirstName(),
              seedProperties.getLastName(),
              seedProperties.getUsername());
    }

    @Test
    @DisplayName("updates user attributes and resets password when admin is absent")
    void seedIfAbsentUpdatesAttributesAndResetsPassword() {
      service.seedIfAbsent();

      verify(keycloakUserAdapter).updateUserAttributes(eq(KEYCLOAK_USER_ID), anyMap());
      verify(keycloakUserAdapter).resetPassword(eq(KEYCLOAK_USER_ID), anyString(), eq(true));
    }

    @Test
    @DisplayName("creates user in base organization and adds to system admin group")
    void seedIfAbsentAddsUserToGroup() {
      service.seedIfAbsent();

      verify(keycloakGroupAdapter).addUserToGroup(anyString(), eq(KEYCLOAK_USER_ID));
    }

    @Test
    @DisplayName("creates user extension and activates it")
    void seedIfAbsentCreatesExtensionAndActivates() {
      service.seedIfAbsent();

      verify(userExtensionService)
          .create(eq(KEYCLOAK_USER_ID), eq(ORGANIZATION_ID), any(CreateUserRequest.class));
      verify(keycloakUserAdapter).enableUser(KEYCLOAK_USER_ID);
      verify(userExtensionService).activate(extension);
    }

    @Test
    @DisplayName("creates membership record for system organization")
    void seedIfAbsentCreatesMembershipRecord() {
      service.seedIfAbsent();

      verify(membershipService)
          .createRecord(
              eq(KEYCLOAK_USER_ID), anyString(), any(CreateMembershipRequest.class), eq(extension));
    }

    @Test
    @DisplayName("clears execution context after successful provisioning")
    void seedIfAbsentClearsExecutionContextAfterSuccess() {
      service.seedIfAbsent();

      assertThatThrownBy(ExecutionContext::get).isInstanceOf(IllegalStateException.class);
    }
  }

  @Nested
  class WhenSeedFails {

    private static final String KEYCLOAK_USER_ID = "kc-" + ApplicationConstants.SYSTEM_ID;
    private static final String ORGANIZATION_ID = ApplicationConstants.SYSTEM_ID;

    @BeforeEach
    void setUp() {
      when(seedProperties.getEmail()).thenReturn(FAKER.internet().emailAddress());
      when(keycloakUserAdapter.isEmailRegistered(anyString())).thenReturn(false);
      when(keycloakProperties.getBaseOrganizationId()).thenReturn(ORGANIZATION_ID);
    }

    @Test
    @DisplayName(
        "compensates by deleting created user and rethrows exception when failure occurs after user"
            + " creation")
    void seedIfAbsentCompensatesAndRethrowsWhenFailureAfterUserCreated() {
      when(seedProperties.getFirstName()).thenReturn(FAKER.name().firstName());
      when(seedProperties.getLastName()).thenReturn(FAKER.name().lastName());
      when(seedProperties.getUsername()).thenReturn(FAKER.credentials().username());
      when(seedProperties.getPassword()).thenReturn(FAKER.credentials().password());
      when(seedProperties.getPhoneNumber()).thenReturn("+254" + FAKER.number().digits(9));
      when(keycloakUserAdapter.createUser(anyString(), anyString(), anyString(), anyString()))
          .thenReturn(KEYCLOAK_USER_ID);
      doThrow(new RuntimeException("group error"))
          .when(keycloakGroupAdapter)
          .addUserToGroup(anyString(), anyString());

      Runnable call = service::seedIfAbsent;
      assertThatThrownBy(call::run).isInstanceOf(RuntimeException.class);

      verify(keycloakUserAdapter).deleteUser(KEYCLOAK_USER_ID);
    }

    @Test
    @DisplayName("does not attempt compensation when failure occurs before user creation")
    void seedIfAbsentSkipsCompensationWhenFailureBeforeUserCreated() {
      when(seedProperties.getFirstName()).thenReturn(FAKER.name().firstName());
      when(seedProperties.getLastName()).thenReturn(FAKER.name().lastName());
      when(seedProperties.getUsername()).thenReturn(FAKER.credentials().username());
      doThrow(new RuntimeException("create error"))
          .when(keycloakUserAdapter)
          .createUser(anyString(), anyString(), anyString(), anyString());

      Runnable call = service::seedIfAbsent;
      assertThatThrownBy(call::run).isInstanceOf(RuntimeException.class);

      verify(keycloakUserAdapter, never()).deleteUser(anyString());
    }

    @Test
    @DisplayName("clears execution context even when provisioning fails before user creation")
    void seedIfAbsentClearsExecutionContextAfterFailure() {
      doThrow(new RuntimeException("fail"))
          .when(keycloakUserAdapter)
          .createUser(anyString(), anyString(), anyString(), anyString());

      when(seedProperties.getFirstName()).thenReturn(FAKER.name().firstName());
      when(seedProperties.getLastName()).thenReturn(FAKER.name().lastName());
      when(seedProperties.getUsername()).thenReturn(FAKER.credentials().username());

      try {
        service.seedIfAbsent();
      } catch (Exception ignored) {
        // exception is expected
      }

      assertThatThrownBy(ExecutionContext::get).isInstanceOf(IllegalStateException.class);
    }
  }
}
