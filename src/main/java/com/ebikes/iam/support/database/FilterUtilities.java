package com.ebikes.iam.support.database;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.ebikes.iam.dtos.requests.filters.BaseFilter;
import com.ebikes.iam.enums.ResponseCode;
import com.ebikes.iam.exceptions.ValidationException;

import lombok.experimental.UtilityClass;

@UtilityClass
public class FilterUtilities {

  public static Pageable buildPageable(BaseFilter filter, Set<String> allowedSortFields) {
    String sortBy = filter.getSortBy();
    if (!allowedSortFields.contains(sortBy)) {
      throw new ValidationException(
          ResponseCode.INVALID_ARGUMENTS,
          "Invalid sort field specified. Allowed fields: " + allowedSortFields,
          "sortBy",
          sortBy);
    }
    Sort.Direction direction = Sort.Direction.fromString(filter.getSortDirection());
    Sort sort = Sort.by(direction, sortBy);
    int zeroIndexedPage = filter.getPage() - 1;
    return PageRequest.of(zeroIndexedPage, filter.getSize(), sort);
  }

  public static <T> Specification<T> dateRangeBetween(
      String fieldPath, LocalDate from, LocalDate to) {
    return (root, query, criteriaBuilder) -> {
      if (from != null && to != null) {
        OffsetDateTime fromDateTime = from.atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
        OffsetDateTime toDateTime =
            to.plusDays(1).atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
        return criteriaBuilder.between(root.get(fieldPath), fromDateTime, toDateTime);
      } else if (from != null) {
        OffsetDateTime fromDateTime = from.atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
        return criteriaBuilder.greaterThanOrEqualTo(root.get(fieldPath), fromDateTime);
      } else if (to != null) {
        OffsetDateTime toDateTime =
            to.plusDays(1).atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
        return criteriaBuilder.lessThan(root.get(fieldPath), toDateTime);
      }
      return criteriaBuilder.conjunction();
    };
  }

  public static <T> Specification<T> likeIgnoreCase(String fieldPath, String searchTerm) {
    return (root, query, criteriaBuilder) -> {
      String pattern = "%" + searchTerm.toLowerCase().trim() + "%";
      return criteriaBuilder.like(criteriaBuilder.lower(root.get(fieldPath)), pattern);
    };
  }
}
