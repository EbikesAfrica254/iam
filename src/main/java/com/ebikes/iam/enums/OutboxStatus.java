package com.ebikes.iam.enums;

public enum OutboxStatus {
  DEAD_LETTER,
  FAILED,
  PENDING,
  SENT
}
