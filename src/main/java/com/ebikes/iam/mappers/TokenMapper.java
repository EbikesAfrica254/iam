package com.ebikes.iam.mappers;

import com.ebikes.iam.database.entities.Token;
import com.ebikes.iam.enums.TokenType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.time.OffsetDateTime;
import java.util.UUID;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = org.mapstruct.NullValuePropertyMappingStrategy.IGNORE)
public interface TokenMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Token toEntity(
            UUID userExtensionId, TokenType tokenType, String tokenHash, OffsetDateTime expiresAt);
}
