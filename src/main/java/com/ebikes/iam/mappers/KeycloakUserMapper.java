package com.ebikes.iam.mappers;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

import org.keycloak.representations.idm.UserRepresentation;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.ebikes.iam.dtos.requests.users.UpdateUserExtensionRequest;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface KeycloakUserMapper {

  default UserRepresentation toUserRepresentation(
      String email,
      @NotBlank String firstName,
      @NotBlank String lastName,
      @NotBlank String username) {
    UserRepresentation user = new UserRepresentation();
    user.setEmail(email);
    user.setEmailVerified(false);
    user.setEnabled(false);
    user.setFirstName(firstName);
    user.setRequiredActions(List.of("VERIFY_EMAIL", "UPDATE_PASSWORD"));
    user.setLastName(lastName);
    user.setUsername(username);

    return user;
  }

  default void updateUserRepresentation(
      UpdateUserExtensionRequest request, @MappingTarget UserRepresentation user) {
    if (request == null) {
      return;
    }

    if (request.email() != null) {
      user.setEmail(request.email());
    }

    user.setEmailVerified(request.emailVerified());

    if (request.firstName() != null) {
      user.setFirstName(request.firstName());
    }

    if (request.lastName() != null) {
      user.setLastName(request.lastName());
    }
  }
}
