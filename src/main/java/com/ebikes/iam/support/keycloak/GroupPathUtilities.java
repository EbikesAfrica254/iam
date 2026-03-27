package com.ebikes.iam.support.keycloak;

import java.nio.charset.StandardCharsets;

import org.springframework.util.DigestUtils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class GroupPathUtilities {

  public static String generate(String organizationId) {
    String suffix = generateDeterministicSuffix(organizationId);
    return "/" + suffix;
  }

  private String generateDeterministicSuffix(String organizationId) {
    String hash = DigestUtils.md5DigestAsHex(organizationId.getBytes(StandardCharsets.UTF_8));
    return hash.substring(0, 8);
  }
}
