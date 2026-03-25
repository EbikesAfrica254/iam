package com.ebikes.iam.mappers;

import com.ebikes.iam.database.entities.Outbox;
import com.ebikes.iam.dtos.responses.outbox.OutboxResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OutboxMapper {

  OutboxResponse toResponse(Outbox outbox);
}
