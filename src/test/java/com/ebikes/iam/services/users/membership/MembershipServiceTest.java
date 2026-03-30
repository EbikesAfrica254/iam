package com.ebikes.iam.services.users.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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

import com.ebikes.iam.adapters.keycloak.KeycloakGroupAdapter;
import com.ebikes.iam.configurations.properties.KeycloakProperties;
import com.ebikes.iam.database.entities.Membership;
import com.ebikes.iam.database.entities.UserExtension;
import com.ebikes.iam.database.repositories.MembershipRepository;
import com.ebikes.iam.dtos.requests.memberships.CreateMembershipRequest;
import com.ebikes.iam.dtos.responses.memberships.MembershipResponse;
import com.ebikes.iam.enums.UserRole;
import com.ebikes.iam.exceptions.BusinessRuleException;
import com.ebikes.iam.exceptions.ResourceNotFoundException;
import com.ebikes.iam.exceptions.ValidationException;
import com.ebikes.iam.mappers.MembershipEnricher;
import com.ebikes.iam.services.users.UserExtensionService;
import com.ebikes.iam.support.audit.AuditContext;
import com.ebikes.iam.support.audit.AuditTemplate;
import com.ebikes.iam.support.audit.ThrowingRunnable;
import com.ebikes.iam.support.audit.ThrowingSupplier;
import com.ebikes.iam.support.fixtures.MembershipFixtures;
import com.ebikes.iam.support.fixtures.UserExtensionFixtures;

@DisplayName("MembershipService")
@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {

  private static final String BASE_ORGANIZATION_ID = "base-org-id";
  private static final String KEYCLOAK_USER_ID = UUID.randomUUID().toString();

  @Mock private AuditTemplate auditTemplate;
  @Mock private KeycloakGroupAdapter keycloakGroupAdapter;
  @Mock private KeycloakProperties keycloakProperties;
  @Mock private MembershipEnricher membershipEnricher;
  @Mock private MembershipRepository repository;
  @Mock private UserExtensionService userExtensionService;

  @InjectMocks private MembershipService membershipService;

  private UserExtension activeUser;

  @BeforeEach
  void setUp() {
    activeUser = UserExtensionFixtures.active();
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

  @SuppressWarnings("unchecked")
  private void stubAuditTemplateToSupply() {
    doAnswer(
            invocation -> {
              ThrowingSupplier<?, ?> operation = invocation.getArgument(1);
              return operation.get();
            })
        .when(auditTemplate)
        .execute(any(AuditContext.class), any(ThrowingSupplier.class));
  }

  @Nested
  @DisplayName("create")
  class Create {

    private CreateMembershipRequest request;
    private Membership membership;
    private MembershipResponse response;

    @BeforeEach
    void setUp() {
      request =
          new CreateMembershipRequest(
              null, false, UUID.randomUUID().toString(), Set.of(UserRole.ORGANIZATION_ADMIN));
      membership = MembershipFixtures.forUser(activeUser);
      response =
          new MembershipResponse(
              null,
              null,
              null,
              false,
              null,
              KEYCLOAK_USER_ID,
              request.organizationId(),
              null,
              Set.of(UserRole.ORGANIZATION_ADMIN.name()),
              null);
      when(userExtensionService.findUserExtensionByKeycloakUserId(KEYCLOAK_USER_ID))
          .thenReturn(activeUser);
    }

    @Test
    @DisplayName("should add the user to the Keycloak group")
    void shouldAddUserToKeycloakGroup() {
      stubAuditTemplateToSupply();
      when(repository.save(any(Membership.class))).thenReturn(membership);
      when(membershipEnricher.enrich(any())).thenReturn(List.of(response));

      membershipService.create(KEYCLOAK_USER_ID, request);

      verify(keycloakGroupAdapter).addUserToGroup(any(), any());
    }

    @Test
    @DisplayName("should save the membership record")
    void shouldSaveMembershipRecord() {
      stubAuditTemplateToSupply();
      when(repository.save(any(Membership.class))).thenReturn(membership);
      when(membershipEnricher.enrich(any())).thenReturn(List.of(response));

      membershipService.create(KEYCLOAK_USER_ID, request);

      verify(repository).save(any(Membership.class));
    }

    @Test
    @DisplayName("should return the enriched membership response")
    void shouldReturnEnrichedMembershipResponse() {
      stubAuditTemplateToSupply();
      when(repository.save(any(Membership.class))).thenReturn(membership);
      when(membershipEnricher.enrich(any())).thenReturn(List.of(response));

      MembershipResponse result = membershipService.create(KEYCLOAK_USER_ID, request);

      assertThat(result).isEqualTo(response);
    }
  }

  @Nested
  @DisplayName("createRecord")
  class CreateRecord {

    private static final String ORGANIZATION_ID = UUID.randomUUID().toString();
    private static final String GROUP_PATH = "/" + ORGANIZATION_ID;

    private CreateMembershipRequest orgLevelRequest;
    private CreateMembershipRequest branchLevelRequest;

    @BeforeEach
    void setUp() {
      orgLevelRequest =
          new CreateMembershipRequest(
              null, false, ORGANIZATION_ID, Set.of(UserRole.ORGANIZATION_ADMIN));
      branchLevelRequest =
          new CreateMembershipRequest(
              UUID.randomUUID().toString(), false, ORGANIZATION_ID, Set.of(UserRole.BRANCH_ADMIN));
    }

    @Test
    @DisplayName("should throw when a branch membership already exists")
    void shouldThrowWhenBranchMembershipAlreadyExists() {
      when(repository.existsByBranchIdAndKeycloakUserIdAndOrganizationId(
              branchLevelRequest.branchId(), KEYCLOAK_USER_ID, ORGANIZATION_ID))
          .thenReturn(true);

      assertThatThrownBy(
              () ->
                  membershipService.createRecord(
                      KEYCLOAK_USER_ID, GROUP_PATH, branchLevelRequest, activeUser))
          .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("should throw when an organization-level membership already exists")
    void shouldThrowWhenOrgLevelMembershipAlreadyExists() {
      when(repository.existsByKeycloakUserIdAndOrganizationIdAndBranchIdIsNull(
              KEYCLOAK_USER_ID, ORGANIZATION_ID))
          .thenReturn(true);

      assertThatThrownBy(
              () ->
                  membershipService.createRecord(
                      KEYCLOAK_USER_ID, GROUP_PATH, orgLevelRequest, activeUser))
          .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("should clear the existing primary membership when isPrimary is true")
    void shouldClearExistingPrimaryWhenIsPrimaryTrue() {
      stubAuditTemplateToSupply();
      CreateMembershipRequest primaryRequest =
          new CreateMembershipRequest(
              null, true, ORGANIZATION_ID, Set.of(UserRole.ORGANIZATION_ADMIN));
      Membership existingPrimary = MembershipFixtures.forUser(activeUser);
      when(repository.findByIsPrimaryAndKeycloakUserId(true, KEYCLOAK_USER_ID))
          .thenReturn(List.of(existingPrimary));
      when(repository.save(any())).thenReturn(MembershipFixtures.forUser(activeUser));

      membershipService.createRecord(KEYCLOAK_USER_ID, GROUP_PATH, primaryRequest, activeUser);

      assertThat(existingPrimary.getIsPrimary()).isFalse();
      verify(repository).saveAll(List.of(existingPrimary));
    }

    @Test
    @DisplayName("should not clear primary membership when isPrimary is false")
    void shouldNotClearPrimaryWhenIsPrimaryFalse() {
      stubAuditTemplateToSupply();
      when(repository.save(any())).thenReturn(MembershipFixtures.secondary(activeUser));

      membershipService.createRecord(KEYCLOAK_USER_ID, GROUP_PATH, orgLevelRequest, activeUser);

      verify(repository, never()).findByIsPrimaryAndKeycloakUserId(any(), any());
    }

    @Test
    @DisplayName("should save and return the new membership")
    void shouldSaveAndReturnNewMembership() {
      stubAuditTemplateToSupply();
      Membership saved = MembershipFixtures.secondary(activeUser);
      when(repository.save(any(Membership.class))).thenReturn(saved);

      Membership result =
          membershipService.createRecord(KEYCLOAK_USER_ID, GROUP_PATH, orgLevelRequest, activeUser);

      verify(repository).save(any(Membership.class));
      assertThat(result).isEqualTo(saved);
    }
  }

  @Nested
  @DisplayName("removeMembership")
  class RemoveMembership {

    private static final String ORGANIZATION_ID = UUID.randomUUID().toString();

    private Membership membership;

    @BeforeEach
    void setUp() {
      membership = MembershipFixtures.forUser(activeUser);
    }

    @Test
    @DisplayName("should throw when removing from the base organization")
    void shouldThrowWhenRemovingFromBaseOrganization() {
      when(keycloakProperties.getBaseOrganizationId()).thenReturn(BASE_ORGANIZATION_ID);

      assertThatThrownBy(
              () ->
                  membershipService.removeMembership(null, KEYCLOAK_USER_ID, BASE_ORGANIZATION_ID))
          .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("should throw when the membership is not found")
    void shouldThrowWhenMembershipNotFound() {
      when(keycloakProperties.getBaseOrganizationId()).thenReturn(BASE_ORGANIZATION_ID);
      when(repository.findByKeycloakUserIdAndOrganizationIdAndBranchIdIsNull(
              KEYCLOAK_USER_ID, ORGANIZATION_ID))
          .thenReturn(Optional.empty());

      assertThatThrownBy(
              () -> membershipService.removeMembership(null, KEYCLOAK_USER_ID, ORGANIZATION_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should delete the membership record")
    void shouldDeleteMembershipRecord() {
      stubAuditTemplateToExecute();
      when(keycloakProperties.getBaseOrganizationId()).thenReturn(BASE_ORGANIZATION_ID);
      when(repository.findByKeycloakUserIdAndOrganizationIdAndBranchIdIsNull(
              KEYCLOAK_USER_ID, ORGANIZATION_ID))
          .thenReturn(Optional.of(membership));

      membershipService.removeMembership(null, KEYCLOAK_USER_ID, ORGANIZATION_ID);

      verify(repository).delete(membership);
    }

    @Test
    @DisplayName("should remove the user from the Keycloak group when no memberships remain")
    void shouldRemoveFromKeycloakGroupWhenNoMembershipsRemain() {
      stubAuditTemplateToExecute();
      when(keycloakProperties.getBaseOrganizationId()).thenReturn(BASE_ORGANIZATION_ID);
      when(repository.findByKeycloakUserIdAndOrganizationIdAndBranchIdIsNull(
              KEYCLOAK_USER_ID, ORGANIZATION_ID))
          .thenReturn(Optional.of(membership));
      when(repository.existsByKeycloakUserIdAndOrganizationIdAndBranchIdIsNull(
              KEYCLOAK_USER_ID, ORGANIZATION_ID))
          .thenReturn(false);

      membershipService.removeMembership(null, KEYCLOAK_USER_ID, ORGANIZATION_ID);

      verify(keycloakGroupAdapter).removeUserFromGroup(any(), any());
    }

    @Test
    @DisplayName("should not remove the user from the Keycloak group when memberships remain")
    void shouldNotRemoveFromKeycloakGroupWhenMembershipsRemain() {
      stubAuditTemplateToExecute();
      when(keycloakProperties.getBaseOrganizationId()).thenReturn(BASE_ORGANIZATION_ID);
      when(repository.findByKeycloakUserIdAndOrganizationIdAndBranchIdIsNull(
              KEYCLOAK_USER_ID, ORGANIZATION_ID))
          .thenReturn(Optional.of(membership));
      when(repository.existsByKeycloakUserIdAndOrganizationIdAndBranchIdIsNull(
              KEYCLOAK_USER_ID, ORGANIZATION_ID))
          .thenReturn(true);

      membershipService.removeMembership(null, KEYCLOAK_USER_ID, ORGANIZATION_ID);

      verify(keycloakGroupAdapter, never()).removeUserFromGroup(any(), any());
    }
  }

  @Nested
  @DisplayName("removeUserFromOrganization")
  class RemoveUserFromOrganization {

    private static final String ORGANIZATION_ID = UUID.randomUUID().toString();

    private Membership membership;

    @BeforeEach
    void setUp() {
      membership = MembershipFixtures.forUser(activeUser);
    }

    @Test
    @DisplayName("should throw when removing from the base organization")
    void shouldThrowWhenRemovingFromBaseOrganization() {
      when(keycloakProperties.getBaseOrganizationId()).thenReturn(BASE_ORGANIZATION_ID);

      assertThatThrownBy(
              () ->
                  membershipService.removeUserFromOrganization(
                      KEYCLOAK_USER_ID, BASE_ORGANIZATION_ID))
          .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("should throw when no memberships are found")
    void shouldThrowWhenNoMembershipsFound() {
      when(keycloakProperties.getBaseOrganizationId()).thenReturn(BASE_ORGANIZATION_ID);
      when(repository.findAllByKeycloakUserIdAndOrganizationId(KEYCLOAK_USER_ID, ORGANIZATION_ID))
          .thenReturn(List.of());

      assertThatThrownBy(
              () -> membershipService.removeUserFromOrganization(KEYCLOAK_USER_ID, ORGANIZATION_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should remove the user from the Keycloak group")
    void shouldRemoveUserFromKeycloakGroup() {
      stubAuditTemplateToExecute();
      when(keycloakProperties.getBaseOrganizationId()).thenReturn(BASE_ORGANIZATION_ID);
      when(repository.findAllByKeycloakUserIdAndOrganizationId(KEYCLOAK_USER_ID, ORGANIZATION_ID))
          .thenReturn(List.of(membership));

      membershipService.removeUserFromOrganization(KEYCLOAK_USER_ID, ORGANIZATION_ID);

      verify(keycloakGroupAdapter).removeUserFromGroup(any(), any());
    }

    @Test
    @DisplayName("should delete all memberships for the organization")
    void shouldDeleteAllMembershipsForOrganization() {
      stubAuditTemplateToExecute();
      when(keycloakProperties.getBaseOrganizationId()).thenReturn(BASE_ORGANIZATION_ID);
      List<Membership> memberships = List.of(membership);
      when(repository.findAllByKeycloakUserIdAndOrganizationId(KEYCLOAK_USER_ID, ORGANIZATION_ID))
          .thenReturn(memberships);

      membershipService.removeUserFromOrganization(KEYCLOAK_USER_ID, ORGANIZATION_ID);

      verify(repository).deleteAll(memberships);
    }
  }

  @Nested
  @DisplayName("setPrimaryMembership")
  class SetPrimaryMembership {

    private static final String ORGANIZATION_ID = UUID.randomUUID().toString();

    private Membership membership;

    @BeforeEach
    void setUp() {
      membership = MembershipFixtures.secondary(activeUser);
    }

    @Test
    @DisplayName("should throw when the membership is not found")
    void shouldThrowWhenMembershipNotFound() {
      when(repository.findByKeycloakUserIdAndOrganizationIdAndBranchIdIsNull(
              KEYCLOAK_USER_ID, ORGANIZATION_ID))
          .thenReturn(Optional.empty());

      assertThatThrownBy(
              () -> membershipService.setPrimaryMembership(KEYCLOAK_USER_ID, ORGANIZATION_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should clear the existing primary membership")
    void shouldClearExistingPrimaryMembership() {
      stubAuditTemplateToExecute();
      Membership existingPrimary = MembershipFixtures.forUser(activeUser);
      when(repository.findByKeycloakUserIdAndOrganizationIdAndBranchIdIsNull(
              KEYCLOAK_USER_ID, ORGANIZATION_ID))
          .thenReturn(Optional.of(membership));
      when(repository.findByIsPrimaryAndKeycloakUserId(true, KEYCLOAK_USER_ID))
          .thenReturn(List.of(existingPrimary));

      membershipService.setPrimaryMembership(KEYCLOAK_USER_ID, ORGANIZATION_ID);

      assertThat(existingPrimary.getIsPrimary()).isFalse();
    }

    @Test
    @DisplayName("should mark the membership as primary and save it")
    void shouldMarkMembershipAsPrimaryAndSave() {
      stubAuditTemplateToExecute();
      when(repository.findByKeycloakUserIdAndOrganizationIdAndBranchIdIsNull(
              KEYCLOAK_USER_ID, ORGANIZATION_ID))
          .thenReturn(Optional.of(membership));
      when(repository.findByIsPrimaryAndKeycloakUserId(any(), any())).thenReturn(List.of());

      membershipService.setPrimaryMembership(KEYCLOAK_USER_ID, ORGANIZATION_ID);

      assertThat(membership.getIsPrimary()).isTrue();
      verify(repository).save(membership);
    }
  }

  @Nested
  @DisplayName("setRoles")
  class SetRoles {

    private static final String ORGANIZATION_ID = UUID.randomUUID().toString();

    private Membership membership;

    @Captor private ArgumentCaptor<Membership> membershipCaptor;

    @BeforeEach
    void setUp() {
      membership = MembershipFixtures.forUser(activeUser);
    }

    @Test
    @DisplayName("should throw when the membership is not found")
    void shouldThrowWhenMembershipNotFound() {
      when(repository.findByKeycloakUserIdAndOrganizationIdAndBranchIdIsNull(
              KEYCLOAK_USER_ID, ORGANIZATION_ID))
          .thenReturn(Optional.empty());

      Set<UserRole> roles = Set.of(UserRole.CUSTOMER);
      assertThatThrownBy(
              () -> membershipService.setRoles(null, KEYCLOAK_USER_ID, ORGANIZATION_ID, roles))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should update the roles and save the membership")
    void shouldUpdateRolesAndSaveMembership() {
      stubAuditTemplateToExecute();
      when(repository.findByKeycloakUserIdAndOrganizationIdAndBranchIdIsNull(
              KEYCLOAK_USER_ID, ORGANIZATION_ID))
          .thenReturn(Optional.of(membership));

      membershipService.setRoles(
          null, KEYCLOAK_USER_ID, ORGANIZATION_ID, Set.of(UserRole.CUSTOMER));

      verify(repository).save(membershipCaptor.capture());
      assertThat(membershipCaptor.getValue().getRoles()).containsExactly(UserRole.CUSTOMER.name());
    }
  }
}
