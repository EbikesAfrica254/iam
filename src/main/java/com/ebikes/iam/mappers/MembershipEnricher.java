package com.ebikes.iam.mappers;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.ebikes.iam.database.entities.Membership;
import com.ebikes.iam.dtos.responses.memberships.MembershipResponse;
import com.ebikes.iam.services.cache.OrganizationCacheService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MembershipEnricher {

  private final MembershipMapper membershipMapper;
  private final OrganizationCacheService organizationCacheService;

  public List<MembershipResponse> enrich(List<Membership> memberships) {
    if (memberships.isEmpty()) {
      return List.of();
    }

    Set<String> organizationIds =
        memberships.stream().map(Membership::getOrganizationId).collect(Collectors.toSet());

    Map<String, Set<String>> branchIdsByOrganization =
        memberships.stream()
            .filter(m -> m.getBranchId() != null)
            .collect(
                Collectors.groupingBy(
                    Membership::getOrganizationId,
                    Collectors.mapping(Membership::getBranchId, Collectors.toSet())));

    Map<String, String> organizationNames =
        organizationCacheService.findOrganizationNames(organizationIds);

    Map<String, String> branchNames =
        branchIdsByOrganization.entrySet().stream()
            .flatMap(
                entry ->
                    organizationCacheService
                        .findBranchNames(entry.getKey(), entry.getValue())
                        .entrySet()
                        .stream())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    return memberships.stream()
        .map(
            m ->
                membershipMapper.toResponse(
                    m,
                    organizationNames.get(m.getOrganizationId()),
                    m.getBranchId() != null ? branchNames.get(m.getBranchId()) : null))
        .toList();
  }
}
