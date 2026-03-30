package com.ebikes.iam.integration.services.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.ebikes.iam.adapters.organizations.OrganizationServiceAdapter;
import com.ebikes.iam.dtos.adapters.organizations.Branch;
import com.ebikes.iam.dtos.adapters.organizations.Organization;
import com.ebikes.iam.services.cache.OrganizationCacheService;
import com.ebikes.iam.support.infrastructure.AbstractIntegrationTest;

class OrganizationCacheServiceIT extends AbstractIntegrationTest {

  private static final String ORGANIZATION_ID = UUID.randomUUID().toString();
  private static final String ORGANIZATION_NAME = "Test Organization";
  private static final String BRANCH_ID = UUID.randomUUID().toString();
  private static final String BRANCH_NAME = "Test Branch";

  @Autowired private OrganizationCacheService organizationCacheService;
  @Autowired private RedisTemplate<String, String> redisTemplate;
  @MockitoBean private OrganizationServiceAdapter adapter;

  @BeforeEach
  void setUp() {
    assert redisTemplate.getConnectionFactory() != null;
    redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
  }

  @Nested
  class FindOrganizationNames {

    @Test
    @DisplayName("returns empty map when no organization ids are provided")
    void fetchesFromAdapterOnCacheMissAndCachesResult() {
      when(adapter.findOrganizationsByIds(Set.of(ORGANIZATION_ID)))
          .thenReturn(List.of(new Organization(ORGANIZATION_ID, ORGANIZATION_NAME)));

      Map<String, String> result =
          organizationCacheService.findOrganizationNames(Set.of(ORGANIZATION_ID));

      assertThat(result).containsEntry(ORGANIZATION_ID, ORGANIZATION_NAME);
      verify(adapter, times(1)).findOrganizationsByIds(Set.of(ORGANIZATION_ID));
    }

    @Test
    @DisplayName("returns empty map when no organization ids are provided")
    void returnsCachedValueWithoutCallingAdapter() {
      when(adapter.findOrganizationsByIds(Set.of(ORGANIZATION_ID)))
          .thenReturn(List.of(new Organization(ORGANIZATION_ID, ORGANIZATION_NAME)));

      organizationCacheService.findOrganizationNames(Set.of(ORGANIZATION_ID));
      Map<String, String> result =
          organizationCacheService.findOrganizationNames(Set.of(ORGANIZATION_ID));

      assertThat(result).containsEntry(ORGANIZATION_ID, ORGANIZATION_NAME);
      verify(adapter, times(1)).findOrganizationsByIds(any());
    }
  }

  @Nested
  class FindBranchNames {

    @Test
    @DisplayName("returns empty map when no branch ids are provided")
    void fetchesFromAdapterOnCacheMissAndCachesResult() {
      when(adapter.findBranchesByIds(ORGANIZATION_ID, Set.of(BRANCH_ID)))
          .thenReturn(List.of(new Branch(BRANCH_ID, BRANCH_NAME)));

      Map<String, String> result =
          organizationCacheService.findBranchNames(ORGANIZATION_ID, Set.of(BRANCH_ID));

      assertThat(result).containsEntry(BRANCH_ID, BRANCH_NAME);
      verify(adapter, times(1)).findBranchesByIds(ORGANIZATION_ID, Set.of(BRANCH_ID));
    }

    @Test
    @DisplayName("returns empty map when no branch ids are provided")
    void returnsCachedValueWithoutCallingAdapter() {
      when(adapter.findBranchesByIds(ORGANIZATION_ID, Set.of(BRANCH_ID)))
          .thenReturn(List.of(new Branch(BRANCH_ID, BRANCH_NAME)));

      organizationCacheService.findBranchNames(ORGANIZATION_ID, Set.of(BRANCH_ID));
      Map<String, String> result =
          organizationCacheService.findBranchNames(ORGANIZATION_ID, Set.of(BRANCH_ID));

      assertThat(result).containsEntry(BRANCH_ID, BRANCH_NAME);
      verify(adapter, times(1)).findBranchesByIds(any(), any());
    }
  }
}
