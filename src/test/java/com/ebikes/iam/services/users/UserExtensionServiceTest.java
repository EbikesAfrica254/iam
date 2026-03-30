package com.ebikes.iam.services.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.ebikes.iam.adapters.keycloak.KeycloakUserAdapter;
import com.ebikes.iam.database.entities.Membership;
import com.ebikes.iam.database.entities.UserExtension;
import com.ebikes.iam.database.repositories.UserExtensionRepository;
import com.ebikes.iam.dtos.requests.filters.UserExtensionFilter;
import com.ebikes.iam.dtos.requests.users.CreateUserRequest;
import com.ebikes.iam.dtos.requests.users.UpdateUserExtensionRequest;
import com.ebikes.iam.dtos.responses.users.UserExtensionDetailResponse;
import com.ebikes.iam.dtos.responses.users.UserExtensionSummaryResponse;
import com.ebikes.iam.dtos.responses.users.UserProfileResponse;
import com.ebikes.iam.enums.UserRole;
import com.ebikes.iam.enums.UserStatus;
import com.ebikes.iam.exceptions.ResourceNotFoundException;
import com.ebikes.iam.exceptions.ValidationException;
import com.ebikes.iam.mappers.UserExtensionMapper;
import com.ebikes.iam.services.notifications.NotificationService;
import com.ebikes.iam.support.audit.AuditTemplate;
import com.ebikes.iam.support.audit.ThrowingRunnable;
import com.ebikes.iam.support.audit.ThrowingSupplier;
import com.ebikes.iam.support.context.ExecutionContext;
import com.ebikes.iam.support.fixtures.UserExtensionFixtures;

@DisplayName("UserExtensionService")
@ExtendWith(MockitoExtension.class)
class UserExtensionServiceTest {

  private static final String KEYCLOAK_USER_ID = UUID.randomUUID().toString();
  private static final String ORGANIZATION_ID = UUID.randomUUID().toString();
  private static final UUID USER_ID = UUID.randomUUID();

  @Mock private AuditTemplate auditTemplate;
  @Mock private KeycloakUserAdapter keycloakUserAdapter;
  @Mock private NotificationService notificationService;
  @Mock private UserExtensionMapper mapper;
  @Mock private UserExtensionRepository repository;

  private UserExtensionService service;

  @BeforeEach
  void setUp() {
    service =
        new UserExtensionService(
            auditTemplate, keycloakUserAdapter, notificationService, mapper, repository);

    ExecutionContext.set(
        UUID.randomUUID().toString(),
        ORGANIZATION_ID,
        null,
        "test@ebikes.test",
        Set.of(),
        null,
        Set.of(UserRole.ORGANIZATION_ADMIN.name()));
  }

  @AfterEach
  void tearDown() {
    ExecutionContext.clear();
  }

  @SuppressWarnings("unchecked")
  private void stubAuditRunnable() {
    doAnswer(
            invocation -> {
              ThrowingRunnable<?> runnable = invocation.getArgument(1);
              runnable.run();
              return null;
            })
        .when(auditTemplate)
        .execute(any(), any(ThrowingRunnable.class));
  }

  @SuppressWarnings("unchecked")
  private void stubAuditSupplier() {
    doAnswer(invocation -> invocation.<ThrowingSupplier<?, ?>>getArgument(1).get())
        .when(auditTemplate)
        .execute(any(), any(ThrowingSupplier.class));
  }

  @SuppressWarnings("unchecked")
  private void stubAuditSupplierWithExtractor() {
    doAnswer(invocation -> invocation.<ThrowingSupplier<?, ?>>getArgument(1).get())
        .when(auditTemplate)
        .execute(any(), any(ThrowingSupplier.class), any());
  }

  private UserExtensionDetailResponse detailResponse() {
    return new UserExtensionDetailResponse(
        null,
        "KE",
        OffsetDateTime.now(),
        null,
        "test@test.com",
        true,
        "John",
        UUID.randomUUID(),
        KEYCLOAK_USER_ID,
        "Doe",
        ORGANIZATION_ID,
        "+254700000001",
        false,
        UserStatus.ACTIVE,
        null,
        "johndoe");
  }

  private UserExtensionSummaryResponse summaryResponse() {
    return new UserExtensionSummaryResponse(
        OffsetDateTime.now(),
        "test@test.com",
        "John",
        UUID.randomUUID(),
        "Doe",
        "+254700000001",
        UserStatus.ACTIVE,
        null,
        "johndoe");
  }

  @Nested
  @DisplayName("activate")
  class Activate {

    @Test
    @DisplayName("should activate entity and save")
    void shouldActivateEntityAndSave() {
      UserExtension user = UserExtensionFixtures.inactive();

      service.activate(user);

      assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
      assertThat(user.isEmailVerified()).isTrue();
      verify(repository).save(user);
    }
  }

  @Nested
  @DisplayName("create")
  class Create {

    private CreateUserRequest request() {
      return new CreateUserRequest(
          null,
          "KE",
          "test@test.com",
          "John",
          true,
          "Doe",
          ORGANIZATION_ID,
          "+254700000001",
          Set.of(UserRole.CUSTOMER),
          "johndoe");
    }

    @Test
    @DisplayName("should save user extension and return it")
    void shouldSaveAndReturnUserExtension() {
      stubAuditSupplierWithExtractor();
      UserExtension saved = UserExtensionFixtures.active(ORGANIZATION_ID);
      when(repository.save(any())).thenReturn(saved);

      UserExtension result = service.create(KEYCLOAK_USER_ID, ORGANIZATION_ID, request());

      assertThat(result).isEqualTo(saved);
      verify(repository).save(any(UserExtension.class));
    }

    @Test
    @DisplayName("should send account verification notification after save")
    void shouldSendAccountVerificationAfterSave() {
      stubAuditSupplierWithExtractor();
      UserExtension saved = UserExtensionFixtures.active(ORGANIZATION_ID);
      when(repository.save(any())).thenReturn(saved);

      service.create(KEYCLOAK_USER_ID, ORGANIZATION_ID, request());

      verify(notificationService).sendAccountVerification(ORGANIZATION_ID, saved);
    }
  }

  @Nested
  @DisplayName("delete")
  class Delete {

    @Test
    @DisplayName("should disable Keycloak user, mark deleted, and save")
    void shouldDisableKeycloakMarkDeletedAndSave() {
      stubAuditRunnable();
      UserExtension user = UserExtensionFixtures.active();
      when(repository.findById(USER_ID)).thenReturn(Optional.of(user));

      service.delete(USER_ID);

      verify(keycloakUserAdapter).disableUser(user.getKeycloakUserId());
      assertThat(user.isDeleted()).isTrue();
      verify(repository).save(user);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when user not found")
    void shouldThrowWhenUserNotFound() {
      when(repository.findById(USER_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.delete(USER_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("deprovision")
  class Deprovision {

    @Test
    @DisplayName("should delete Keycloak user, mark deleted, and save")
    void shouldDeleteKeycloakMarkDeletedAndSave() {
      stubAuditRunnable();
      UserExtension user = UserExtensionFixtures.active();
      when(repository.findById(USER_ID)).thenReturn(Optional.of(user));

      service.deprovision(USER_ID);

      verify(keycloakUserAdapter).deleteUser(user.getKeycloakUserId());
      assertThat(user.isDeleted()).isTrue();
      verify(repository).save(user);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when user not found")
    void shouldThrowWhenUserNotFound() {
      when(repository.findById(USER_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.deprovision(USER_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("findById")
  class FindById {

    @Test
    @DisplayName("should return detail response for non-deleted user")
    void shouldReturnDetailResponseForNonDeletedUser() {
      UserExtension user = UserExtensionFixtures.active();
      UserExtensionDetailResponse response = detailResponse();
      when(repository.findById(USER_ID)).thenReturn(Optional.of(user));
      when(mapper.toDetailResponse(user)).thenReturn(response);

      assertThat(service.findById(USER_ID)).isEqualTo(response);
    }

    @Test
    @DisplayName("should return detail response for deleted user when caller is admin")
    void shouldReturnDetailResponseForDeletedUserWhenAdmin() {
      UserExtension user = UserExtensionFixtures.active();
      user.delete();
      UserExtensionDetailResponse response = detailResponse();
      when(repository.findById(USER_ID)).thenReturn(Optional.of(user));
      when(mapper.toDetailResponse(user)).thenReturn(response);

      assertThat(service.findById(USER_ID)).isEqualTo(response);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException for deleted user when caller is not admin")
    void shouldThrowForDeletedUserWhenNotAdmin() {
      ExecutionContext.clear();
      ExecutionContext.set(
          UUID.randomUUID().toString(),
          ORGANIZATION_ID,
          null,
          "test@ebikes.test",
          Set.of(),
          null,
          Set.of(UserRole.CUSTOMER.name()));

      UserExtension user = UserExtensionFixtures.active();
      user.delete();
      when(repository.findById(USER_ID)).thenReturn(Optional.of(user));

      assertThatThrownBy(() -> service.findById(USER_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when user not found")
    void shouldThrowWhenUserNotFound() {
      when(repository.findById(USER_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.findById(USER_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("findUserExtensionByEmail")
  class FindUserExtensionByEmail {

    @Test
    @DisplayName("should delegate to repository and return Optional")
    void shouldDelegateToRepository() {
      UserExtension user = UserExtensionFixtures.active();
      when(repository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

      assertThat(service.findUserExtensionByEmail("test@test.com")).contains(user);
    }
  }

  @Nested
  @DisplayName("findUserExtensionByKeycloakUserId")
  class FindUserExtensionByKeycloakUserId {

    @Test
    @DisplayName("should return user extension when found")
    void shouldReturnUserWhenFound() {
      UserExtension user = UserExtensionFixtures.active();
      when(repository.findByKeycloakUserId(KEYCLOAK_USER_ID)).thenReturn(Optional.of(user));

      assertThat(service.findUserExtensionByKeycloakUserId(KEYCLOAK_USER_ID)).isEqualTo(user);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when not found")
    void shouldThrowWhenNotFound() {
      when(repository.findByKeycloakUserId(KEYCLOAK_USER_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.findUserExtensionByKeycloakUserId(KEYCLOAK_USER_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("findUserExtensionByPhoneNumber")
  class FindUserExtensionByPhoneNumber {

    @Test
    @DisplayName("should delegate to repository and return Optional")
    void shouldDelegateToRepository() {
      UserExtension user = UserExtensionFixtures.active();
      when(repository.findByPhoneNumber("+254700000001")).thenReturn(Optional.of(user));

      assertThat(service.findUserExtensionByPhoneNumber("+254700000001")).contains(user);
    }
  }

  @Nested
  @DisplayName("me")
  class Me {

    @Test
    @DisplayName("should return profile response for matching org membership")
    void shouldReturnProfileForMatchingOrgMembership() {
      UserExtension user = UserExtensionFixtures.active(ORGANIZATION_ID);
      Membership membership =
          Membership.builder()
              .userExtension(user)
              .keycloakUserId(user.getKeycloakUserId())
              .organizationId(ORGANIZATION_ID)
              .keycloakGroupPath("/" + ORGANIZATION_ID)
              .isPrimary(true)
              .build();
      user.getMemberships().add(membership);

      UserProfileResponse response =
          new UserProfileResponse(
              null,
              "KE",
              OffsetDateTime.now(),
              user.getEmail(),
              true,
              user.getFirstName(),
              UUID.randomUUID(),
              user.getLastName(),
              List.of(),
              user.getPhoneNumber(),
              false,
              UserStatus.ACTIVE,
              user.getUsername());

      when(repository.findByKeycloakUserIdWithMemberships(any())).thenReturn(Optional.of(user));
      when(mapper.toProfileResponse(eq(user), eq(membership), any())).thenReturn(response);

      assertThat(service.me()).isEqualTo(response);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when user not found")
    void shouldThrowWhenUserNotFound() {
      when(repository.findByKeycloakUserIdWithMemberships(any())).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.me()).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should throw IllegalStateException when no matching membership found")
    void shouldThrowWhenNoMatchingMembership() {
      UserExtension user = UserExtensionFixtures.active(UUID.randomUUID().toString());
      when(repository.findByKeycloakUserIdWithMemberships(any())).thenReturn(Optional.of(user));

      assertThatThrownBy(() -> service.me())
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("No active membership found");
    }
  }

  @Nested
  @DisplayName("restore")
  class Restore {

    @Test
    @DisplayName("should restore entity and save")
    void shouldRestoreEntityAndSave() {
      stubAuditRunnable();
      UserExtension user = UserExtensionFixtures.active();
      user.delete();
      when(repository.findById(USER_ID)).thenReturn(Optional.of(user));

      service.restore(USER_ID);

      assertThat(user.isDeleted()).isFalse();
      verify(repository).save(user);
    }

    @Test
    @DisplayName("should throw ValidationException when user is not deleted")
    void shouldThrowWhenUserIsNotDeleted() {
      UserExtension user = UserExtensionFixtures.active();
      when(repository.findById(USER_ID)).thenReturn(Optional.of(user));

      assertThatThrownBy(() -> service.restore(USER_ID)).isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when user not found")
    void shouldThrowWhenUserNotFound() {
      when(repository.findById(USER_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.restore(USER_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("search")
  class Search {

    @Test
    @DisplayName("should build spec and pageable, query repository, and return mapped page")
    @SuppressWarnings("unchecked")
    void shouldBuildSpecAndPageableAndReturnMappedPage() {
      UserExtension user = UserExtensionFixtures.active(ORGANIZATION_ID);
      UserExtensionSummaryResponse summary = summaryResponse();
      when(repository.findAll(any(Specification.class), any(Pageable.class)))
          .thenReturn(new PageImpl<>(List.of(user)));
      when(mapper.toSummaryResponse(user)).thenReturn(summary);

      Page<UserExtensionSummaryResponse> result = service.search(new UserExtensionFilter());

      assertThat(result.getContent()).containsExactly(summary);
    }
  }

  @Nested
  @DisplayName("update")
  class Update {

    private UpdateUserExtensionRequest request() {
      return new UpdateUserExtensionRequest(
          "newemail@test.com", true, "Jane", "Smith", null, "+254711111111", false, null);
    }

    @Test
    @DisplayName("should update Keycloak, update entity, save, and return response")
    void shouldUpdateKeycloakEntitySaveAndReturnResponse() {
      stubAuditSupplier();
      UserExtension user = UserExtensionFixtures.active();
      UserExtensionDetailResponse response = detailResponse();
      UpdateUserExtensionRequest request = request();
      when(repository.findById(USER_ID)).thenReturn(Optional.of(user));
      when(repository.save(user)).thenReturn(user);
      when(mapper.toDetailResponse(user)).thenReturn(response);

      UserExtensionDetailResponse result = service.update(USER_ID, request);

      verify(keycloakUserAdapter).updateUser(user.getKeycloakUserId(), request);
      verify(repository).save(user);
      assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when user not found")
    void shouldThrowWhenUserNotFound() {
      when(repository.findById(USER_ID)).thenReturn(Optional.empty());

      UpdateUserExtensionRequest request = request();
      assertThatThrownBy(() -> service.update(USER_ID, request))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("updateStatus")
  class UpdateStatus {

    @Test
    @DisplayName("should update status and save")
    void shouldUpdateStatusAndSave() {
      stubAuditRunnable();
      UserExtension user = UserExtensionFixtures.active();
      when(repository.findById(USER_ID)).thenReturn(Optional.of(user));

      service.updateStatus(USER_ID, UserStatus.INACTIVE);

      assertThat(user.getStatus()).isEqualTo(UserStatus.INACTIVE);
      verify(repository).save(user);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when user not found")
    void shouldThrowWhenUserNotFound() {
      when(repository.findById(USER_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.updateStatus(USER_ID, UserStatus.INACTIVE))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("verifyEmail")
  class VerifyEmail {

    @Test
    @DisplayName("should call verifyEmail() and save")
    void shouldVerifyEmailAndSave() {
      UserExtension user = UserExtensionFixtures.inactive();

      service.verifyEmail(user);

      assertThat(user.isEmailVerified()).isTrue();
      verify(repository).save(user);
    }
  }

  @Nested
  @DisplayName("verifyPhone")
  class VerifyPhone {

    @Test
    @DisplayName("should call verifyPhone() and save")
    void shouldVerifyPhoneAndSave() {
      UserExtension user = UserExtensionFixtures.active();

      service.verifyPhone(user);

      assertThat(user.isPhoneNumberVerified()).isTrue();
      verify(repository).save(user);
    }
  }
}
