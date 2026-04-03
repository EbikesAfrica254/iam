package com.ebikes.iam.adapters.organizations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.ebikes.iam.dtos.adapters.organizations.Branch;
import com.ebikes.iam.dtos.adapters.organizations.Organization;
import com.ebikes.iam.dtos.responses.api.SuccessResponse;
import com.ebikes.iam.exceptions.ExternalServiceException;

@DisplayName("OrganizationServiceAdapter")
@ExtendWith(MockitoExtension.class)
class OrganizationServiceAdapterTest {

  private static final String ORGANIZATION_ID = "org-1";
  private static final String BRANCH_ID = "branch-1";

  @Mock private RestClient restClient;

  private OrganizationServiceAdapter adapter;

  private RestClient.ResponseSpec responseSpec;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    adapter = new OrganizationServiceAdapter(restClient);

    @SuppressWarnings("rawtypes")
    RestClient.RequestHeadersUriSpec requestHeadersUriSpec =
        mock(RestClient.RequestHeadersUriSpec.class);
    RestClient.RequestHeadersSpec<?> requestHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
    responseSpec = mock(RestClient.ResponseSpec.class);

    when(restClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
  }

  @Nested
  @DisplayName("findOrganizationsByIds")
  class FindOrganizationsByIds {

    @Test
    @DisplayName("should return organisations from successful response")
    @SuppressWarnings("unchecked")
    void shouldReturnOrganisations() {
      List<Organization> organizations = List.of(new Organization(ORGANIZATION_ID, "Test Org"));
      when(responseSpec.body(any(ParameterizedTypeReference.class)))
          .thenReturn(SuccessResponse.of(organizations));

      List<Organization> result = adapter.findOrganizationsByIds(Set.of(ORGANIZATION_ID));

      assertThat(result).containsExactlyElementsOf(organizations);
    }

    @Test
    @DisplayName("should throw ExternalServiceException when response is null")
    @SuppressWarnings("unchecked")
    void shouldThrowWhenResponseIsNull() {
      when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(null);

      var ids = Set.of(ORGANIZATION_ID);
      assertThatThrownBy(() -> adapter.findOrganizationsByIds(ids))
          .isInstanceOf(ExternalServiceException.class);
    }

    @Test
    @DisplayName("should throw ExternalServiceException when response data is null")
    @SuppressWarnings("unchecked")
    void shouldThrowWhenResponseDataIsNull() {
      when(responseSpec.body(any(ParameterizedTypeReference.class)))
          .thenReturn(new SuccessResponse<>("SUCCESS", null, null));

      var ids = Set.of(ORGANIZATION_ID);
      assertThatThrownBy(() -> adapter.findOrganizationsByIds(ids))
          .isInstanceOf(ExternalServiceException.class);
    }

    @Test
    @DisplayName("should wrap RestClientException as ExternalServiceException")
    @SuppressWarnings("unchecked")
    void shouldWrapRestClientException() {
      when(responseSpec.body(any(ParameterizedTypeReference.class)))
          .thenThrow(new RestClientException("connection refused"));

      var ids = Set.of(ORGANIZATION_ID);
      assertThatThrownBy(() -> adapter.findOrganizationsByIds(ids))
          .isInstanceOf(ExternalServiceException.class);
    }

    @Test
    @DisplayName("should map HTTP status from HttpStatusCodeException")
    @SuppressWarnings("unchecked")
    void shouldMapHttpStatusFromStatusCodeException() {
      when(responseSpec.body(any(ParameterizedTypeReference.class)))
          .thenThrow(
              HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", null, null, null));

      var ids = Set.of(ORGANIZATION_ID);
      assertThatThrownBy(() -> adapter.findOrganizationsByIds(ids))
          .isInstanceOf(ExternalServiceException.class);
    }
  }

  @Nested
  @DisplayName("findBranchesByIds")
  class FindBranchesByIds {

    @Test
    @DisplayName("should return branches from successful response")
    @SuppressWarnings("unchecked")
    void shouldReturnBranches() {
      List<Branch> branches = List.of(new Branch(BRANCH_ID, "Test Branch"));
      when(responseSpec.body(any(ParameterizedTypeReference.class)))
          .thenReturn(SuccessResponse.of(branches));

      List<Branch> result = adapter.findBranchesByIds(ORGANIZATION_ID, Set.of(BRANCH_ID));

      assertThat(result).containsExactlyElementsOf(branches);
    }

    @Test
    @DisplayName("should throw ExternalServiceException when response is null")
    @SuppressWarnings("unchecked")
    void shouldThrowWhenResponseIsNull() {
      when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(null);

      var ids = Set.of(BRANCH_ID);
      assertThatThrownBy(() -> adapter.findBranchesByIds(ORGANIZATION_ID, ids))
          .isInstanceOf(ExternalServiceException.class);
    }

    @Test
    @DisplayName("should throw ExternalServiceException when response data is null")
    @SuppressWarnings("unchecked")
    void shouldThrowWhenResponseDataIsNull() {
      when(responseSpec.body(any(ParameterizedTypeReference.class)))
          .thenReturn(new SuccessResponse<>("SUCCESS", null, null));

      var ids = Set.of(BRANCH_ID);
      assertThatThrownBy(() -> adapter.findBranchesByIds(ORGANIZATION_ID, ids))
          .isInstanceOf(ExternalServiceException.class);
    }

    @Test
    @DisplayName("should wrap RestClientException as ExternalServiceException")
    @SuppressWarnings("unchecked")
    void shouldWrapRestClientException() {
      when(responseSpec.body(any(ParameterizedTypeReference.class)))
          .thenThrow(new RestClientException("connection refused"));

      var ids = Set.of(BRANCH_ID);
      assertThatThrownBy(() -> adapter.findBranchesByIds(ORGANIZATION_ID, ids))
          .isInstanceOf(ExternalServiceException.class);
    }

    @Test
    @DisplayName("should map HTTP status from HttpStatusCodeException")
    @SuppressWarnings("unchecked")
    void shouldMapHttpStatusFromStatusCodeException() {
      when(responseSpec.body(any(ParameterizedTypeReference.class)))
          .thenThrow(
              HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", null, null, null));

      var ids = Set.of(BRANCH_ID);
      assertThatThrownBy(() -> adapter.findBranchesByIds(ORGANIZATION_ID, ids))
          .isInstanceOf(ExternalServiceException.class);
    }
  }
}
