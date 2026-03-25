package com.ebikes.iam.mappers;

import java.util.Set;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.ebikes.iam.database.entities.Membership;
import com.ebikes.iam.dtos.events.incoming.OrganizationApprovedAuditEvent;
import com.ebikes.iam.dtos.requests.memberships.CreateMembershipRequest;
import com.ebikes.iam.dtos.responses.memberships.MembershipResponse;
import com.ebikes.iam.enums.UserRole;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MembershipMapper {

  @Mapping(target = "branchId", ignore = true)
  @Mapping(target = "branchName", ignore = true)
  @Mapping(target = "isPrimary", source = "isPrimary")
  @Mapping(target = "organizationId", source = "event.organizationId")
  @Mapping(target = "organizationName", expression = "java(event.metadata().get(\"displayName\"))")
  @Mapping(target = "roles", source = "roles")
  CreateMembershipRequest toRequest(
      OrganizationApprovedAuditEvent event, boolean isPrimary, Set<UserRole> roles);

  @Mapping(target = "branchId", source = "branchId")
  @Mapping(target = "branchName", source = "branchName")
  @Mapping(target = "isPrimary", source = "isPrimary")
  @Mapping(target = "organizationId", source = "organizationId")
  @Mapping(target = "organizationName", source = "organizationName")
  @Mapping(target = "roles", source = "roles")
  CreateMembershipRequest toRequest(
      String organizationId,
      String organizationName,
      String branchId,
      String branchName,
      boolean isPrimary,
      Set<UserRole> roles);

  @Mapping(target = "userExtensionId", source = "membership.userExtension.id")
  MembershipResponse toResponse(Membership membership);
}
