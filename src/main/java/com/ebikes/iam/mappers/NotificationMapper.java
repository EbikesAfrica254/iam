package com.ebikes.iam.mappers;

import com.ebikes.iam.constants.EventConstants.EventTypes;
import com.ebikes.iam.constants.EventConstants.TemplateNames;
import com.ebikes.iam.database.entities.UserExtension;
import com.ebikes.iam.dtos.events.outgoing.NotificationRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.io.Serializable;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

  @Mapping(target = "branchId", source = "userExtension.branchId")
  @Mapping(target = "category", constant = "SECURITY")
  @Mapping(target = "channel", constant = "EMAIL")
  @Mapping(target = "eventType", constant = EventTypes.IAM.ACCOUNT_VERIFICATION_REQUESTED)
  @Mapping(target = "organizationId", source = "userExtension.organizationId")
  @Mapping(target = "recipient", source = "userExtension.email")
  @Mapping(target = "serviceReference", source = "serviceReference")
  @Mapping(target = "subjectUserId", source = "userExtension.keycloakUserId")
  @Mapping(target = "templateName", constant = TemplateNames.ACCOUNT_VERIFICATION)
  @Mapping(target = "variables", source = "variables")
  @Mapping(target = "timestamp", ignore = true)
  NotificationRequest toAccountVerificationRequest(
      UserExtension userExtension, Map<String, Serializable> variables, String serviceReference);

  @Mapping(target = "branchId", source = "userExtension.branchId")
  @Mapping(target = "category", constant = "SECURITY")
  @Mapping(target = "channel", constant = "EMAIL")
  @Mapping(target = "eventType", constant = EventTypes.IAM.EMAIL_VERIFICATION_REQUESTED)
  @Mapping(target = "organizationId", source = "userExtension.organizationId")
  @Mapping(target = "recipient", source = "userExtension.email")
  @Mapping(target = "serviceReference", source = "serviceReference")
  @Mapping(target = "subjectUserId", source = "userExtension.keycloakUserId")
  @Mapping(target = "templateName", constant = TemplateNames.EMAIL_VERIFICATION)
  @Mapping(target = "variables", source = "variables")
  @Mapping(target = "timestamp", ignore = true)
  NotificationRequest toEmailVerificationRequest(
      UserExtension userExtension, Map<String, Serializable> variables, String serviceReference);

  @Mapping(target = "branchId", source = "userExtension.branchId")
  @Mapping(target = "category", constant = "SECURITY")
  @Mapping(target = "channel", constant = "EMAIL")
  @Mapping(target = "eventType", constant = EventTypes.IAM.PASSWORD_RESET_REQUESTED)
  @Mapping(target = "organizationId", source = "userExtension.organizationId")
  @Mapping(target = "recipient", source = "userExtension.email")
  @Mapping(target = "serviceReference", source = "serviceReference")
  @Mapping(target = "subjectUserId", source = "userExtension.keycloakUserId")
  @Mapping(target = "templateName", constant = TemplateNames.PASSWORD_RESET)
  @Mapping(target = "variables", source = "variables")
  @Mapping(target = "timestamp", ignore = true)
  NotificationRequest toPasswordResetRequest(
      UserExtension userExtension, Map<String, Serializable> variables, String serviceReference);

  @Mapping(target = "branchId", source = "userExtension.branchId")
  @Mapping(target = "category", constant = "SECURITY")
  @Mapping(target = "channel", constant = "SMS")
  @Mapping(target = "eventType", constant = EventTypes.IAM.PHONE_VERIFICATION_REQUESTED)
  @Mapping(target = "organizationId", source = "userExtension.organizationId")
  @Mapping(target = "recipient", source = "userExtension.phoneNumber")
  @Mapping(target = "serviceReference", source = "serviceReference")
  @Mapping(target = "subjectUserId", source = "userExtension.keycloakUserId")
  @Mapping(target = "templateName", constant = TemplateNames.PHONE_VERIFICATION)
  @Mapping(target = "variables", source = "variables")
  @Mapping(target = "timestamp", ignore = true)
  NotificationRequest toPhoneVerificationRequest(
      UserExtension userExtension, Map<String, Serializable> variables, String serviceReference);
}
