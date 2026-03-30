package com.ebikes.iam.support.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TokenHashUtilities")
class TokenHashUtilitiesTest {

  @Test
  @DisplayName("should return a 64-character lowercase hex string")
  void shouldReturnValid64CharHex() {
    String hash = TokenHashUtilities.hashToken("some-plain-token");

    assertThat(hash).hasSize(64).matches("[0-9a-f]+");
  }

  @Test
  @DisplayName("should return the same hash for the same input")
  void shouldBeDeterministic() {
    String token = "deterministic-token";

    assertThat(TokenHashUtilities.hashToken(token)).isEqualTo(TokenHashUtilities.hashToken(token));
  }
}
