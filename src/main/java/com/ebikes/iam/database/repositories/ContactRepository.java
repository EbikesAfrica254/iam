package com.ebikes.iam.database.repositories;

import com.ebikes.iam.database.entities.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ContactRepository
    extends JpaRepository<Contact, UUID>, JpaSpecificationExecutor<Contact> {

  @Modifying
  @Query(
      """
      UPDATE Contact c
      SET c.expiresAt = :newExpiresAt,
          c.version = c.version + 1
      WHERE c.id IN :ids
      AND c.status = 'UNRESOLVED'
      """)
  int bulkUpdateExpiresAt(
      @Param("ids") List<UUID> ids, @Param("newExpiresAt") OffsetDateTime newExpiresAt);

  List<Contact> findAllByPhoneNumberInAndOrganizationId(
      List<String> phoneNumbers, String organizationId);

}
