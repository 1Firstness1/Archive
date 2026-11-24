package ru.mirea.archive.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.mirea.archive.backend.domain.DocumentNote;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentNoteRepository extends JpaRepository<DocumentNote, UUID> {
    List<DocumentNote> findByDocumentRequest_UuidOrderByPinnedDescCreatedAtDesc(UUID requestUuid);
    Optional<DocumentNote> findByIdAndDocumentRequest_Uuid(UUID id, UUID requestUuid);
}
