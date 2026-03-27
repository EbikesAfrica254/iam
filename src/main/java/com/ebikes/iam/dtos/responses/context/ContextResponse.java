package com.ebikes.iam.dtos.responses.context;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ContextResponse(
    String branchId, @NotNull String organizationId, @NotEmpty Set<String> roles)
    implements Serializable {
  public ContextResponse {
    roles = Collections.unmodifiableSet(roles);
  }
}
