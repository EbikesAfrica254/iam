package com.ebikes.iam.services.users.provisioning;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.iam.database.entities.Membership;
import com.ebikes.iam.database.entities.UserExtension;
import com.ebikes.iam.enums.UserRole;
import com.ebikes.iam.exceptions.AuthorizationException;
import com.ebikes.iam.services.users.membership.MembershipService;
import com.ebikes.iam.support.context.ExecutionContext;
import com.ebikes.iam.support.fixtures.MembershipFixtures;
import com.ebikes.iam.support.fixtures.UserExtensionFixtures;

@DisplayName("AuthorizationService")
@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

  private static final String CREATOR_USER_ID = "creator-1";
  private static final String ORGANIZATION_ID = "org-1";
  private static final String BRANCH_ID = "branch-1";

  @Mock private MembershipService membershipService;

  private AuthorizationService service;
  private UserExtension creatorExtension;

  @BeforeEach
  void setUp() {
    service = new AuthorizationService(membershipService);
    creatorExtension = UserExtensionFixtures.withKeycloakId(CREATOR_USER_ID);
    ExecutionContext.set(
        CREATOR_USER_ID,
        ORGANIZATION_ID,
        null,
        "test@test.com",
        Collections.emptySet(),
        null,
        Collections.emptySet());
  }

  @AfterEach
  void tearDown() {
    ExecutionContext.clear();
  }

  @Test
  @DisplayName("should throw AuthorizationException when creator has no membership in organization")
  void shouldThrowWhenNoMembership() {
    when(membershipService.findMembershipInScope(any(), anyString(), anyString()))
        .thenReturn(Optional.empty());

    Set<UserRole> roles = Set.of(UserRole.CUSTOMER);
    assertThatThrownBy(() -> service.authorize(null, ORGANIZATION_ID, roles))
        .isInstanceOf(AuthorizationException.class);
  }

  @Test
  @DisplayName("should throw AuthorizationException when creator has no roles")
  void shouldThrowWhenNoRoles() {
    Membership membership = MembershipFixtures.forUser(creatorExtension);
    membership.updateRoles(Collections.emptySet());
    when(membershipService.findMembershipInScope(any(), anyString(), anyString()))
        .thenReturn(Optional.of(membership));

    Set<UserRole> roles = Set.of(UserRole.CUSTOMER);
    assertThatThrownBy(() -> service.authorize(null, ORGANIZATION_ID, roles))
        .isInstanceOf(AuthorizationException.class);
  }

  @Test
  @DisplayName(
      "should throw AuthorizationException when creator lacks authority to assign target role")
  void shouldThrowWhenInsufficientAuthority() {
    // BRANCH_ADMIN (authority=60) cannot create ORGANIZATION_ADMIN (requiredAuthority=80)
    Membership membership = MembershipFixtures.forUser(creatorExtension, UserRole.BRANCH_ADMIN);
    when(membershipService.findMembershipInScope(any(), anyString(), anyString()))
        .thenReturn(Optional.of(membership));

    Set<UserRole> roles = Set.of(UserRole.ORGANIZATION_ADMIN);
    assertThatThrownBy(() -> service.authorize(BRANCH_ID, ORGANIZATION_ID, roles))
        .isInstanceOf(AuthorizationException.class);
  }

  @Test
  @DisplayName("should authorize successfully when creator has sufficient authority")
  void shouldAuthorizeWhenSufficientAuthority() {
    // ORGANIZATION_ADMIN (authority=80) can create BRANCH_ADMIN (requiredAuthority=80)
    Membership membership =
        MembershipFixtures.forUser(creatorExtension, UserRole.ORGANIZATION_ADMIN);
    when(membershipService.findMembershipInScope(any(), anyString(), anyString()))
        .thenReturn(Optional.of(membership));

    assertThatNoException()
        .isThrownBy(
            () -> service.authorize(BRANCH_ID, ORGANIZATION_ID, Set.of(UserRole.BRANCH_ADMIN)));
  }

  @Test
  @DisplayName("should throw when BRANCH_ADMIN tries to create org-level user")
  void shouldThrowWhenBranchAdminCreatesOrgLevelUser() {
    ExecutionContext.set(
        CREATOR_USER_ID,
        ORGANIZATION_ID,
        BRANCH_ID,
        "test@test.com",
        Collections.emptySet(),
        null,
        Set.of(UserRole.BRANCH_ADMIN.name()));

    Membership membership = MembershipFixtures.forUser(creatorExtension, UserRole.BRANCH_ADMIN);
    when(membershipService.findMembershipInScope(any(), anyString(), anyString()))
        .thenReturn(Optional.of(membership));

    Set<UserRole> roles = Set.of(UserRole.CUSTOMER);
    assertThatThrownBy(() -> service.authorize(null, ORGANIZATION_ID, roles))
        .isInstanceOf(AuthorizationException.class);
  }

  @Test
  @DisplayName("should throw when BRANCH_ADMIN tries to create user in a different branch")
  void shouldThrowWhenBranchAdminCreateUserInDifferentBranch() {
    ExecutionContext.set(
        CREATOR_USER_ID,
        ORGANIZATION_ID,
        BRANCH_ID,
        "test@test.com",
        Collections.emptySet(),
        null,
        Set.of(UserRole.BRANCH_ADMIN.name()));

    Membership membership = MembershipFixtures.forUser(creatorExtension, UserRole.BRANCH_ADMIN);
    when(membershipService.findMembershipInScope(any(), anyString(), anyString()))
        .thenReturn(Optional.of(membership));

    Set<UserRole> roles = Set.of(UserRole.CUSTOMER);
    assertThatThrownBy(() -> service.authorize("other-branch", ORGANIZATION_ID, roles))
        .isInstanceOf(AuthorizationException.class);
  }

  @Test
  @DisplayName("should authorize when BRANCH_ADMIN creates user in their own branch")
  void shouldAuthorizeWhenBranchAdminCreatesUserInOwnBranch() {
    ExecutionContext.set(
        CREATOR_USER_ID,
        ORGANIZATION_ID,
        BRANCH_ID,
        "test@test.com",
        Collections.emptySet(),
        null,
        Set.of(UserRole.BRANCH_ADMIN.name()));

    // BRANCH_ADMIN (authority=60) can create CUSTOMER (requiredAuthority=40)
    Membership membership = MembershipFixtures.forUser(creatorExtension, UserRole.BRANCH_ADMIN);
    when(membershipService.findMembershipInScope(any(), anyString(), anyString()))
        .thenReturn(Optional.of(membership));

    assertThatNoException()
        .isThrownBy(() -> service.authorize(BRANCH_ID, ORGANIZATION_ID, Set.of(UserRole.CUSTOMER)));
  }
}
