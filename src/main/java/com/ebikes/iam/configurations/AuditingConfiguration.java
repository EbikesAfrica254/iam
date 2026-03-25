package com.ebikes.iam.configurations;

import com.ebikes.iam.support.context.ExecutionContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.OffsetDateTime;
import java.util.Optional;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "offsetDateTimeProvider")
public class AuditingConfiguration {

    @Bean
    public AuditorAware<String> auditorAware() {
        return AuditingConfiguration::tryGetUserId;
    }

    @Bean
    public DateTimeProvider offsetDateTimeProvider() {
        return () -> Optional.of(OffsetDateTime.now());
    }

    private static Optional<String> tryGetUserId() {
        try {
            return Optional.of(ExecutionContext.getUserId());
        } catch (IllegalStateException e) {
            return Optional.empty();
        }
    }
}
