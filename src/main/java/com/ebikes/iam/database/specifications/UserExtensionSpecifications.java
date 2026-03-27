package com.ebikes.iam.database.specifications;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;

import com.ebikes.iam.database.entities.UserExtension;
import com.ebikes.iam.dtos.requests.filters.UserExtensionFilter;
import com.ebikes.iam.enums.UserStatus;
import com.ebikes.iam.support.database.FilterUtilities;

public class UserExtensionSpecifications {

  public static final String FIELD_BRANCH_ID = "branchId";
  public static final String FIELD_COUNTRY_CODE = "countryCode";
  public static final String FIELD_CREATED_AT = "createdAt";
  public static final String FIELD_EMAIL = "email";
  public static final String FIELD_EMAIL_VERIFIED = "emailVerified";
  public static final String FIELD_FIRST_NAME = "firstName";
  public static final String FIELD_KEYCLOAK_USER_ID = "keycloakUserId";
  public static final String FIELD_LAST_NAME = "lastName";
  public static final String FIELD_ORGANIZATION_ID = "organizationId";
  public static final String FIELD_PHONE_NUMBER = "phoneNumber";
  public static final String FIELD_PHONE_NUMBER_VERIFIED = "phoneNumberVerified";
  public static final String FIELD_STATUS = "status";
  public static final String FIELD_USERNAME = "username";

  public static final Set<String> ALLOWED_SORT_FIELDS =
      Set.of(
          FIELD_BRANCH_ID,
          FIELD_COUNTRY_CODE,
          FIELD_CREATED_AT,
          FIELD_EMAIL,
          FIELD_EMAIL_VERIFIED,
          FIELD_FIRST_NAME,
          FIELD_KEYCLOAK_USER_ID,
          FIELD_LAST_NAME,
          FIELD_ORGANIZATION_ID,
          FIELD_PHONE_NUMBER,
          FIELD_PHONE_NUMBER_VERIFIED,
          FIELD_STATUS,
          FIELD_USERNAME);

  private UserExtensionSpecifications() {
    // prevent instantiation
  }

  public static Specification<UserExtension> buildSpecification(UserExtensionFilter filter) {
    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();

      predicates.add(
          AuthorizationSpecifications.forUserExtensions()
              .toPredicate(root, query, criteriaBuilder));

      addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getBranchId(),
          hasBranchId(filter.getBranchId()));
      addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getCountryCode(),
          hasCountryCode(filter.getCountryCode()));
      addIfPresent(
          predicates, root, query, criteriaBuilder, filter.getEmail(), hasEmail(filter.getEmail()));
      addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getFirstName(),
          hasFirstName(filter.getFirstName()));
      addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getKeycloakUserId(),
          hasKeycloakUserId(filter.getKeycloakUserId()));
      addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getLastName(),
          hasLastName(filter.getLastName()));
      addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getOrganizationId(),
          hasOrganizationId(filter.getOrganizationId()));
      addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getPhoneNumber(),
          hasPhoneNumber(filter.getPhoneNumber()));
      addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getUsername(),
          hasUsername(filter.getUsername()));

      addDateRange(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getCreatedDateFrom(),
          filter.getCreatedDateTo());

      if (filter.getEmailVerified() != null) {
        predicates.add(isEmailVerified().toPredicate(root, query, criteriaBuilder));
      }

      if (filter.getPhoneNumberVerified() != null) {
        predicates.add(isPhoneNumberVerified().toPredicate(root, query, criteriaBuilder));
      }

      if (filter.getStatus() != null) {
        predicates.add(hasStatus(filter.getStatus()).toPredicate(root, query, criteriaBuilder));
      }

      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }

  public static Specification<UserExtension> hasBranchId(String branchId) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(root.get(FIELD_BRANCH_ID), branchId);
  }

  public static Specification<UserExtension> hasCountryCode(String countryCode) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(
            criteriaBuilder.lower(root.get(FIELD_COUNTRY_CODE)), countryCode.toLowerCase());
  }

  public static Specification<UserExtension> hasEmail(String email) {
    return FilterUtilities.likeIgnoreCase(FIELD_EMAIL, email);
  }

  public static Specification<UserExtension> hasFirstName(String firstName) {
    return FilterUtilities.likeIgnoreCase(FIELD_FIRST_NAME, firstName);
  }

  public static Specification<UserExtension> hasKeycloakUserId(String keycloakUserId) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(root.get(FIELD_KEYCLOAK_USER_ID), keycloakUserId);
  }

  public static Specification<UserExtension> hasLastName(String lastName) {
    return FilterUtilities.likeIgnoreCase(FIELD_LAST_NAME, lastName);
  }

  public static Specification<UserExtension> hasOrganizationId(String organizationId) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(root.get(FIELD_ORGANIZATION_ID), organizationId);
  }

  public static Specification<UserExtension> hasPhoneNumber(String phoneNumber) {
    return FilterUtilities.likeIgnoreCase(FIELD_PHONE_NUMBER, phoneNumber);
  }

  public static Specification<UserExtension> hasStatus(UserStatus status) {
    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(FIELD_STATUS), status);
  }

  public static Specification<UserExtension> hasUsername(String username) {
    return FilterUtilities.likeIgnoreCase(FIELD_USERNAME, username);
  }

  public static Specification<UserExtension> isEmailVerified() {
    return (root, query, criteriaBuilder) -> criteriaBuilder.isTrue(root.get(FIELD_EMAIL_VERIFIED));
  }

  public static Specification<UserExtension> isPhoneNumberVerified() {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.isTrue(root.get(FIELD_PHONE_NUMBER_VERIFIED));
  }

  private static void addDateRange(
      List<Predicate> predicates,
      Root<UserExtension> root,
      CriteriaQuery<?> query,
      CriteriaBuilder cb,
      LocalDate from,
      LocalDate to) {
    if (from != null || to != null) {
      Specification<UserExtension> spec =
          FilterUtilities.dateRangeBetween(UserExtensionSpecifications.FIELD_CREATED_AT, from, to);
      predicates.add(spec.toPredicate(root, query, cb));
    }
  }

  private static void addIfPresent(
      List<Predicate> predicates,
      Root<UserExtension> root,
      CriteriaQuery<?> query,
      CriteriaBuilder criteriaBuilder,
      String value,
      Specification<UserExtension> spec) {
    if (value != null && !value.isBlank()) {
      predicates.add(spec.toPredicate(root, query, criteriaBuilder));
    }
  }
}
