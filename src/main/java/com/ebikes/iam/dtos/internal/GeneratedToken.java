package com.ebikes.iam.dtos.internal;

import com.ebikes.iam.database.entities.Token;

import java.time.OffsetDateTime;

public record GeneratedToken(OffsetDateTime expiresAt, String plainToken, Token token) {}
