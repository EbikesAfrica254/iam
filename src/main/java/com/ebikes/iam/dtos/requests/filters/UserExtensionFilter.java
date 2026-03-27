package com.ebikes.iam.dtos.requests.filters;

import java.time.LocalDate;

import com.ebikes.iam.enums.UserStatus;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserExtensionFilter extends BaseFilter {
  private String branchId;
  private String countryCode;
  private LocalDate createdDateFrom;
  private LocalDate createdDateTo;
  private String email;
  private Boolean emailVerified;
  private String firstName;
  private String keycloakUserId;
  private String lastName;
  private String organizationId;
  private String phoneNumber;
  private Boolean phoneNumberVerified;
  private UserStatus status;
  private String username;
}
