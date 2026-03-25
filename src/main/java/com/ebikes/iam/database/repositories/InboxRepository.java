package com.ebikes.iam.database.repositories;

import com.ebikes.iam.database.entities.Inbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InboxRepository extends JpaRepository<Inbox, String> {
}
