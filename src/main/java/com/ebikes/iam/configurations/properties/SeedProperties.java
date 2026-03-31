package com.ebikes.iam.configurations.properties;

import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@ConfigurationProperties(prefix = "seed.system-admin")
@Component
@Data
public class SeedProperties {

  @NotBlank private String email;
  @NotBlank private String firstName;
  @NotBlank private String lastName;
  @NotBlank private String password;
  @NotBlank private String phoneNumber;
  @NotBlank private String username;
}
