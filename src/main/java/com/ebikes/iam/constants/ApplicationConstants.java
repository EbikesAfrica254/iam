package com.ebikes.iam.constants;

public final class ApplicationConstants {
  public static final String CLASS_CANNOT_BE_INSTANTIATED = "Class cannot be instantiated";
  public static final String DOCUMENTATION_ERRORS_BASE = "https://docs.ebikesafrica.co.ke/errors/";
  public static final String ERROR_REFERENCE_PREFIX = "ERR";
  public static final int ERROR_REFERENCE_ID_LENGTH = 6;
  public static final String PROBLEM_JSON_MEDIA_TYPE = "application/problem+json";
  public static final String REQUEST_ID_HEADER = "X-Request-Id";
  public static final String SYSTEM_ID = "00000000-0000-0000-0000-000000000000";

  public static final class HttpClient {
    public static final int CONNECT_TIMEOUT_SECONDS = 10;
    public static final int READ_TIMEOUT_SECONDS = 15;

    private HttpClient() {
      throw new UnsupportedOperationException(CLASS_CANNOT_BE_INSTANTIATED);
    }
  }

  public static final class Keycloak {
    public static final String ACTIVE_BRANCH_ATTRIBUTE = "activeBranch";
    public static final String ACTIVE_ORGANIZATION_ATTRIBUTE = "activeOrganization";
    public static final String ACTIVE_ORGANIZATION_ROLES_ATTRIBUTE = "activeOrganizationRoles";
    public static final String PHONE_NUMBER_ATTRIBUTE = "phoneNumber";
    public static final String PHONE_NUMBER_VERIFIED_ATTRIBUTE = "phoneNumberVerified";

    private Keycloak() {
      throw new UnsupportedOperationException(CLASS_CANNOT_BE_INSTANTIATED);
    }
  }

  public static final class MessageHeaders {

    private MessageHeaders() {}

    public static final String EVENT_TYPE = "eventType";
    public static final String OUTBOX_ID = "outboxId";
    public static final String ROUTING_KEY = "routingKey";
  }

  public static final class Outbox {
    public static final String BINDING_NAME = "eventPublisher-out-0";
    public static final int MAX_RETRY_COUNT = 5;

    private Outbox() {
      throw new UnsupportedOperationException(CLASS_CANNOT_BE_INSTANTIATED);
    }
  }

  private ApplicationConstants() {
    throw new UnsupportedOperationException(CLASS_CANNOT_BE_INSTANTIATED);
  }
}
