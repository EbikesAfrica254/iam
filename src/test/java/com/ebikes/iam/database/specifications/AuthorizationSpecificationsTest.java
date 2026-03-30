package com.ebikes.iam.database.specifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.data.jpa.domain.Specification;

import com.ebikes.iam.database.entities.Contact;
import com.ebikes.iam.database.entities.UserExtension;
import com.ebikes.iam.enums.UserRole;
import com.ebikes.iam.exceptions.AuthorizationException;
import com.ebikes.iam.support.context.ExecutionContext;

@DisplayName("AuthorizationSpecifications")
class AuthorizationSpecificationsTest {

  private static final String BRANCH_ID = UUID.randomUUID().toString();
  private static final String ORGANIZATION_ID = UUID.randomUUID().toString();
  private static final String USER_ID = UUID.randomUUID().toString();

  @AfterEach
  void clearContext() {
    ExecutionContext.clear();
  }

  private void setContext(String organization, String branch, UserRole... roles) {
    ExecutionContext.set(
        USER_ID,
        organization,
        branch,
        "test@ebikes.test",
        Set.of(),
        null,
        Set.of(roles).stream().map(UserRole::name).collect(Collectors.toSet()));
  }

  @Nested
  @DisplayName("forContacts")
  class ForContacts {

    @Test
    @DisplayName("SYSTEM_ADMIN receives a specification without throwing")
    void systemAdminReceivesNoFilter() {
      setContext(ORGANIZATION_ID, null, UserRole.SYSTEM_ADMIN);

      Specification<Contact> spec = AuthorizationSpecifications.forContacts();

      assertThat(spec).isNotNull();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(
        value = UserRole.class,
        names = {
          "ORGANIZATION_ADMIN",
          "ORGANIZATION_CHECKER",
          "ORGANIZATION_FLEET_MANAGER",
          "ORGANIZATION_FLEET_SUPPORT",
          "ORGANIZATION_INVENTORY_MANAGER",
          "ORGANIZATION_MAKER",
          "ORGANIZATION_OPERATOR"
        })
    @DisplayName("organisation roles receive a specification without throwing")
    void organisationRolesReceiveOrgScopedFilter(UserRole role) {
      setContext(ORGANIZATION_ID, null, role);

      Specification<Contact> spec = AuthorizationSpecifications.forContacts();

      assertThat(spec).isNotNull();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(
        value = UserRole.class,
        names = {
          "BRANCH_ADMIN",
          "BRANCH_CHECKER",
          "BRANCH_FLEET_MANAGER",
          "BRANCH_FLEET_SUPPORT",
          "BRANCH_INVENTORY_MANAGER",
          "BRANCH_MAKER",
          "BRANCH_OPERATOR"
        })
    @DisplayName("branch roles receive a specification without throwing")
    void branchRolesReceiveBranchScopedFilter(UserRole role) {
      setContext(ORGANIZATION_ID, BRANCH_ID, role);

      Specification<Contact> spec = AuthorizationSpecifications.forContacts();

      assertThat(spec).isNotNull();
    }

    @Test
    @DisplayName("AGENT throws 403 — agents have no access to contacts")
    void agentThrowsForbidden() {
      setContext(ORGANIZATION_ID, BRANCH_ID, UserRole.AGENT);

      assertThatThrownBy(AuthorizationSpecifications::forContacts)
          .isInstanceOf(AuthorizationException.class);
    }

    @Test
    @DisplayName("CUSTOMER throws 403 — customers have no access to contacts")
    void customerThrowsForbidden() {
      setContext(ORGANIZATION_ID, null, UserRole.CUSTOMER);

      assertThatThrownBy(AuthorizationSpecifications::forContacts)
          .isInstanceOf(AuthorizationException.class);
    }

    @Test
    @DisplayName("branch role with null active branch throws 403")
    void branchRoleWithMissingBranchContextThrowsForbidden() {
      setContext(ORGANIZATION_ID, null, UserRole.BRANCH_ADMIN);

      assertThatThrownBy(AuthorizationSpecifications::forContacts)
          .isInstanceOf(AuthorizationException.class);
    }

    @Test
    @DisplayName("branch role with blank active branch throws 403")
    void branchRoleWithBlankBranchContextThrowsForbidden() {
      setContext(ORGANIZATION_ID, "   ", UserRole.BRANCH_ADMIN);

      assertThatThrownBy(AuthorizationSpecifications::forContacts)
          .isInstanceOf(AuthorizationException.class);
    }

    @Test
    @DisplayName("null active organization throws 403")
    void missingOrganizationContextThrowsForbidden() {
      setContext(null, null, UserRole.ORGANIZATION_ADMIN);

      assertThatThrownBy(AuthorizationSpecifications::forContacts)
          .isInstanceOf(AuthorizationException.class);
    }

    @Test
    @DisplayName("blank active organization throws 403")
    void blankOrganizationContextThrowsForbidden() {
      setContext("   ", null, UserRole.ORGANIZATION_ADMIN);

      assertThatThrownBy(AuthorizationSpecifications::forContacts)
          .isInstanceOf(AuthorizationException.class);
    }
  }

  @Nested
  @DisplayName("forUserExtensions")
  class ForUserExtensions {

    @Test
    @DisplayName("SYSTEM_ADMIN receives a specification without throwing")
    void systemAdminReceivesNoFilter() {
      setContext(ORGANIZATION_ID, null, UserRole.SYSTEM_ADMIN);

      Specification<UserExtension> spec = AuthorizationSpecifications.forUserExtensions();

      assertThat(spec).isNotNull();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(
        value = UserRole.class,
        names = {
          "ORGANIZATION_ADMIN",
          "ORGANIZATION_CHECKER",
          "ORGANIZATION_FLEET_MANAGER",
          "ORGANIZATION_FLEET_SUPPORT",
          "ORGANIZATION_INVENTORY_MANAGER",
          "ORGANIZATION_MAKER",
          "ORGANIZATION_OPERATOR"
        })
    @DisplayName("organisation roles receive a specification without throwing")
    void organisationRolesReceiveOrgScopedFilter(UserRole role) {
      setContext(ORGANIZATION_ID, null, role);

      Specification<UserExtension> spec = AuthorizationSpecifications.forUserExtensions();

      assertThat(spec).isNotNull();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(
        value = UserRole.class,
        names = {
          "BRANCH_ADMIN",
          "BRANCH_CHECKER",
          "BRANCH_FLEET_MANAGER",
          "BRANCH_FLEET_SUPPORT",
          "BRANCH_INVENTORY_MANAGER",
          "BRANCH_MAKER",
          "BRANCH_OPERATOR"
        })
    @DisplayName("branch roles receive a specification without throwing")
    void branchRolesReceiveBranchScopedFilter(UserRole role) {
      setContext(ORGANIZATION_ID, BRANCH_ID, role);

      Specification<UserExtension> spec = AuthorizationSpecifications.forUserExtensions();

      assertThat(spec).isNotNull();
    }

    @Test
    @DisplayName("AGENT receives self-scoped specification without throwing")
    void agentReceivesSelfScopedFilter() {
      setContext(ORGANIZATION_ID, BRANCH_ID, UserRole.AGENT);

      Specification<UserExtension> spec = AuthorizationSpecifications.forUserExtensions();

      assertThat(spec).isNotNull();
    }

    @Test
    @DisplayName("CUSTOMER receives self-scoped specification without throwing")
    void customerReceivesSelfScopedFilter() {
      setContext(ORGANIZATION_ID, null, UserRole.CUSTOMER);

      Specification<UserExtension> spec = AuthorizationSpecifications.forUserExtensions();

      assertThat(spec).isNotNull();
    }

    @Test
    @DisplayName("branch role with null active branch throws 403")
    void branchRoleWithMissingBranchContextThrowsForbidden() {
      setContext(ORGANIZATION_ID, null, UserRole.BRANCH_ADMIN);

      assertThatThrownBy(AuthorizationSpecifications::forUserExtensions)
          .isInstanceOf(AuthorizationException.class);
    }

    @Test
    @DisplayName("branch role with blank active branch throws 403")
    void branchRoleWithBlankBranchContextThrowsForbidden() {
      setContext(ORGANIZATION_ID, "   ", UserRole.BRANCH_ADMIN);

      assertThatThrownBy(AuthorizationSpecifications::forUserExtensions)
          .isInstanceOf(AuthorizationException.class);
    }

    @Test
    @DisplayName("null active organization throws 403")
    void missingOrganizationContextThrowsForbidden() {
      setContext(null, null, UserRole.ORGANIZATION_ADMIN);

      assertThatThrownBy(AuthorizationSpecifications::forUserExtensions)
          .isInstanceOf(AuthorizationException.class);
    }

    @Test
    @DisplayName("blank active organization throws 403")
    void blankOrganizationContextThrowsForbidden() {
      setContext("   ", null, UserRole.ORGANIZATION_ADMIN);

      assertThatThrownBy(AuthorizationSpecifications::forUserExtensions)
          .isInstanceOf(AuthorizationException.class);
    }
  }
}
