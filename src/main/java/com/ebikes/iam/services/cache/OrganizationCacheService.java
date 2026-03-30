package com.ebikes.iam.services.cache;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.ebikes.iam.adapters.organizations.OrganizationServiceAdapter;
import com.ebikes.iam.configurations.properties.CacheProperties;
import com.ebikes.iam.dtos.adapters.organizations.Branch;
import com.ebikes.iam.dtos.adapters.organizations.Organization;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class OrganizationCacheService {

  private static final String BRANCH_KEY_PREFIX = "organizations:branch:";
  private static final String ORGANIZATIONS_KEY_PREFIX = "organizations:organization:";

  private final CacheProperties cacheProperties;
  private final OrganizationServiceAdapter adapter;
  private final RedisTemplate<String, String> redisTemplate;

  public Map<String, String> findOrganizationNames(Set<String> organizationIds) {
    Map<String, String> cached = fetchCachedOrganizations(organizationIds);

    Set<String> misses =
        organizationIds.stream().filter(id -> !cached.containsKey(id)).collect(Collectors.toSet());

    if (!misses.isEmpty()) {
      log.debug("Cache miss for {} organization(s)", misses.size());
      List<Organization> fetched = adapter.findOrganizationsByIds(misses);
      fetched.forEach(
          org -> {
            cached.put(org.id(), org.displayName());
            cacheOrganization(org);
          });
    }

    return cached;
  }

  public Map<String, String> findBranchNames(String organizationId, Set<String> branchIds) {
    Map<String, String> cached = fetchCachedBranches(organizationId, branchIds);

    Set<String> misses =
        branchIds.stream().filter(id -> !cached.containsKey(id)).collect(Collectors.toSet());

    if (!misses.isEmpty()) {
      log.debug("Cache miss for {} branch(es) in organizationId={}", misses.size(), organizationId);
      List<Branch> fetched = adapter.findBranchesByIds(organizationId, misses);
      fetched.forEach(
          branch -> {
            cached.put(branch.id(), branch.branchName());
            cacheBranch(organizationId, branch);
          });
    }

    return cached;
  }

  private Map<String, String> fetchCachedOrganizations(Set<String> organizationIds) {
    Map<String, String> result = new java.util.HashMap<>();
    organizationIds.forEach(
        id -> {
          String cached = redisTemplate.opsForValue().get(ORGANIZATIONS_KEY_PREFIX + id);
          if (cached != null) {
            result.put(id, cached);
          }
        });
    return result;
  }

  private Map<String, String> fetchCachedBranches(String organizationId, Set<String> branchIds) {
    Map<String, String> result = new java.util.HashMap<>();
    branchIds.forEach(
        id -> {
          String cached =
              redisTemplate.opsForValue().get(BRANCH_KEY_PREFIX + organizationId + ":" + id);
          if (cached != null) {
            result.put(id, cached);
          }
        });
    return result;
  }

  private void cacheOrganization(Organization org) {
    redisTemplate
        .opsForValue()
        .set(
            ORGANIZATIONS_KEY_PREFIX + org.id(),
            org.displayName(),
            Duration.ofMinutes(cacheProperties.getTtlMinutes()));
  }

  private void cacheBranch(String organizationId, Branch branch) {
    redisTemplate
        .opsForValue()
        .set(
            BRANCH_KEY_PREFIX + organizationId + ":" + branch.id(),
            branch.branchName(),
            Duration.ofMinutes(cacheProperties.getTtlMinutes()));
  }
}
