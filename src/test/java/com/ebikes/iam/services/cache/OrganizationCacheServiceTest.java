package com.ebikes.iam.services.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.ebikes.iam.adapters.organizations.OrganizationServiceAdapter;
import com.ebikes.iam.configurations.properties.CacheProperties;
import com.ebikes.iam.dtos.adapters.organizations.Branch;
import com.ebikes.iam.dtos.adapters.organizations.Organization;

@DisplayName("OrganizationCacheService")
@ExtendWith(MockitoExtension.class)
class OrganizationCacheServiceTest {

  private static final String ORGANIZATION_ID = "org-1";
  private static final String ORGANIZATION_NAME = "Test Org";
  private static final String BRANCH_ID = "branch-1";
  private static final String BRANCH_NAME = "Test Branch";
  private static final int TTL_MINUTES = 10;

  private static final String ORGANIZATION_CACHE_KEY =
      "organizations:organization:" + ORGANIZATION_ID;
  private static final String BRANCH_CACHE_KEY =
      "organizations:branch:" + ORGANIZATION_ID + ":" + BRANCH_ID;

  @Mock private CacheProperties cacheProperties;
  @Mock private OrganizationServiceAdapter adapter;
  @Mock private RedisTemplate<String, String> redisTemplate;
  @Mock private ValueOperations<String, String> valueOperations;

  private OrganizationCacheService service;

  @BeforeEach
  void setUp() {
    service = new OrganizationCacheService(cacheProperties, adapter, redisTemplate);
  }

  @Test
  @DisplayName("findOrganizationNames should return empty map when input is empty")
  void findOrganizationNamesShouldReturnEmptyMapWhenInputIsEmpty() {
    Map<String, String> result = service.findOrganizationNames(Set.of());

    assertThat(result).isEmpty();
    verify(adapter, never()).findOrganizationsByIds(any());
  }

  @Test
  @DisplayName("findBranchNames should return empty map when input is empty")
  void findBranchNamesShouldReturnEmptyMapWhenInputIsEmpty() {
    Map<String, String> result = service.findBranchNames(ORGANIZATION_ID, Set.of());

    assertThat(result).isEmpty();
    verify(adapter, never()).findBranchesByIds(anyString(), any());
  }

  @Nested
  @DisplayName("findOrganizationNames")
  class FindOrganizationNames {

    @BeforeEach
    void setUp() {
      when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("should return names from cache when all ids are cached")
    void shouldReturnFromCacheWhenAllCached() {
      when(valueOperations.get(ORGANIZATION_CACHE_KEY)).thenReturn(ORGANIZATION_NAME);

      Map<String, String> result = service.findOrganizationNames(Set.of(ORGANIZATION_ID));

      assertThat(result).containsExactly(Map.entry(ORGANIZATION_ID, ORGANIZATION_NAME));
      verify(adapter, never()).findOrganizationsByIds(any());
    }

    @Test
    @DisplayName("should fetch from adapter and cache when all ids are cache misses")
    void shouldFetchAndCacheOnFullMiss() {
      when(valueOperations.get(ORGANIZATION_CACHE_KEY)).thenReturn(null);
      when(adapter.findOrganizationsByIds(Set.of(ORGANIZATION_ID)))
          .thenReturn(List.of(new Organization(ORGANIZATION_ID, ORGANIZATION_NAME)));
      when(cacheProperties.getTtlMinutes()).thenReturn(TTL_MINUTES);

      Map<String, String> result = service.findOrganizationNames(Set.of(ORGANIZATION_ID));

      assertThat(result).containsExactly(Map.entry(ORGANIZATION_ID, ORGANIZATION_NAME));
      verify(valueOperations)
          .set(ORGANIZATION_CACHE_KEY, ORGANIZATION_NAME, Duration.ofMinutes(TTL_MINUTES));
    }

    @Test
    @DisplayName("should fetch only missing ids from adapter when partially cached")
    void shouldFetchOnlyMissesWhenPartiallyCached() {
      String cachedOrgId = "org-cached";
      String cachedOrgName = "Cached Org";
      String missOrgId = "org-miss";
      String missOrgName = "Miss Org";

      when(valueOperations.get("organizations:organization:" + cachedOrgId))
          .thenReturn(cachedOrgName);
      when(valueOperations.get("organizations:organization:" + missOrgId)).thenReturn(null);
      when(adapter.findOrganizationsByIds(Set.of(missOrgId)))
          .thenReturn(List.of(new Organization(missOrgId, missOrgName)));
      when(cacheProperties.getTtlMinutes()).thenReturn(TTL_MINUTES);

      var ids = Set.of(cachedOrgId, missOrgId);
      Map<String, String> result = service.findOrganizationNames(ids);

      assertThat(result)
          .hasSize(2)
          .containsEntry(cachedOrgId, cachedOrgName)
          .containsEntry(missOrgId, missOrgName);
      verify(adapter).findOrganizationsByIds(Set.of(missOrgId));
    }
  }

  @Nested
  @DisplayName("findBranchNames")
  class FindBranchNames {

    @BeforeEach
    void setUp() {
      when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("should return names from cache when all ids are cached")
    void shouldReturnFromCacheWhenAllCached() {
      when(valueOperations.get(BRANCH_CACHE_KEY)).thenReturn(BRANCH_NAME);

      Map<String, String> result = service.findBranchNames(ORGANIZATION_ID, Set.of(BRANCH_ID));

      assertThat(result).containsExactly(Map.entry(BRANCH_ID, BRANCH_NAME));
      verify(adapter, never()).findBranchesByIds(anyString(), any());
    }

    @Test
    @DisplayName("should fetch from adapter and cache when all ids are cache misses")
    void shouldFetchAndCacheOnFullMiss() {
      when(valueOperations.get(BRANCH_CACHE_KEY)).thenReturn(null);
      when(adapter.findBranchesByIds(ORGANIZATION_ID, Set.of(BRANCH_ID)))
          .thenReturn(List.of(new Branch(BRANCH_ID, BRANCH_NAME)));
      when(cacheProperties.getTtlMinutes()).thenReturn(TTL_MINUTES);

      Map<String, String> result = service.findBranchNames(ORGANIZATION_ID, Set.of(BRANCH_ID));

      assertThat(result).containsExactly(Map.entry(BRANCH_ID, BRANCH_NAME));
      verify(valueOperations).set(BRANCH_CACHE_KEY, BRANCH_NAME, Duration.ofMinutes(TTL_MINUTES));
    }

    @Test
    @DisplayName("should fetch only missing ids from adapter when partially cached")
    void shouldFetchOnlyMissesWhenPartiallyCached() {
      String cachedBranchId = "branch-cached";
      String cachedBranchName = "Cached Branch";
      String missBranchId = "branch-miss";
      String missBranchName = "Miss Branch";

      when(valueOperations.get("organizations:branch:" + ORGANIZATION_ID + ":" + cachedBranchId))
          .thenReturn(cachedBranchName);
      when(valueOperations.get("organizations:branch:" + ORGANIZATION_ID + ":" + missBranchId))
          .thenReturn(null);
      when(adapter.findBranchesByIds(ORGANIZATION_ID, Set.of(missBranchId)))
          .thenReturn(List.of(new Branch(missBranchId, missBranchName)));
      when(cacheProperties.getTtlMinutes()).thenReturn(TTL_MINUTES);

      var ids = Set.of(cachedBranchId, missBranchId);
      Map<String, String> result = service.findBranchNames(ORGANIZATION_ID, ids);

      assertThat(result)
          .hasSize(2)
          .containsEntry(cachedBranchId, cachedBranchName)
          .containsEntry(missBranchId, missBranchName);
      verify(adapter).findBranchesByIds(ORGANIZATION_ID, Set.of(missBranchId));
    }
  }
}
