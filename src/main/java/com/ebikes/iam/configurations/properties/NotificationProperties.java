package com.ebikes.iam.configurations.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "notifications")
@Component
@Data
@Validated
public class NotificationProperties {

    private boolean enabled = true;
}
