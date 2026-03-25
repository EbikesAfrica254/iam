package com.ebikes.iam.mappers;

import com.ebikes.iam.database.entities.Membership;
import com.ebikes.iam.database.entities.UserExtension;
import com.ebikes.iam.dtos.responses.users.UserExtensionDetailResponse;
import com.ebikes.iam.dtos.responses.users.UserExtensionSummaryResponse;
import com.ebikes.iam.dtos.responses.users.UserProfileResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    uses = {MembershipMapper.class})
public interface UserExtensionMapper {

  UserExtensionDetailResponse toDetailResponse(UserExtension userExtension);

  UserExtensionSummaryResponse toSummaryResponse(UserExtension userExtension);

  @Mapping(target = "activeMembership", source = "activeMembership")
  @Mapping(target = "createdAt", source = "userExtension.createdAt")
  @Mapping(target = "id", source = "userExtension.id")
  @Mapping(target = "memberships", source = "memberships")
  UserProfileResponse toProfileResponse(
      UserExtension userExtension, Membership activeMembership, List<Membership> memberships);
}
