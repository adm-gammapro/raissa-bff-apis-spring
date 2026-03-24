package com.raissa.bffapis.domain.repository;

import com.raissa.bffapis.domain.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
    /**
     * Busca sesión por transactionId
     */
    Optional<Session> findByTransactionId(String transactionId);
}
