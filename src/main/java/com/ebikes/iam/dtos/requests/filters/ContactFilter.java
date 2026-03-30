package com.ebikes.iam.dtos.requests.filters;

import java.time.OffsetDateTime;

import com.ebikes.iam.enums.ContactSourceType;
import com.ebikes.iam.enums.ContactStatus;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ContactFilter extends BaseFilter {
  private String branchId;
  private OffsetDateTime createdAtFrom;
  private OffsetDateTime createdAtTo;
  private OffsetDateTime expiresFrom;
  private OffsetDateTime expiresTo;
  private String organizationId;
  private String phoneNumber;
  private String sourceReference;
  private ContactSourceType sourceType;
  private ContactStatus status;
}
