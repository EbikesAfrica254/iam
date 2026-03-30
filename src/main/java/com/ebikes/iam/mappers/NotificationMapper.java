package com.ebikes.iam.mappers;

import java.io.Serializable;
import java.util.Map;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.ebikes.iam.constants.EventConstants.DomainEvents;
import com.ebikes.iam.database.entities.UserExtension;
import com.ebikes.iam.dtos.events.outgoing.NotificationRequestEvent;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

  @Mapping(target = "branchId", source = "userExtension.branchId")
  @Mapping(target = "category", constant = "SECURITY")
  @Mapping(target = "channel", constant = "EMAIL")
  @Mapping(target = "eventType", constant = DomainEvents.UserExtension.ACTIVATION_REQUESTED)
  @Mapping(target = "organizationId", source = "userExtension.organizationId")
  @Mapping(target = "recipient", source = "userExtension.email")
  @Mapping(target = "serviceReference", source = "serviceReference")
  @Mapping(target = "subjectUserId", source = "userExtension.keycloakUserId")
  @Mapping(target = "templateName", constant = TemplateNames.ACCOUNT_VERIFICATION)
  @Mapping(target = "variables", source = "variables")
  @Mapping(target = "timestamp", ignore = true)
  NotificationRequestEvent toAccountVerificationRequest(
      UserExtension userExtension, Map<String, Serializable> variables, String serviceReference);

  @Mapping(target = "branchId", source = "userExtension.branchId")
  @Mapping(target = "category", constant = "SECURITY")
  @Mapping(target = "channel", constant = "EMAIL")
  @Mapping(target = "eventType", constant = DomainEvents.UserExtension.EMAIL_VERIFICATION_REQUESTED)
  @Mapping(target = "organizationId", source = "userExtension.organizationId")
  @Mapping(target = "recipient", source = "userExtension.email")
  @Mapping(target = "serviceReference", source = "serviceReference")
  @Mapping(target = "subjectUserId", source = "userExtension.keycloakUserId")
  @Mapping(target = "templateName", constant = TemplateNames.EMAIL_VERIFICATION)
  @Mapping(target = "variables", source = "variables")
  @Mapping(target = "timestamp", ignore = true)
  NotificationRequestEvent toEmailVerificationRequest(
      UserExtension userExtension, Map<String, Serializable> variables, String serviceReference);

  @Mapping(target = "branchId", source = "userExtension.branchId")
  @Mapping(target = "category", constant = "SECURITY")
  @Mapping(target = "channel", constant = "EMAIL")
  @Mapping(target = "eventType", constant = DomainEvents.UserExtension.PASSWORD_RESET_REQUESTED)
  @Mapping(target = "organizationId", source = "userExtension.organizationId")
  @Mapping(target = "recipient", source = "userExtension.email")
  @Mapping(target = "serviceReference", source = "serviceReference")
  @Mapping(target = "subjectUserId", source = "userExtension.keycloakUserId")
  @Mapping(target = "templateName", constant = TemplateNames.PASSWORD_RESET)
  @Mapping(target = "variables", source = "variables")
  @Mapping(target = "timestamp", ignore = true)
  NotificationRequestEvent toPasswordResetRequest(
      UserExtension userExtension, Map<String, Serializable> variables, String serviceReference);

  @Mapping(target = "branchId", source = "userExtension.branchId")
  @Mapping(target = "category", constant = "SECURITY")
  @Mapping(target = "channel", constant = "SMS")
  @Mapping(target = "eventType", constant = DomainEvents.UserExtension.PHONE_VERIFICATION_REQUESTED)
  @Mapping(target = "organizationId", source = "userExtension.organizationId")
  @Mapping(target = "recipient", source = "userExtension.phoneNumber")
  @Mapping(target = "serviceReference", source = "serviceReference")
  @Mapping(target = "subjectUserId", source = "userExtension.keycloakUserId")
  @Mapping(target = "templateName", constant = TemplateNames.PHONE_VERIFICATION)
  @Mapping(target = "variables", source = "variables")
  @Mapping(target = "timestamp", ignore = true)
  NotificationRequestEvent toPhoneVerificationRequest(
      UserExtension userExtension, Map<String, Serializable> variables, String serviceReference);

  final class TemplateNames {

    private TemplateNames() {}

    static final String ACCOUNT_VERIFICATION = "ACCOUNT_VERIFICATION";
    static final String EMAIL_VERIFICATION = "EMAIL_VERIFICATION";
    static final String PASSWORD_RESET = "PASSWORD_RESET";
    static final String PHONE_VERIFICATION = "PHONE_VERIFICATION";
  }
}
