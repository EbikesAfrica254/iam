package com.ebikes.iam.support.events;

import com.ebikes.iam.constants.EventConstants.EventSource;
import com.ebikes.iam.constants.EventConstants.MessageHeaders;
import com.ebikes.iam.support.context.EventContext;
import com.ebikes.iam.support.context.ExecutionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;

@Slf4j
public class EventContextDecorator {

    private EventContextDecorator() {
        // prevent instantiation
    }

    public static void decorate(Message<?> message, Runnable handler) {
        try {
            ExecutionContext.setSystem();
            EventContext.set(
                    extractHeader(message, MessageHeaders.OUTBOX_ID),
                    extractHeader(message, MessageHeaders.EVENT_TYPE),
                    extractHeader(message, MessageHeaders.ROUTING_KEY),
                    EventSource.HOST_SERVICE);

            handler.run();

        } finally {
            ExecutionContext.clear();
            EventContext.clear();
        }
    }

    private static String extractHeader(Message<?> message, String headerName) {
        Object value = message.getHeaders().get(headerName);
        return value instanceof String s ? s : null;
    }
}
