package com.ebikes.iam.dtos.requests.users;

import com.ebikes.iam.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.Length;

import java.io.Serializable;
import java.util.Set;

public record CreateUserRequest(
    @Length(min = 36, max = 36) String branchId,
    @Size(max = 255) String branchName,
    @NotBlank(message = "Country code is required") @Size(min = 2, max = 2, message = "Country code must be 2 characters (ISO 3166-1 alpha-2)")
        String countryCode,
    @NotBlank(message = "Email is required") @Email(message = "Email must be valid") @Size(max = 255) String email,
    @NotBlank(message = "First name is required") @Size(max = 255) String firstName,
    Boolean isPrimary,
    @NotBlank(message = "Last name is required") @Size(max = 255) String lastName,
    @Length(min = 36, max = 36) String organizationId,
    @Size(max = 255) String organizationName,
    @NotBlank(message = "Phone number is required") @Pattern(
            regexp = "^\\+?[1-9]\\d{1,14}$",
            message = "Phone number must be valid E.164 format")
        @Size(max = 20) String phoneNumber,
    @NotEmpty(message = "At least one role is required") Set<UserRole> roles,
    @NotBlank(message = "Username is required") @Size(max = 255) String username)
    implements Serializable {
  public CreateUserRequest {
    roles = roles != null ? Set.copyOf(roles) : Set.of();
  }
}
