package com.ebikes.iam.listeners;

public interface IncomingEventHandler {

  void handle(byte[] payload);

  boolean matches(String routingKey);
}
