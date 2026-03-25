package com.ebikes.iam.publishers;

import com.ebikes.iam.constants.EventConstants.RoutingKeys;
import com.ebikes.iam.dtos.events.outgoing.NotificationRequest;
import com.ebikes.iam.enums.ChannelType;
import com.ebikes.iam.services.events.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventPublisher {

    private final OutboxService outboxService;

    public void publish(NotificationRequest request) {
        String routingKey = resolveRoutingKey(request.channel());

        outboxService.save(request.eventType(), request, routingKey);

        log.info(
                "Notification event queued - eventType={} channel={} recipient={}",
                request.eventType(),
                request.channel(),
                request.recipient());
    }

    private String resolveRoutingKey(ChannelType channel) {
        return switch (channel) {
            case EMAIL -> RoutingKeys.NOTIFICATIONS_EMAIL;
            case SMS -> RoutingKeys.NOTIFICATIONS_SMS;
            case SSE -> RoutingKeys.NOTIFICATIONS_SSE;
            case WHATSAPP -> RoutingKeys.NOTIFICATIONS_WHATSAPP;
        };
    }
}
