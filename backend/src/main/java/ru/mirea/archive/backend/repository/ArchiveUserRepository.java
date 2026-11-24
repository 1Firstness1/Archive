package ru.mirea.archive.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.mirea.archive.backend.domain.ArchiveUser;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArchiveUserRepository extends JpaRepository<ArchiveUser, Long> {
    Optional<ArchiveUser> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);
    List<ArchiveUser> findAllByActiveTrueOrderByFullNameAsc();
    List<ArchiveUser> findAllByOrderByFullNameAsc();
}
