package com.ebikes.iam.support.fixtures;

import java.util.Map;
import java.util.UUID;

import com.ebikes.iam.constants.EventConstants.DomainEvents;
import com.ebikes.iam.constants.EventConstants.Source;
import com.ebikes.iam.dtos.events.outgoing.NotificationRequestEvent;
import com.ebikes.iam.enums.ChannelType;
import com.ebikes.iam.enums.NotificationCategory;

import net.datafaker.Faker;

public final class NotificationRequestFixtures {

  private static final Faker FAKER = new Faker();

  private NotificationRequestFixtures() {}

  public static NotificationRequestEvent accountVerification() {
    return base(
        ChannelType.EMAIL,
        DomainEvents.UserExtension.ACTIVATION_REQUESTED,
        "ACCOUNT_VERIFICATION",
        Map.of("name", FAKER.name().firstName()));
  }

  public static NotificationRequestEvent emailVerification() {
    return base(
        ChannelType.EMAIL,
        DomainEvents.UserExtension.EMAIL_VERIFICATION_REQUESTED,
        "EMAIL_VERIFICATION",
        Map.of("name", FAKER.name().firstName(), "link", FAKER.internet().url()));
  }

  public static NotificationRequestEvent passwordReset() {
    return base(
        ChannelType.EMAIL,
        DomainEvents.UserExtension.PASSWORD_RESET_REQUESTED,
        "PASSWORD_RESET",
        Map.of("name", FAKER.name().firstName(), "link", FAKER.internet().url()));
  }

  public static NotificationRequestEvent phoneVerification() {
    return base(
        ChannelType.SMS,
        DomainEvents.UserExtension.PHONE_VERIFICATION_REQUESTED,
        "PHONE_VERIFICATION",
        Map.of("code", FAKER.number().digits(6)));
  }

  private static NotificationRequestEvent base(
      ChannelType channel, String eventType, String templateName, Map<String, Object> variables) {
    return new NotificationRequestEvent(
        null,
        NotificationCategory.SECURITY,
        channel,
        eventType,
        UUID.randomUUID().toString(),
        FAKER.internet().emailAddress(),
        Source.serviceReference(),
        UUID.randomUUID().toString(),
        templateName,
        null,
        variables);
  }
}
