package com.ebikes.iam.dtos.requests.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "Country code is required") @Size(min = 2, max = 2, message = "Country code must be 2 characters (ISO 3166-1 alpha-2)")
        String countryCode,
        @NotBlank(message = "Email is required") @Email(message = "Email must be valid") @Size(max = 255) String email,
        @NotBlank(message = "First name is required") @Size(max = 255) String firstName,
        @NotBlank(message = "Last name is required") @Size(max = 255) String lastName,
        @NotBlank(message = "Phone number is required") @Pattern(
                regexp = "^\\+?[1-9]\\d{1,14}$",
                message = "Phone number must be valid E.164 format")
        @Size(max = 20) String phoneNumber,
        @NotBlank(message = "Username is required") @Size(max = 255) String username) {
}
