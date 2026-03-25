package com.ebikes.iam.mappers;

import org.mapstruct.Mapper;

import com.ebikes.iam.database.entities.Outbox;
import com.ebikes.iam.dtos.responses.outbox.OutboxResponse;

@Mapper(componentModel = "spring")
public interface OutboxMapper {

  OutboxResponse toResponse(Outbox outbox);
}
