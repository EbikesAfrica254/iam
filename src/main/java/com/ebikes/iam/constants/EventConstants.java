package com.ebikes.iam.constants;

import com.ebikes.iam.support.references.ReferenceGenerator;

public final class EventConstants {

  private EventConstants() {}

  public static final class Source {

    private Source() {}

    public static final String IAM = "iam";
    public static final String ORGANIZATIONS = "organizations";
    public static final String ORDERS = "orders";

    public static String serviceReference() {
      return ReferenceGenerator.generateServiceReference(IAM);
    }
  }

  public static final class DomainEvents {

    private DomainEvents() {}

    public static final class UserExtension {

      private UserExtension() {}

      public static final String ACTIVATED = Source.IAM + ".user-extension.activated";
      public static final String ACTIVATION_REQUESTED =
          Source.IAM + ".user-extension.activation-requested";
      public static final String CREATED = Source.IAM + ".user-extension.created";
      public static final String DELETED = Source.IAM + ".user-extension.deleted";
      public static final String DEPROVISIONED = Source.IAM + ".user-extension.deprovisioned";
      public static final String EMAIL_VERIFICATION_REQUESTED =
          Source.IAM + ".user-extension.email.verification-requested";
      public static final String EMAIL_VERIFIED = Source.IAM + ".user-extension.email.verified";
      public static final String PASSWORD_RESET = Source.IAM + ".user-extension.password.updated";
      public static final String PASSWORD_RESET_REQUESTED =
          Source.IAM + ".user-extension.password.requested";
      public static final String PHONE_VERIFICATION_REQUESTED =
          Source.IAM + ".user-extension.phone.verification-requested";
      public static final String PHONE_VERIFIED = Source.IAM + ".user-extension.phone.verified";
      public static final String RESTORED = Source.IAM + ".user-extension.restored";
      public static final String UPDATED = Source.IAM + ".user-extension.updated";
    }

    public static final class Membership {

      private Membership() {}

      public static final String CREATED = Source.IAM + ".membership.created";
      public static final String ORGANIZATION_REMOVED =
          Source.IAM + ".membership.organization-removed";
      public static final String PRIMARY_CHANGED = Source.IAM + ".membership.primary-changed";
      public static final String REMOVED = Source.IAM + ".membership.removed";
      public static final String UPDATED = Source.IAM + ".membership.updated";
    }

    public static final class Contact {

      private Contact() {}

      public static final String CLAIMED = Source.IAM + ".contact.claimed";
      public static final String CREATED = Source.IAM + ".contact.created";
    }

    public static final class Context {

      private Context() {}

      public static final String SWITCHED = Source.IAM + ".context.switched";
    }

    public static final class Configuration {

      private Configuration() {}

      public static final String REQUESTED = Source.IAM + ".user-extension.requested";
    }
  }

  public static final class AuditEvents {

    private AuditEvents() {}

    public static final String CONTACT = Source.IAM + ".contact.audit";
    public static final String CONTEXT = Source.IAM + ".context.audit";
    public static final String MEMBERSHIP = Source.IAM + ".membership.audit";
    public static final String USER_EXTENSION = Source.IAM + ".user-extension.audit";
  }

  public static final class RoutingKeys {

    private RoutingKeys() {}

    public static final String NOTIFICATIONS_EMAIL = "notifications.email";
    public static final String NOTIFICATIONS_SMS = "notifications.sms";
    public static final String NOTIFICATIONS_SSE = "notifications.sse";
    public static final String NOTIFICATIONS_WHATSAPP = "notifications.whatsapp";

    public static final String IAM_CONTACT_CONFIGURATION = Source.IAM + ".contact.configuration";
    public static final String IAM_USER_CONFIGURATION =
        Source.IAM + ".user-extension.configuration";
  }

  public static final class ExternalContracts {

    private ExternalContracts() {}

    public static final String ORDERS_MANIFEST_CONTACTS = Source.ORDERS + ".manifest.contacts";
    public static final String ORGANIZATIONS_ORGANIZATION_CREATED =
        Source.ORGANIZATIONS + ".organization.created";
  }
}
