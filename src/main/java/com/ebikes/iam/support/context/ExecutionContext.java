package com.ebikes.iam.support.context;

import com.ebikes.iam.constants.ApplicationConstants;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Set;

@Slf4j
public final class ExecutionContext {

  private static final ThreadLocal<ContextData> context = new ThreadLocal<>();

  private ExecutionContext() {
    // prevent instantiation
  }

  public static void clear() {
    context.remove();
  }

  public static String getActiveOrganization() {
    ContextData data = context.get();
    return data != null ? data.activeOrganization() : null;
  }

  public static String getActiveBranch() {
    ContextData data = context.get();
    return data != null ? data.activeBranch() : null;
  }

  public static String getEmail() {
    ContextData data = context.get();
    return data != null ? data.email() : null;
  }

  public static String getPhoneNumber() {
    ContextData data = context.get();
    return data != null ? data.phoneNumber() : null;
  }

  public static Set<String> getRoles() {
    ContextData data = context.get();
    return data != null ? data.roles() : Collections.emptySet();
  }

  public static String getUserId() {
    ContextData data = context.get();
    if (data == null) {
      throw new IllegalStateException(
          "getUserId() called with no execution context on current thread");
    }
    return data.userId();
  }

  public static void set(
      String userId,
      String activeOrganization,
      String activeBranch,
      String email,
      Set<String> groups,
      String phoneNumber,
      Set<String> roles) {
    if (userId == null || userId.isBlank()) {
      log.warn("Attempted to set null or blank userId in ExecutionContext");
      throw new IllegalArgumentException("userId cannot be null or blank");
    }
    context.set(
        new ContextData(
            activeBranch,
            activeOrganization,
            email,
            groups != null ? groups : Collections.emptySet(),
            phoneNumber,
            roles != null ? roles : Collections.emptySet(),
            userId));
  }

  public static void setSystem() {
    context.set(
        new ContextData(
            null,
            null,
            null,
            Collections.emptySet(),
            null,
            Collections.emptySet(),
            ApplicationConstants.SYSTEM_ID));
  }

  public record ContextData(
      String activeBranch,
      String activeOrganization,
      String email,
      Set<String> groups,
      String phoneNumber,
      Set<String> roles,
      String userId) {

    public ContextData {
      groups = Set.copyOf(groups);
      roles = Set.copyOf(roles);
    }
  }
}
