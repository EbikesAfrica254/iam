package com.ebikes.iam.jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.iam.database.entities.Contact;
import com.ebikes.iam.database.repositories.ContactRepository;
import com.ebikes.iam.enums.ContactStatus;
import com.ebikes.iam.support.context.ExecutionContext;
import com.ebikes.iam.support.fixtures.ContactFixtures;

@ExtendWith(MockitoExtension.class)
class ContactExpiryJobTest {

  private static final String TEST_ORGANIZATION_ID = "00000000-0000-0000-0000-000000000001";

  @Mock private ContactRepository contactRepository;
  @InjectMocks private ContactExpiryJob contactExpiryJob;

  @Nested
  class ExpireStaleContacts {

    @Test
    @DisplayName("expires all stale contacts")
    void expiresAllStaleContacts() {
      List<Contact> stale =
          List.of(
              ContactFixtures.unresolved(TEST_ORGANIZATION_ID),
              ContactFixtures.unresolved(TEST_ORGANIZATION_ID));

      when(contactRepository.findAllByStatusAndExpiresAtBefore(any(), any())).thenReturn(stale);

      contactExpiryJob.expireStaleContacts();

      assertThat(stale).allMatch(c -> c.getStatus() == ContactStatus.EXPIRED);
      verify(contactRepository).saveAll(stale);
    }

    @Test
    @DisplayName("does nothing when no stale contacts exist")
    void doesNothingWhenNoStaleContactsExist() {
      when(contactRepository.findAllByStatusAndExpiresAtBefore(any(), any())).thenReturn(List.of());

      contactExpiryJob.expireStaleContacts();

      verify(contactRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("clears execution context after run")
    void clearsExecutionContextAfterRun() {
      when(contactRepository.findAllByStatusAndExpiresAtBefore(any(), any())).thenReturn(List.of());

      contactExpiryJob.expireStaleContacts();

      assertThatThrownBy(ExecutionContext::get).isInstanceOf(IllegalStateException.class);
    }
  }
}
