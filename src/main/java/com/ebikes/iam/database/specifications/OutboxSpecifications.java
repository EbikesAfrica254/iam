package com.ebikes.iam.database.specifications;

import com.ebikes.iam.database.entities.Outbox;
import com.ebikes.iam.dtos.requests.filters.OutboxFilter;
import com.ebikes.iam.enums.OutboxStatus;
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

public class OutboxSpecifications {

  public static final String FIELD_CREATED_AT = "createdAt";
  public static final String FIELD_EVENT_TYPE = "eventType";
  public static final String FIELD_RETRY_COUNT = "retryCount";
  public static final String FIELD_STATUS = "status";
  public static final String FIELD_UPDATED_AT = "updatedAt";

  public static final Set<String> ALLOWED_SORT_FIELDS =
      Set.of(FIELD_CREATED_AT, FIELD_EVENT_TYPE, FIELD_RETRY_COUNT, FIELD_STATUS, FIELD_UPDATED_AT);

  private OutboxSpecifications() {
    // prevent instantiation
  }

  public static Specification<Outbox> buildSpecification(OutboxFilter filter) {
    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();

      addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getEventType(),
          hasEventType(filter.getEventType()));

      addDateRange(
          predicates,
          root,
          query,
          criteriaBuilder,
          FIELD_CREATED_AT,
          filter.getCreatedAtFrom(),
          filter.getCreatedAtTo());
      addDateRange(
          predicates,
          root,
          query,
          criteriaBuilder,
          FIELD_UPDATED_AT,
          filter.getUpdatedAtFrom(),
          filter.getUpdatedAtTo());

      if (filter.getStatus() != null) {
        predicates.add(hasStatus(filter.getStatus()).toPredicate(root, query, criteriaBuilder));
      }

      if (filter.getMinRetryCount() != null) {
        predicates.add(
            hasMinRetryCount(filter.getMinRetryCount()).toPredicate(root, query, criteriaBuilder));
      }

      if (filter.getMaxRetryCount() != null) {
        predicates.add(
            hasMaxRetryCount(filter.getMaxRetryCount()).toPredicate(root, query, criteriaBuilder));
      }

      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }

  public static Specification<Outbox> hasEventType(String eventType) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(root.get(FIELD_EVENT_TYPE), eventType);
  }

  public static Specification<Outbox> hasMaxRetryCount(Integer maxRetryCount) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.lessThanOrEqualTo(root.get(FIELD_RETRY_COUNT), maxRetryCount);
  }

  public static Specification<Outbox> hasMinRetryCount(Integer minRetryCount) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.greaterThanOrEqualTo(root.get(FIELD_RETRY_COUNT), minRetryCount);
  }

  public static Specification<Outbox> hasStatus(OutboxStatus status) {
    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(FIELD_STATUS), status);
  }

  private static void addDateRange(
      List<Predicate> predicates,
      Root<Outbox> root,
      CriteriaQuery<?> query,
      CriteriaBuilder criteriaBuilder,
      String field,
      LocalDate from,
      LocalDate to) {
    if (from != null || to != null) {
      Specification<Outbox> spec = FilterUtilities.dateRangeBetween(field, from, to);
      predicates.add(spec.toPredicate(root, query, criteriaBuilder));
    }
  }

  private static void addIfPresent(
      List<Predicate> predicates,
      Root<Outbox> root,
      CriteriaQuery<?> query,
      CriteriaBuilder criteriaBuilder,
      String value,
      Specification<Outbox> spec) {
    if (value != null && !value.isBlank()) {
      predicates.add(spec.toPredicate(root, query, criteriaBuilder));
    }
  }
}
