package com.ebikes.iam.dtos.internal;

import java.time.OffsetDateTime;

import com.ebikes.iam.database.entities.Token;

public record GeneratedToken(OffsetDateTime expiresAt, String plainToken, Token token) {}
