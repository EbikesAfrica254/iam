package com.ebikes.iam.database.repositories;

import com.ebikes.iam.database.entities.Membership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, UUID> {

  boolean existsByBranchIdAndKeycloakUserIdAndOrganizationId(
      String branchId, String keycloakUserId, String organizationId);

  boolean existsByKeycloakUserIdAndOrganizationIdAndBranchIdIsNull(
      String keycloakUserId, String organizationId);

  List<Membership> findAllByKeycloakUserIdAndOrganizationId(
      String keycloakUserId, String organizationId);

  List<Membership> findByKeycloakUserId(String keycloakUserId);

  List<Membership> findByIsPrimaryAndKeycloakUserId(Boolean isPrimary, String keycloakUserId);

  Optional<Membership> findByKeycloakUserIdAndOrganizationIdAndBranchId(
      String keycloakUserId, String organizationId, String branchId);

  Optional<Membership> findByKeycloakUserIdAndOrganizationIdAndBranchIdIsNull(
      String keycloakUserId, String organizationId);
}
