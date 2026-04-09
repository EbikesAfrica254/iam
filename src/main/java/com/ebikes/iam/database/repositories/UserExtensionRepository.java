package com.ebikes.iam.database.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ebikes.iam.database.entities.UserExtension;
import com.ebikes.iam.dtos.responses.users.UserExtensionReference;

@Repository
public interface UserExtensionRepository
    extends JpaRepository<UserExtension, UUID>, JpaSpecificationExecutor<UserExtension> {

  Optional<UserExtension> findByEmail(String email);

  Optional<UserExtension> findByKeycloakUserId(String keycloakUserId);

  List<UserExtensionReference> findByKeycloakUserIdIn(List<String> keycloakUserIds);

  @EntityGraph(attributePaths = {"memberships"})
  @Query("SELECT ue FROM UserExtension ue WHERE ue.keycloakUserId = :keycloakUserId")
  Optional<UserExtension> findByKeycloakUserIdWithMemberships(
      @Param("keycloakUserId") String keycloakUserId);

  Optional<UserExtension> findByPhoneNumber(String phoneNumber);
}
