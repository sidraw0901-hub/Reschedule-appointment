package com.carebridge.rescheduling.domain.repository;

import com.carebridge.rescheduling.domain.model.Provider;
import com.carebridge.rescheduling.domain.model.ProviderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProviderRepository extends JpaRepository<Provider, UUID> {
    List<Provider> findByStatus(ProviderStatus status);
    List<Provider> findBySpecialtyAndStatus(String specialty, ProviderStatus status);
}
