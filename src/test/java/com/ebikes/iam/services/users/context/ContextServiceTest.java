package com.ebikes.iam.services.users.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.iam.adapters.keycloak.KeycloakUserAdapter;
import com.ebikes.iam.constants.ApplicationConstants.Keycloak;
import com.ebikes.iam.database.entities.Membership;
import com.ebikes.iam.database.entities.UserExtension;
import com.ebikes.iam.dtos.responses.context.ContextResponse;
import com.ebikes.iam.dtos.responses.memberships.MembershipResponse;
import com.ebikes.iam.exceptions.ResourceNotFoundException;
import com.ebikes.iam.services.users.membership.MembershipService;
import com.ebikes.iam.support.audit.AuditContext;
import com.ebikes.iam.support.audit.AuditTemplate;
import com.ebikes.iam.support.audit.ThrowingRunnable;
import com.ebikes.iam.support.context.ExecutionContext;
import com.ebikes.iam.support.fixtures.MembershipFixtures;
import com.ebikes.iam.support.fixtures.SecurityFixtures;
import com.ebikes.iam.support.fixtures.UserExtensionFixtures;

@DisplayName("ContextService")
@ExtendWith(MockitoExtension.class)
class ContextServiceTest {

  @Mock private AuditTemplate auditTemplate;
  @Mock private KeycloakUserAdapter keycloakUserAdapter;
  @Mock private MembershipService membershipService;

  @InjectMocks private ContextService contextService;

  @Captor private ArgumentCaptor<Map<String, String>> attributesCaptor;

  private UserExtension user;
  private Membership membership;

  @BeforeEach
  void setUp() {
    user = UserExtensionFixtures.active();
    membership = MembershipFixtures.forUser(user);
    ExecutionContext.set(
        user.getKeycloakUserId(),
        membership.getOrganizationId(),
        null,
        SecurityFixtures.TEST_EMAIL,
        java.util.Set.of(),
        SecurityFixtures.TEST_PHONE_NUMBER,
        java.util.Set.of());
  }

  @AfterEach
  void tearDown() {
    ExecutionContext.clear();
  }

  @SuppressWarnings("unchecked")
  private void stubAuditTemplateToExecute() {
    doAnswer(
            invocation -> {
              ThrowingRunnable<Exception> operation = invocation.getArgument(1);
              operation.run();
              return null;
            })
        .when(auditTemplate)
        .execute(any(AuditContext.class), any(ThrowingRunnable.class));
  }

  @Nested
  @DisplayName("getCurrentContext")
  class GetCurrentContext {

    @Test
    @DisplayName("should return context response for active membership")
    void shouldReturnContextResponseForActiveMembership() {
      when(membershipService.findMembershipInScope(
              null, user.getKeycloakUserId(), membership.getOrganizationId()))
          .thenReturn(Optional.of(membership));

      ContextResponse result = contextService.getCurrentContext();

      assertThat(result.organizationId()).isEqualTo(membership.getOrganizationId());
      assertThat(result.branchId()).isNull();
      assertThat(result.roles()).isEqualTo(membership.getRoles());
    }

    @Test
    @DisplayName("should throw when membership not found")
    void shouldThrowWhenMembershipNotFound() {
      when(membershipService.findMembershipInScope(
              null, user.getKeycloakUserId(), membership.getOrganizationId()))
          .thenReturn(Optional.empty());

      assertThatThrownBy(() -> contextService.getCurrentContext())
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should throw when called in system context")
    void shouldThrowWhenCalledInSystemContext() {

      ExecutionContext.clear();

      assertThatThrownBy(() -> contextService.getCurrentContext())
          .isInstanceOf(IllegalStateException.class);
    }
  }

  @Nested
  @DisplayName("getSwitchableMemberships")
  class GetSwitchableMemberships {

    @Test
    @DisplayName("should return memberships from service")
    void shouldReturnMembershipsFromService() {
      List<MembershipResponse> expected = List.of();
      when(membershipService.findMembershipsByKeycloakUserId(user.getKeycloakUserId()))
          .thenReturn(expected);

      List<MembershipResponse> result = contextService.getSwitchableMemberships();

      assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("should throw when called in system context")
    void shouldThrowWhenCalledInSystemContext() {
      ExecutionContext.clear();
      assertThatThrownBy(() -> contextService.getSwitchableMemberships())
          .isInstanceOf(IllegalStateException.class);
    }
  }

  @Nested
  @DisplayName("switchActiveMembership")
  class SwitchActiveMembership {

    @Test
    @DisplayName("should update Keycloak attributes with organisation and roles")
    void shouldUpdateKeycloakAttributesWithOrganisationAndRoles() {
      stubAuditTemplateToExecute();
      when(membershipService.findMembershipInScope(
              null, user.getKeycloakUserId(), membership.getOrganizationId()))
          .thenReturn(Optional.of(membership));

      contextService.switchActiveMembership(null, membership.getOrganizationId());

      verify(keycloakUserAdapter)
          .updateUserAttributes(eq(user.getKeycloakUserId()), attributesCaptor.capture());

      assertThat(attributesCaptor.getValue())
          .containsKey(Keycloak.ACTIVE_ORGANIZATION_ATTRIBUTE)
          .containsKey(Keycloak.ACTIVE_ORGANIZATION_ROLES_ATTRIBUTE)
          .doesNotContainKey(Keycloak.ACTIVE_BRANCH_ATTRIBUTE);
    }

    @Test
    @DisplayName("should include branch attribute when branchId is present")
    void shouldIncludeBranchAttributeWhenBranchIdPresent() {
      stubAuditTemplateToExecute();
      String branchId = UUID.randomUUID().toString();
      Membership branchMembership = MembershipFixtures.withBranch(user, branchId);
      when(membershipService.findMembershipInScope(
              branchId, user.getKeycloakUserId(), branchMembership.getOrganizationId()))
          .thenReturn(Optional.of(branchMembership));

      contextService.switchActiveMembership(branchId, branchMembership.getOrganizationId());

      verify(keycloakUserAdapter)
          .updateUserAttributes(eq(user.getKeycloakUserId()), attributesCaptor.capture());

      assertThat(attributesCaptor.getValue())
          .containsEntry(Keycloak.ACTIVE_BRANCH_ATTRIBUTE, branchId);
    }

    @Test
    @DisplayName("should throw when membership not found")
    void shouldThrowWhenMembershipNotFound() {
      when(membershipService.findMembershipInScope(
              null, user.getKeycloakUserId(), membership.getOrganizationId()))
          .thenReturn(Optional.empty());
      String testMembership = membership.getOrganizationId();
      assertThatThrownBy(() -> contextService.switchActiveMembership(null, testMembership))
          .isInstanceOf(ResourceNotFoundException.class);

      verify(keycloakUserAdapter, never()).updateUserAttributes(any(), any());
    }

    @Test
    @DisplayName("should throw when called in system context")
    void shouldThrowWhenCalledInSystemContext() {
      ExecutionContext.clear();
      String testMembership = membership.getOrganizationId();
      assertThatThrownBy(() -> contextService.switchActiveMembership(null, testMembership))
          .isInstanceOf(IllegalStateException.class);
    }
  }
}
