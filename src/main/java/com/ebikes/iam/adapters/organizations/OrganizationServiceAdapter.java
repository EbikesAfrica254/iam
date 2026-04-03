package com.ebikes.iam.adapters.organizations;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.ebikes.iam.dtos.adapters.organizations.Branch;
import com.ebikes.iam.dtos.adapters.organizations.Organization;
import com.ebikes.iam.dtos.responses.api.SuccessResponse;
import com.ebikes.iam.enums.ResponseCode;
import com.ebikes.iam.exceptions.ExternalServiceException;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class OrganizationServiceAdapter {

  private static final String ORGANIZATIONS_REFERENCE_ENDPOINT = "/organizations/reference";
  private static final String BRANCHES_REFERENCE_ENDPOINT =
      "/organizations/{organizationId}/branches/reference";

  private final RestClient restClient;

  public OrganizationServiceAdapter(
      @Qualifier("organizationServiceRestClient") RestClient restClient) {
    this.restClient = restClient;
  }

  public List<Organization> findOrganizationsByIds(Set<String> organizationIds) {
    log.debug("Fetching organization references: count={}", organizationIds.size());

    try {
      SuccessResponse<List<Organization>> response =
          restClient
              .get()
              .uri(
                  uriBuilder ->
                      uriBuilder
                          .path(ORGANIZATIONS_REFERENCE_ENDPOINT)
                          .queryParam("ids", organizationIds)
                          .build())
              .retrieve()
              .body(new ParameterizedTypeReference<>() {});

      if (response == null || response.data() == null) {
        throw new ExternalServiceException(
            ORGANIZATIONS_REFERENCE_ENDPOINT,
            "Organization service returned empty response",
            ResponseCode.EXTERNAL_SERVICE_ERROR);
      }

      return response.data();

    } catch (RestClientException e) {
      log.error("Failed to fetch organization references: organizationIds={}", organizationIds, e);
      throw new ExternalServiceException(
          ORGANIZATIONS_REFERENCE_ENDPOINT,
          "Failed to fetch organization references: " + e.getMessage(),
          ResponseCode.fromHttpStatus(extractStatus(e)),
          e);
    }
  }

  public List<Branch> findBranchesByIds(String organizationId, Set<String> branchIds) {
    log.debug(
        "Fetching branch references: organizationId={} count={}", organizationId, branchIds.size());

    try {
      SuccessResponse<List<Branch>> response =
          restClient
              .get()
              .uri(
                  uriBuilder ->
                      uriBuilder
                          .path(BRANCHES_REFERENCE_ENDPOINT)
                          .queryParam("ids", branchIds)
                          .build(organizationId))
              .retrieve()
              .body(new ParameterizedTypeReference<>() {});

      if (response == null || response.data() == null) {
        throw new ExternalServiceException(
            BRANCHES_REFERENCE_ENDPOINT,
            "Organization service returned empty response for organizationId=" + organizationId,
            ResponseCode.EXTERNAL_SERVICE_ERROR);
      }

      return response.data();

    } catch (RestClientException e) {
      log.error(
          "Failed to fetch branch references: organizationId={} branchIds={}",
          organizationId,
          branchIds,
          e);
      throw new ExternalServiceException(
          BRANCHES_REFERENCE_ENDPOINT,
          "Failed to fetch branch references: " + e.getMessage(),
          ResponseCode.fromHttpStatus(extractStatus(e)),
          e);
    }
  }

  private int extractStatus(RestClientException e) {
    if (e instanceof HttpStatusCodeException statusEx) {
      return statusEx.getStatusCode().value();
    }
    return 500;
  }
}
