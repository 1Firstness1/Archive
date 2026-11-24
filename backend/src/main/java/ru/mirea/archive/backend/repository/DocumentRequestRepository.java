package ru.mirea.archive.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.mirea.archive.backend.domain.DocumentRequest;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRequestRepository extends JpaRepository<DocumentRequest, Long>, JpaSpecificationExecutor<DocumentRequest> {

    Optional<DocumentRequest> findByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);

    boolean existsByRequestNumber(String requestNumber);

    boolean existsByRequestNumberAndUuidNot(String requestNumber, UUID uuid);

    void deleteByUuid(UUID uuid);

    long countByArchived(boolean archived);

    long countByConfidential(boolean confidential);

    @Query("SELECT AVG(d.pageCount) FROM DocumentRequest d WHERE d.archived = false")
    Double findAveragePageCountActive();

    @Query("SELECT AVG(d.pageCount) FROM DocumentRequest d")
    Double findAveragePageCountTotal();
}
