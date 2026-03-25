package com.ebikes.iam.database.repositories;

import com.ebikes.iam.database.entities.Token;
import com.ebikes.iam.enums.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TokenRepository
    extends JpaRepository<Token, UUID>, JpaSpecificationExecutor<Token> {

  Optional<Token> findByTokenHash(@Param("tokenHash") String tokenHash);

  @Modifying
  @Transactional
  @Query(
      "UPDATE Token t SET t.consumed = true "
          + "WHERE t.userExtensionId = :userExtensionId "
          + "AND t.tokenType = :tokenType "
          + "AND t.consumed = false")
  int invalidateTokensByUserAndType(
      @Param("userExtensionId") UUID userExtensionId, @Param("tokenType") TokenType tokenType);
}
