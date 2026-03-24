package com.raissa.bffapis.domain.repository;

import com.raissa.bffapis.domain.entity.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProviderRepository extends JpaRepository<Provider, Integer> {
    Optional<Provider> findByReferenceAndActive(String reference, Short active);
}
