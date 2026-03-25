package com.ebikes.iam.constants;

import com.ebikes.iam.support.references.ReferenceGenerator;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EventConstants {

  public static final class EventSource {

    private EventSource() {
      // prevent instantiation
    }

    public static final String HOST_SERVICE = "iam";

    public static String serviceReference() {
      return ReferenceGenerator.generateServiceReference(HOST_SERVICE);
    }
  }

  public static final class EventTypes {

    private EventTypes() {
      // prevent instantiation
    }

    public static final class IAM {

      private IAM() {
        // prevent instantiation
      }

      public static final String ACCOUNT_DELETED = EventSource.HOST_SERVICE + ".account.deleted";
      public static final String ACCOUNT_RESTORED = EventSource.HOST_SERVICE + ".account.restored";
      public static final String ACCOUNT_STATUS_CHANGED =
          EventSource.HOST_SERVICE + ".account.status-changed";
      public static final String ACCOUNT_UPDATED = EventSource.HOST_SERVICE + ".account.updated";
      public static final String ACCOUNT_VERIFICATION_COMPLETED =
          EventSource.HOST_SERVICE + ".account.verification-completed";
      public static final String ACCOUNT_VERIFICATION_REQUESTED =
          EventSource.HOST_SERVICE + ".account.verification-requested";
      public static final String CONTACT_CLAIMED = EventSource.HOST_SERVICE + ".contact.claimed";
      public static final String CONTACT_CREATED = EventSource.HOST_SERVICE + ".contact.created";
      public static final String CONTEXT_SWITCHED = EventSource.HOST_SERVICE + ".context.switched";
      public static final String EMAIL_VERIFICATION_COMPLETED =
          EventSource.HOST_SERVICE + ".email.verification-completed";
      public static final String EMAIL_VERIFICATION_REQUESTED =
          EventSource.HOST_SERVICE + ".email.verification-requested";
      public static final String MEMBERSHIP_CREATED =
          EventSource.HOST_SERVICE + ".membership.created";
      public static final String MEMBERSHIP_ORGANIZATION_REMOVED =
          EventSource.HOST_SERVICE + ".membership.organization-removed";
      public static final String MEMBERSHIP_PRIMARY_CHANGED =
          EventSource.HOST_SERVICE + ".membership.primary-changed";
      public static final String MEMBERSHIP_REMOVED =
          EventSource.HOST_SERVICE + ".membership.removed";
      public static final String MEMBERSHIP_ROLES_UPDATED =
          EventSource.HOST_SERVICE + ".membership.roles-updated";
      public static final String PASSWORD_RESET_COMPLETED =
          EventSource.HOST_SERVICE + ".password.reset-completed";
      public static final String PASSWORD_RESET_REQUESTED =
          EventSource.HOST_SERVICE + ".password.reset-requested";
      public static final String PHONE_VERIFICATION_COMPLETED =
          EventSource.HOST_SERVICE + ".phone.verification-completed";
      public static final String PHONE_VERIFICATION_REQUESTED =
          EventSource.HOST_SERVICE + ".phone.verification-requested";
      public static final String USER_DEPROVISIONED =
          EventSource.HOST_SERVICE + ".user.deprovisioned";
      public static final String USER_PROVISIONED = EventSource.HOST_SERVICE + ".user.provisioned";
    }
  }

  public static final class MessageHeaders {

    private MessageHeaders() {
      // prevent instantiation
    }

    public static final String EVENT_TYPE = "eventType";
    public static final String OUTBOX_ID = "outboxId";
    public static final String ROUTING_KEY = "routingKey";
  }

  public static final class RoutingKeys {

    private RoutingKeys() {
      // prevent instantiation
    }

    // outbound routing keys — pattern: <service>.<domain>.audit → matches *.*.audit
    public static final String IAM_ACCOUNT_AUDIT = audit(EventSource.HOST_SERVICE + ".account");
    public static final String IAM_CONTEXT_AUDIT = audit(EventSource.HOST_SERVICE + ".context");
    public static final String IAM_EMAIL_AUDIT = audit(EventSource.HOST_SERVICE + ".email");
    public static final String IAM_MEMBERSHIP_AUDIT =
        audit(EventSource.HOST_SERVICE + ".membership");
    public static final String IAM_PASSWORD_AUDIT = audit(EventSource.HOST_SERVICE + ".password");
    public static final String IAM_PHONE_AUDIT = audit(EventSource.HOST_SERVICE + ".phone");
    public static final String IAM_USER_AUDIT = audit(EventSource.HOST_SERVICE + ".user");

    // configuration routing keys — pattern: <service>.<domain>.configuration
    public static final String IAM_CONTACT_CONFIGURATION =
        configuration(EventSource.HOST_SERVICE + ".contact");
    public static final String IAM_USER_CONFIGURATION =
        configuration(EventSource.HOST_SERVICE + ".user");

    // inbound routing keys — external contracts, hardcoded intentionally
    public static final String ORDERS_MANIFEST_CONTACTS = "orders.manifest.contacts";
    public static final String ORGANIZATIONS_APPROVED = "organizations.organization.audit";

    // outbound notification routing keys
    public static final String NOTIFICATIONS_EMAIL = notifications("email");
    public static final String NOTIFICATIONS_SMS = notifications("sms");
    public static final String NOTIFICATIONS_SSE = notifications("sse");
    public static final String NOTIFICATIONS_WHATSAPP = notifications("whatsapp");

    public static String audit(String domain) {
      return domain + ".audit";
    }

    public static String configuration(String domain) {
      return domain + ".configuration";
    }

    public static String makerCheckerRequest(String sourceService, String entityType) {
      return sourceService
          + "."
          + entityType.toLowerCase().replace("_", "-")
          + ".maker-checker-request";
    }

    public static String notifications(String channel) {
      return "notifications." + channel.toLowerCase();
    }
  }

  public static final class TemplateNames {

    private TemplateNames() {
      // prevent instantiation
    }

    public static final String ACCOUNT_VERIFICATION = "ACCOUNT_VERIFICATION";
    public static final String EMAIL_VERIFICATION = "EMAIL_VERIFICATION";
    public static final String PASSWORD_RESET = "PASSWORD_RESET";
    public static final String PHONE_VERIFICATION = "PHONE_VERIFICATION";
  }
}
