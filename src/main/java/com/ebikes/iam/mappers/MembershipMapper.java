package com.ebikes.iam.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.ebikes.iam.database.entities.Membership;
import com.ebikes.iam.dtos.responses.memberships.MembershipResponse;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MembershipMapper {

  @Mapping(target = "userExtensionId", source = "membership.userExtension.id")
  @Mapping(target = "organizationName", source = "organizationName")
  @Mapping(target = "branchName", source = "branchName")
  MembershipResponse toResponse(Membership membership, String organizationName, String branchName);
}
