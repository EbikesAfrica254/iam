package com.ebikes.iam.support.references;

import com.ebikes.iam.constants.ApplicationConstants;
import com.ebikes.iam.enums.ChannelType;
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
        return generate(
                ApplicationConstants.ERROR_REFERENCE_PREFIX,
                null,
                ApplicationConstants.ERROR_REFERENCE_ID_LENGTH);
    }

    public static String generateMessageReference(ChannelType channel) {
        return generate(
                ApplicationConstants.MESSAGE_REFERENCE_PREFIX,
                channel.name(),
                ApplicationConstants.MESSAGE_REFERENCE_ID_LENGTH);
    }

    public static String generateServiceReference(String serviceName) {
        return serviceName + ":" + UUID.randomUUID();
    }

    private static String generate(String prefix, String qualifier, int length) {
        String datePart = LocalDate.now().format(DATE_FORMATTER);
        String randomPart = RandomStringUtils.insecure().nextAlphanumeric(length).toUpperCase();

        return qualifier != null
                ? prefix + "-" + qualifier + "-" + datePart + "-" + randomPart
                : prefix + "-" + datePart + "-" + randomPart;
    }
}
