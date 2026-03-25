package com.ebikes.iam.database.specifications;

import com.ebikes.iam.database.entities.Contact;
import com.ebikes.iam.dtos.requests.filters.ContactFilter;
import com.ebikes.iam.enums.ContactSourceType;
import com.ebikes.iam.enums.ContactStatus;
import com.ebikes.iam.support.database.FilterUtilities;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ContactSpecifications {

    public static final String FIELD_BRANCH_ID = "branchId";
    public static final String FIELD_CREATED_AT = "createdAt";
    public static final String FIELD_EXPIRES_AT = "expiresAt";
    public static final String FIELD_ORGANIZATION_ID = "organizationId";
    public static final String FIELD_PHONE_NUMBER = "phoneNumber";
    public static final String FIELD_SOURCE_REFERENCE = "sourceReference";
    public static final String FIELD_SOURCE_TYPE = "sourceType";
    public static final String FIELD_STATUS = "status";

    public static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of(
                    FIELD_BRANCH_ID,
                    FIELD_CREATED_AT,
                    FIELD_EXPIRES_AT,
                    FIELD_ORGANIZATION_ID,
                    FIELD_PHONE_NUMBER,
                    FIELD_SOURCE_REFERENCE,
                    FIELD_SOURCE_TYPE,
                    FIELD_STATUS);

    private ContactSpecifications() {
        // prevent instantiation
    }

    public static Specification<Contact> buildSpecification(ContactFilter filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(
                    AuthorizationSpecifications.forContacts().toPredicate(root, query, criteriaBuilder));

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
                    filter.getSourceReference(),
                    hasSourceReference(filter.getSourceReference()));

            addDateRange(
                    predicates,
                    root,
                    query,
                    criteriaBuilder,
                    FIELD_CREATED_AT,
                    filter.getCreatedFrom(),
                    filter.getCreatedTo());
            addDateRange(
                    predicates,
                    root,
                    query,
                    criteriaBuilder,
                    FIELD_EXPIRES_AT,
                    filter.getExpiresFrom(),
                    filter.getExpiresTo());

            if (filter.getSourceType() != null) {
                predicates.add(
                        hasSourceType(filter.getSourceType()).toPredicate(root, query, criteriaBuilder));
            }

            if (filter.getStatus() != null) {
                predicates.add(hasStatus(filter.getStatus()).toPredicate(root, query, criteriaBuilder));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Contact> hasBranchId(String branchId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get(FIELD_BRANCH_ID), branchId);
    }

    public static Specification<Contact> hasOrganizationId(String organizationId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get(FIELD_ORGANIZATION_ID), organizationId);
    }

    public static Specification<Contact> hasPhoneNumber(String phoneNumber) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get(FIELD_PHONE_NUMBER), phoneNumber);
    }

    public static Specification<Contact> hasSourceReference(String sourceReference) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get(FIELD_SOURCE_REFERENCE), sourceReference);
    }

    public static Specification<Contact> hasSourceType(ContactSourceType sourceType) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get(FIELD_SOURCE_TYPE), sourceType);
    }

    public static Specification<Contact> hasStatus(ContactStatus status) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(FIELD_STATUS), status);
    }

    public static Specification<Contact> isUnresolved() {
        return hasStatus(ContactStatus.UNRESOLVED);
    }

    private static void addDateRange(
            List<Predicate> predicates,
            Root<Contact> root,
            CriteriaQuery<?> query,
            CriteriaBuilder criteriaBuilder,
            String field,
            LocalDate from,
            LocalDate to) {
        if (from != null || to != null) {
            Specification<Contact> spec = FilterUtilities.dateRangeBetween(field, from, to);
            predicates.add(spec.toPredicate(root, query, criteriaBuilder));
        }
    }

    private static void addIfPresent(
            List<Predicate> predicates,
            Root<Contact> root,
            CriteriaQuery<?> query,
            CriteriaBuilder criteriaBuilder,
            String value,
            Specification<Contact> spec) {
        if (value != null && !value.isBlank()) {
            predicates.add(spec.toPredicate(root, query, criteriaBuilder));
        }
    }
}
