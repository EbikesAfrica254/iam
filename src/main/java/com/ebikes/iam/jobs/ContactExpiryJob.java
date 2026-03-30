package com.ebikes.iam.jobs;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.iam.database.entities.Contact;
import com.ebikes.iam.database.repositories.ContactRepository;
import com.ebikes.iam.enums.ContactStatus;
import com.ebikes.iam.support.jobs.ScheduledJobDecorator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContactExpiryJob {

  private final ContactRepository contactRepository;

  @Scheduled(cron = "${contact.expiry-cron}")
  @Transactional
  public void expireStaleContacts() {
    ScheduledJobDecorator.decorate(
        () -> {
          OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

          List<Contact> stale =
              contactRepository.findAllByStatusAndExpiresAtBefore(ContactStatus.UNRESOLVED, now);

          if (stale.isEmpty()) {
            return;
          }

          stale.forEach(Contact::expire);
          contactRepository.saveAll(stale);
          log.info("contact-expiry: expired={}", stale.size());
        });
  }
}
