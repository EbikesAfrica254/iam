package com.ebikes.iam.support.fixtures;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.UUID;

import com.ebikes.iam.database.entities.Token;
import com.ebikes.iam.enums.TokenType;

public final class TokenFixtures {

  private TokenFixtures() {}

  public static Token active(UUID userExtensionId, TokenType type) {
    return base(userExtensionId, type)
        .expiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusHours(24))
        .build();
  }

  public static Token expired(UUID userExtensionId, TokenType type) {
    return base(userExtensionId, type)
        .expiresAt(OffsetDateTime.now(ZoneOffset.UTC).minusHours(1))
        .build();
  }

  public static Token consumed(UUID userExtensionId, TokenType type) {
    Token token = active(userExtensionId, type);
    token.consume();
    return token;
  }

  private static Token.TokenBuilder<?, ?> base(UUID userExtensionId, TokenType type) {
    return Token.builder()
        .userExtensionId(userExtensionId)
        .tokenType(type)
        .tokenHash(randomHash())
        .consumed(false)
        .version(0L);
  }

  private static String randomHash() {
    byte[] bytes = UUID.randomUUID().toString().replace("-", "").getBytes();
    return HexFormat.of().formatHex(bytes).substring(0, 64);
  }
}
