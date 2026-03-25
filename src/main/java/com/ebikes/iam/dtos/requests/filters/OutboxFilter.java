package com.ebikes.iam.dtos.requests.filters;

import java.time.LocalDate;

import com.ebikes.iam.enums.OutboxStatus;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class OutboxFilter extends BaseFilter {
  private LocalDate createdAtFrom;

  private LocalDate createdAtTo;

  private String eventType;

  private Integer maxRetryCount;

  private Integer minRetryCount;

  private OutboxStatus status;

  private LocalDate updatedAtFrom;

  private LocalDate updatedAtTo;
}
