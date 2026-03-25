package com.ebikes.iam.configurations;

import com.ebikes.iam.listeners.IncomingEventListener;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Configuration
@RequiredArgsConstructor
public class EventConsumerConfiguration {

  private final IncomingEventListener incomingEventListener;

  @Bean
  public Consumer<Message<?>> incomingEventConsumer() {
    return incomingEventListener::route;
  }
}
