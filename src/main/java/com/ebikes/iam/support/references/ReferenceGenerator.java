package com.ebikes.iam.support.references;

import com.ebikes.iam.constants.ApplicationConstants;
import org.apache.commons.lang3.RandomStringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class ReferenceGenerator {

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

  private ReferenceGenerator() {
    // prevent instantiation
  }

  public static String generateErrorReference() {
    return generate();
  }

  public static String generateServiceReference(String serviceName) {
    return serviceName + ":" + UUID.randomUUID();
  }

  private static String generate() {
    String datePart = LocalDate.now().format(DATE_FORMATTER);
    String randomPart =
        RandomStringUtils.insecure()
            .nextAlphanumeric(ApplicationConstants.ERROR_REFERENCE_ID_LENGTH)
            .toUpperCase();

    return ApplicationConstants.ERROR_REFERENCE_PREFIX + "-" + datePart + "-" + randomPart;
  }
}
