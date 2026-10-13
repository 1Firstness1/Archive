package ru.mirea.archive.service;

import ru.mirea.archive.exception.BusinessException;
import ru.mirea.archive.exception.EntityNotFoundException;
import ru.mirea.archive.model.DocumentRequest;
import ru.mirea.archive.model.Priority;
import ru.mirea.archive.model.RequestStatus;
import ru.mirea.archive.repository.DocumentRequestRepository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DocumentRequestService {

    private static final Map<RequestStatus, Set<RequestStatus>> ALLOWED_TRANSITIONS = Map.of(
            RequestStatus.NEW,         Set.of(RequestStatus.IN_PROGRESS, RequestStatus.CANCELLED),
            RequestStatus.IN_PROGRESS, Set.of(RequestStatus.APPROVED, RequestStatus.REJECTED, RequestStatus.CANCELLED),
            RequestStatus.APPROVED,    Set.of(RequestStatus.COMPLETED, RequestStatus.CANCELLED),
            RequestStatus.REJECTED,    Set.of(RequestStatus.NEW),
            RequestStatus.COMPLETED,   Set.of(),
            RequestStatus.CANCELLED,   Set.of(RequestStatus.NEW)
    );

    private final DocumentRequestRepository requestRepository;
    private final UserService userService;

    public DocumentRequestService(DocumentRequestRepository requestRepository, UserService userService) {
        this.requestRepository = requestRepository;
        this.userService = userService;
    }

    public DocumentRequest create(int userId, String documentTitle, String description,
                                  String category, Priority priority) {
        String trimmedTitle = documentTitle != null ? documentTitle.trim() : "";
        String trimmedCat = category != null ? category.trim() : "";
        String trimmedDesc = (description != null && !description.trim().isEmpty()) ? description.trim() : null;

        if (trimmedTitle.isEmpty()) {
            throw new BusinessException("Название документа не может быть пустым.");
        }

        if (trimmedCat.isEmpty()) {
            throw new BusinessException("Категория документа не может быть пустой.");
        }

        if (!userService.existsById(userId)) {
            throw new BusinessException("Пользователь с ID " + userId + " не найден.");
        }

        if (priority == null) {
            priority = Priority.MEDIUM;
        }

        DocumentRequest req = new DocumentRequest(userId, trimmedTitle, trimmedDesc, trimmedCat, priority);
        return requestRepository.save(req);
    }

    public List<DocumentRequest> findAll() {
        return requestRepository.findAll();
    }

    public DocumentRequest findById(int id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Запрос с ID " + id + " не найден."));
    }

    public void update(int id, int userId, String documentTitle, String description,
                       String category, Priority priority) {
        DocumentRequest existing = findById(id);

        String trimmedTitle = documentTitle != null ? documentTitle.trim() : "";
        String trimmedCat = category != null ? category.trim() : "";
        String trimmedDesc = (description != null && !description.trim().isEmpty()) ? description.trim() : null;

        if (trimmedTitle.isEmpty()) {
            throw new BusinessException("Название документа не может быть пустым.");
        }
        if (trimmedCat.isEmpty()) {
            throw new BusinessException("Категория документа не может быть пустой.");
        }
        if (!userService.existsById(userId)) {
            throw new BusinessException("Пользователь с ID " + userId + " не найден.");
        }

        existing.setUserId(userId);
        existing.setDocumentTitle(trimmedTitle);
        existing.setDescription(trimmedDesc);
        existing.setCategory(trimmedCat);
        existing.setPriority(priority != null ? priority : Priority.MEDIUM);
        requestRepository.update(existing);
    }

    public void changeStatus(int id, RequestStatus newStatus) {
        DocumentRequest req = findById(id);
        RequestStatus currentStatus = req.getStatus();

        if (newStatus == null) {
            throw new BusinessException("Статус не может быть пустым.");
        }

        Set<RequestStatus> allowed = ALLOWED_TRANSITIONS.get(currentStatus);
        if (allowed == null || !allowed.contains(newStatus)) {
            throw new BusinessException(
                    String.format("Переход из статуса '%s' в '%s' невозможен. Допустимые: %s",
                            currentStatus.getDisplayName(),
                            newStatus.getDisplayName(),
                            allowed != null ? allowed.stream()
                                    .map(RequestStatus::getDisplayName)
                                    .collect(Collectors.joining(", ")) : "нет")
            );
        }

        req.setStatus(newStatus);
        requestRepository.update(req);
    }

    public boolean delete(int id) {
        findById(id);
        return requestRepository.deleteById(id);
    }

    public List<DocumentRequest> searchByTitle(String title) {
        String trimmed = title != null ? title.trim() : "";
        if (trimmed.isEmpty()) {
            throw new BusinessException("Строка поиска не может быть пустой.");
        }
        return requestRepository.searchByTitle(trimmed);
    }

    public List<DocumentRequest> searchByDescription(String keyword) {
        String trimmed = keyword != null ? keyword.trim() : "";
        if (trimmed.isEmpty()) {
            throw new BusinessException("Строка поиска не может быть пустой.");
        }
        return requestRepository.searchByDescription(trimmed);
    }

    public List<DocumentRequest> findByUserId(int userId) {
        if (!userService.existsById(userId)) {
            throw new BusinessException("Пользователь с ID " + userId + " не найден.");
        }
        return requestRepository.findByUserId(userId);
    }

    public List<DocumentRequest> filterByStatus(RequestStatus status) {
        return requestRepository.filterByStatus(status);
    }

    public List<DocumentRequest> filterByPriority(Priority priority) {
        return requestRepository.filterByPriority(priority);
    }

    public List<DocumentRequest> filterByCategory(String category) {
        String trimmed = category != null ? category.trim() : "";
        if (trimmed.isEmpty()) {
            throw new BusinessException("Категория для фильтрации не может быть пустой.");
        }
        return requestRepository.filterByCategory(trimmed);
    }

    public List<DocumentRequest> filterByDateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BusinessException("Даты диапазона должны быть указаны.");
        }
        if (from.isAfter(to)) {
            throw new BusinessException("Дата начала не может быть позже даты окончания.");
        }
        return requestRepository.filterByDateRange(from, to);
    }

    public List<DocumentRequest> sortByDateDesc(List<DocumentRequest> list) {
        return list.stream()
                .sorted(Comparator.comparing(DocumentRequest::getCreatedAt).reversed())
                .toList();
    }

    public List<DocumentRequest> sortByPriorityDesc(List<DocumentRequest> list) {
        return list.stream()
                .sorted(Comparator.comparing(DocumentRequest::getPriority).reversed())
                .toList();
    }

    public List<DocumentRequest> sortByTitle(List<DocumentRequest> list) {
        return list.stream()
                .sorted(Comparator.comparing(DocumentRequest::getDocumentTitle, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public int count() {
        return requestRepository.count();
    }

    public int countByStatus(RequestStatus status) {
        return requestRepository.countByStatus(status);
    }

    public int countByPriority(Priority priority) {
        return requestRepository.countByPriority(priority);
    }

    public int countCategories() {
        return requestRepository.countCategories();
    }

    public List<String> findAllCategories() {
        return requestRepository.findAllCategories();
    }
}
