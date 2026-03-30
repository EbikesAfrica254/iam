package com.ebikes.iam.services.users.provisioning;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.iam.adapters.keycloak.KeycloakGroupAdapter;
import com.ebikes.iam.adapters.keycloak.KeycloakUserAdapter;
import com.ebikes.iam.configurations.properties.KeycloakProperties;
import com.ebikes.iam.dtos.requests.users.CreateUserRequest;
import com.ebikes.iam.dtos.requests.users.SignupRequest;
import com.ebikes.iam.enums.UserRole;
import com.ebikes.iam.exceptions.DuplicateResourceException;
import com.ebikes.iam.exceptions.ValidationException;
import com.ebikes.iam.support.context.ExecutionContext;
import com.ebikes.iam.support.fixtures.UserRequestFixtures;

@DisplayName("ProvisioningService")
@ExtendWith(MockitoExtension.class)
class ProvisioningServiceTest {

  private static final String CREATOR_USER_ID = UUID.randomUUID().toString();
  private static final String ORGANIZATION_ID = UUID.randomUUID().toString();
  private static final String KEYCLOAK_USER_ID = UUID.randomUUID().toString();

  @Mock private AuthorizationService authorizationService;
  @Mock private KeycloakGroupAdapter keycloakGroupAdapter;
  @Mock private KeycloakProperties keycloakProperties;
  @Mock private KeycloakUserAdapter keycloakUserAdapter;
  @Mock private PersistenceService persistenceService;

  @InjectMocks private ProvisioningService provisioningService;

  @BeforeEach
  void setUp() {
    ExecutionContext.set(
        CREATOR_USER_ID,
        ORGANIZATION_ID,
        null,
        "creator@ebikes.test",
        Set.of(),
        "+254700000001",
        Set.of());
  }

  @AfterEach
  void tearDown() {
    ExecutionContext.clear();
  }

  @Nested
  @DisplayName("provisionUser")
  class ProvisionUser {

    private CreateUserRequest request;

    @BeforeEach
    void setUp() {
      request = UserRequestFixtures.createUser(ORGANIZATION_ID);
    }

    private void stubHappyPath() {
      when(keycloakUserAdapter.createUser(any(), any(), any(), any())).thenReturn(KEYCLOAK_USER_ID);
    }

    @Test
    @DisplayName("should check authorization before any external call")
    void shouldCheckAuthorizationBeforeAnyExternalCall() {
      stubHappyPath();

      provisioningService.provisionUser(request);

      InOrder inOrder = inOrder(authorizationService, keycloakUserAdapter);
      inOrder.verify(authorizationService).authorize(any(), any(), any());
      inOrder.verify(keycloakUserAdapter).createUser(any(), any(), any(), any());
    }

    @Test
    @DisplayName("should throw when the email is already registered")
    void shouldThrowWhenEmailAlreadyRegistered() {
      when(keycloakUserAdapter.isEmailRegistered(request.email())).thenReturn(true);

      assertThatThrownBy(() -> provisioningService.provisionUser(request))
          .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("should throw when no roles are provided")
    void shouldThrowWhenNoRolesProvided() {
      CreateUserRequest noRolesRequest =
          new CreateUserRequest(
              null,
              request.countryCode(),
              request.email(),
              request.firstName(),
              request.isPrimary(),
              request.lastName(),
              request.organizationId(),
              request.phoneNumber(),
              Set.of(),
              request.username());

      assertThatThrownBy(() -> provisioningService.provisionUser(noRolesRequest))
          .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("should create the user in Keycloak")
    void shouldCreateUserInKeycloak() {
      stubHappyPath();

      provisioningService.provisionUser(request);

      verify(keycloakUserAdapter)
          .createUser(request.email(), request.firstName(), request.lastName(), request.username());
    }

    @Test
    @DisplayName("should add the user to the Keycloak group")
    void shouldAddUserToKeycloakGroup() {
      stubHappyPath();

      provisioningService.provisionUser(request);

      verify(keycloakGroupAdapter).addUserToGroup(anyString(), eq(KEYCLOAK_USER_ID));
    }

    @Test
    @DisplayName("should delegate persistence to PersistenceService")
    void shouldDelegatePersistence() {
      stubHappyPath();

      provisioningService.provisionUser(request);

      verify(persistenceService)
          .persist(eq(KEYCLOAK_USER_ID), anyString(), eq(ORGANIZATION_ID), any(), eq(request));
    }

    @Test
    @DisplayName(
        "should compensate by deleting the Keycloak user when an error occurs after creation")
    void shouldCompensateWhenErrorOccursAfterKeycloakUserCreated() {
      when(keycloakUserAdapter.createUser(any(), any(), any(), any())).thenReturn(KEYCLOAK_USER_ID);
      RuntimeException cause = new RuntimeException("downstream failure");
      doThrow(cause).when(keycloakGroupAdapter).addUserToGroup(any(), any());

      assertThatThrownBy(() -> provisioningService.provisionUser(request)).isSameAs(cause);

      verify(keycloakUserAdapter).deleteUser(KEYCLOAK_USER_ID);
    }

    @Test
    @DisplayName("should not compensate when the error occurs before Keycloak user is created")
    void shouldNotCompensateWhenErrorOccursBeforeKeycloakUserCreated() {
      when(keycloakUserAdapter.isEmailRegistered(request.email())).thenReturn(true);

      assertThatThrownBy(() -> provisioningService.provisionUser(request))
          .isInstanceOf(DuplicateResourceException.class);

      verify(keycloakUserAdapter, never()).deleteUser(any());
    }
  }

  @Nested
  @DisplayName("provisionSignup")
  class ProvisionSignup {

    private SignupRequest signupRequest;

    @BeforeEach
    void setUp() {
      signupRequest = UserRequestFixtures.signup();
      when(keycloakProperties.getBaseOrganizationId()).thenReturn(ORGANIZATION_ID);
      when(keycloakUserAdapter.createUser(any(), any(), any(), any())).thenReturn(KEYCLOAK_USER_ID);
    }

    @Test
    @DisplayName("should assign the CUSTOMER role")
    void shouldAssignCustomerRole() {
      provisioningService.provisionSignup(signupRequest);

      verify(persistenceService)
          .persist(
              any(),
              any(),
              any(),
              any(),
              argThat(req -> req.roles().equals(Set.of(UserRole.CUSTOMER))));
    }

    @Test
    @DisplayName("should set isPrimary to true")
    void shouldSetIsPrimaryToTrue() {
      provisioningService.provisionSignup(signupRequest);

      verify(persistenceService)
          .persist(
              any(), any(), any(), any(), argThat(req -> Boolean.TRUE.equals(req.isPrimary())));
    }

    @Test
    @DisplayName("should create the user in Keycloak with signup request fields")
    void shouldCreateKeycloakUserWithSignupFields() {
      provisioningService.provisionSignup(signupRequest);

      verify(keycloakUserAdapter)
          .createUser(
              signupRequest.email(), signupRequest.firstName(),
              signupRequest.lastName(), signupRequest.username());
    }
  }
}
