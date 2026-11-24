package ru.mirea.archive.backend.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "document_requests")
public class DocumentRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID uuid = UUID.randomUUID();

    @Column(name = "request_number", nullable = false, unique = true, length = 64)
    private String requestNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private ArchiveUser user;

    @Column(name = "document_title", nullable = false, length = 300)
    private String documentTitle;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 100)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RequestStatus status = RequestStatus.NEW;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Priority priority = Priority.MEDIUM;

    @Column(name = "page_count", nullable = false)
    private int pageCount = 1;

    @Column(name = "is_confidential", nullable = false)
    private boolean confidential = false;

    @Column(name = "estimated_days", nullable = false)
    private int estimatedDays = 3;

    @Column(name = "is_archived", nullable = false)
    private boolean archived = false;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "document_request_tags",
            joinColumns = @JoinColumn(name = "request_uuid", referencedColumnName = "uuid")
    )
    @Column(name = "tag", length = 50)
    private Set<String> tags = new HashSet<>();

    @OneToMany(mappedBy = "documentRequest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("pinned DESC, createdAt DESC")
    private List<DocumentNote> notes = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public DocumentRequest() {
    }

    public DocumentRequest(ArchiveUser user, String requestNumber, String documentTitle,
                           String description, String category, Priority priority,
                           int pageCount, boolean confidential) {
        this.uuid = UUID.randomUUID();
        this.user = user;
        this.requestNumber = requestNumber;
        this.documentTitle = documentTitle;
        this.description = description;
        this.category = category;
        this.priority = priority != null ? priority : Priority.MEDIUM;
        this.status = RequestStatus.NEW;
        this.pageCount = Math.max(1, pageCount);
        this.confidential = confidential;
        this.archived = false;
        this.version = 0L;
        this.estimatedDays = calculateEstimatedDays(this.priority, this.pageCount, this.confidential);
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Бизнес-правило вычисления планового срока обработки документа в днях.
     * Базовые дни зависят от приоритета: CRITICAL=1, HIGH=2, MEDIUM=4, LOW=6.
     * Дополнительно: +1 день на каждые 20 страниц объема и +1 день при грифе конфиденциальности.
     */
    public static int calculateEstimatedDays(Priority priority, int pages, boolean confidential) {
        int base = switch (priority != null ? priority : Priority.MEDIUM) {
            case CRITICAL -> 1;
            case HIGH -> 2;
            case MEDIUM -> 4;
            case LOW -> 6;
        };
        int pageAddition = Math.max(0, pages) / 20;
        int confAddition = confidential ? 1 : 0;
        return Math.max(1, base + pageAddition + confAddition);
    }

    public void recalculateEstimatedDays() {
        this.estimatedDays = calculateEstimatedDays(this.priority, this.pageCount, this.confidential);
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getRequestNumber() {
        return requestNumber;
    }

    public void setRequestNumber(String requestNumber) {
        this.requestNumber = requestNumber;
    }

    public ArchiveUser getUser() {
        return user;
    }

    public void setUser(ArchiveUser user) {
        this.user = user;
    }

    public String getDocumentTitle() {
        return documentTitle;
    }

    public void setDocumentTitle(String documentTitle) {
        this.documentTitle = documentTitle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    public boolean isConfidential() {
        return confidential;
    }

    public void setConfidential(boolean confidential) {
        this.confidential = confidential;
    }

    public int getEstimatedDays() {
        return estimatedDays;
    }

    public void setEstimatedDays(int estimatedDays) {
        this.estimatedDays = estimatedDays;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags != null ? tags : new HashSet<>();
    }

    public List<DocumentNote> getNotes() {
        return notes;
    }

    public void setNotes(List<DocumentNote> notes) {
        this.notes = notes != null ? notes : new ArrayList<>();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
