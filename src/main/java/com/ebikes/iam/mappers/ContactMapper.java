package com.ebikes.iam.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.ebikes.iam.database.entities.Contact;
import com.ebikes.iam.dtos.responses.contacts.ContactResponse;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ContactMapper {

  @Mapping(target = "userExtensionId", source = "userExtension.id")
  ContactResponse toResponse(Contact contact);
}
