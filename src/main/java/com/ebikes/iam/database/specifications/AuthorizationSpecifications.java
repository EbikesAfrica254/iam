package com.ebikes.iam.database.specifications;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.domain.Specification;

import com.ebikes.iam.database.entities.Contact;
import com.ebikes.iam.database.entities.UserExtension;
import com.ebikes.iam.enums.ResponseCode;
import com.ebikes.iam.enums.UserRole;
import com.ebikes.iam.exceptions.AuthorizationException;
import com.ebikes.iam.support.context.ExecutionContext;
import com.ebikes.iam.support.security.RBACUtilities;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AuthorizationSpecifications {

  private static final String FIELD_BRANCH_ID = "branchId";
  private static final String FIELD_KEYCLOAK_USER_ID = "keycloakUserId";
  private static final String FIELD_ORGANIZATION_ID = "organizationId";

  private AuthorizationSpecifications() {}

  public static Specification<Contact> forContacts() {
    if (!(ExecutionContext.get() instanceof ExecutionContext.UserContext ctx)) {
      throw new AuthorizationException(
          ResponseCode.FORBIDDEN, "Contacts are not accessible in system context");
    }
    Set<UserRole> roles = RBACUtilities.parseRoles(ctx.roles());
    Optional<Specification<Contact>> spec = resolveUpperTiers(roles, ctx);

    if (spec.isPresent()) {
      return spec.get();
    }

    log.error("Insufficient role for contact access: roles={}", roles);
    throw new AuthorizationException(
        ResponseCode.FORBIDDEN,
        "Contacts are only accessible to organisation-level roles and above");
  }

  public static Specification<UserExtension> forUserExtensions() {
    if (!(ExecutionContext.get() instanceof ExecutionContext.UserContext ctx)) {
      throw new AuthorizationException(
          ResponseCode.FORBIDDEN, "User extensions are not accessible in system context");
    }
    Set<UserRole> roles = RBACUtilities.parseRoles(ctx.roles());
    Optional<Specification<UserExtension>> spec = resolveUpperTiers(roles, ctx);

    if (spec.isPresent()) {
      return spec.get();
    }

    log.debug(
        "USER-level access: filtering by organizationId={}, keycloakUserId={}",
        ctx.activeOrganization(),
        ctx.userId());
    return Specification.allOf(
        filterByOrganizationId(validateActiveOrganization(ctx)),
        filterByKeycloakUserId(ctx.userId()));
  }

  private static <T> Optional<Specification<T>> resolveUpperTiers(
      Set<UserRole> roles, ExecutionContext.UserContext ctx) {

    if (roles.contains(UserRole.SYSTEM_ADMIN)) {
      log.debug("SYSTEM_ADMIN access: no filter applied");
      return Optional.of(noFilter());
    }

    if (roles.stream().anyMatch(RBACUtilities::isOrganizationRole)) {
      String organization = validateActiveOrganization(ctx);
      log.debug("ORGANIZATION-level access: filtering by organizationId={}", organization);
      return Optional.of(filterByOrganizationId(organization));
    }

    if (roles.stream().anyMatch(r -> RBACUtilities.isBranchRole(r) && r != UserRole.AGENT)) {
      String organization = validateActiveOrganization(ctx);
      String branch = validateActiveBranch(ctx);
      log.debug(
          "BRANCH-level access: filtering by organizationId={}, branchId={}", organization, branch);
      return Optional.of(
          Specification.allOf(filterByOrganizationId(organization), filterByBranchId(branch)));
    }

    return Optional.empty();
  }

  private static String validateActiveBranch(ExecutionContext.UserContext ctx) {
    String branch = ctx.activeBranch();
    if (branch == null || branch.isBlank()) {
      log.error("Missing active_branch claim");
      throw new AuthorizationException(
          ResponseCode.FORBIDDEN, "Active branch context required for this operation");
    }
    return branch;
  }

  private static String validateActiveOrganization(ExecutionContext.UserContext ctx) {
    String organization = ctx.activeOrganization();
    if (organization == null || organization.isBlank()) {
      log.error("Missing active_organization claim");
      throw new AuthorizationException(
          ResponseCode.FORBIDDEN, "Active organization context required for this operation");
    }
    return organization;
  }

  private static <T> Specification<T> filterByBranchId(String branchId) {
    return (root, query, cb) -> cb.equal(root.get(FIELD_BRANCH_ID), branchId);
  }

  private static <T> Specification<T> filterByKeycloakUserId(String keycloakUserId) {
    return (root, query, cb) -> cb.equal(root.get(FIELD_KEYCLOAK_USER_ID), keycloakUserId);
  }

  private static <T> Specification<T> filterByOrganizationId(String organizationId) {
    return (root, query, cb) -> cb.equal(root.get(FIELD_ORGANIZATION_ID), organizationId);
  }

  private static <T> Specification<T> noFilter() {
    return (root, query, cb) -> cb.conjunction();
  }
}
