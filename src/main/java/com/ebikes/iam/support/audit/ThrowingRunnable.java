package com.ebikes.iam.support.audit;

@FunctionalInterface
public interface ThrowingRunnable<E extends Exception> {
  void run() throws E;
}
