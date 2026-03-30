package com.ebikes.iam.mappers;

import java.util.Set;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.ebikes.iam.database.entities.Membership;
import com.ebikes.iam.dtos.requests.memberships.CreateMembershipRequest;
import com.ebikes.iam.dtos.responses.memberships.MembershipResponse;
import com.ebikes.iam.enums.UserRole;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MembershipMapper {

  @Mapping(target = "branchId", source = "branchId")
  @Mapping(target = "isPrimary", source = "isPrimary")
  @Mapping(target = "organizationId", source = "organizationId")
  @Mapping(target = "roles", source = "roles")
  CreateMembershipRequest toRequest(
      String organizationId, String branchId, boolean isPrimary, Set<UserRole> roles);

  @Mapping(target = "userExtensionId", source = "membership.userExtension.id")
  @Mapping(target = "organizationName", source = "organizationName")
  @Mapping(target = "branchName", source = "branchName")
  MembershipResponse toResponse(Membership membership, String organizationName, String branchName);
}
