package com.ebikes.iam.dtos.responses.context;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ContextResponse(
        String branchId, @NotNull String organizationId, @NotEmpty Set<String> roles)
        implements Serializable {
    public ContextResponse {
        roles = Collections.unmodifiableSet(roles);
    }
}
