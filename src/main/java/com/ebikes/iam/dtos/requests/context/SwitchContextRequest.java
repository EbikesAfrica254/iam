package com.ebikes.iam.dtos.requests.context;

import jakarta.validation.constraints.NotBlank;

public record SwitchContextRequest(String branchId, @NotBlank String organizationId) {
}
