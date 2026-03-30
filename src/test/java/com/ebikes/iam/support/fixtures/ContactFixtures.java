package com.ebikes.iam.support.fixtures;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import com.ebikes.iam.database.entities.Contact;
import com.ebikes.iam.database.entities.UserExtension;
import com.ebikes.iam.enums.ContactSourceType;
import com.ebikes.iam.enums.ContactStatus;

import net.datafaker.Faker;

public final class ContactFixtures {

  private static final Faker FAKER = new Faker();

  private ContactFixtures() {}

  public static Contact unresolved(String organizationId) {
    return base(organizationId).build();
  }

  public static Contact claimed(String organizationId, UserExtension userExtension) {
    Contact contact = base(organizationId).build();
    contact.claim(userExtension);
    return contact;
  }

  public static Contact expired(String organizationId) {
    Contact contact = base(organizationId).build();
    contact.expire();
    return contact;
  }

  public static Contact withBranch(String organizationId, String branchId) {
    return base(organizationId).branchId(branchId).build();
  }

  public static Contact withSourceType(String organizationId, ContactSourceType sourceType) {
    return base(organizationId).sourceType(sourceType).build();
  }

  private static Contact.ContactBuilder<?, ?> base(String organizationId) {
    return Contact.builder()
        .organizationId(organizationId)
        .phoneNumber("+254" + FAKER.number().digits(9))
        .sourceReference(UUID.randomUUID().toString())
        .sourceType(ContactSourceType.CSV_MANIFEST)
        .status(ContactStatus.UNRESOLVED)
        .expiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusDays(30));
  }
}
