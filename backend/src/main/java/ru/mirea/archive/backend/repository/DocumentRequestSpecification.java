package ru.mirea.archive.backend.repository;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import ru.mirea.archive.backend.domain.ArchiveUser;
import ru.mirea.archive.backend.domain.DocumentRequest;
import ru.mirea.archive.backend.domain.Priority;
import ru.mirea.archive.backend.domain.RequestStatus;

import java.util.ArrayList;
import java.util.List;

public class DocumentRequestSpecification {

    public static Specification<DocumentRequest> filterBy(
            String search,
            Long userId,
            String category,
            RequestStatus status,
            Priority priority,
            Boolean confidential,
            Boolean archived,
            String tag,
            Integer estimatedDays,
            Integer minEstimatedDays,
            Integer maxEstimatedDays,
            String pageOp,
            Integer pages,
            Integer minPages,
            Integer maxPages
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Свободный текстовый поиск
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Join<DocumentRequest, ArchiveUser> userJoin = root.join("user", JoinType.LEFT);

                Predicate titlePred = cb.like(cb.lower(root.get("documentTitle")), pattern);
                Predicate descPred = cb.like(cb.lower(root.get("description")), pattern);
                Predicate numPred = cb.like(cb.lower(root.get("requestNumber")), pattern);
                Predicate catPred = cb.like(cb.lower(root.get("category")), pattern);
                Predicate userPred = cb.like(cb.lower(userJoin.get("fullName")), pattern);

                predicates.add(cb.or(titlePred, descPred, numPred, catPred, userPred));
            }

            // 2. Фильтр по справочнику (пользователю архива)
            if (userId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
            }

            // 3. Фильтр по категории
            if (category != null && !category.isBlank()) {
                predicates.add(cb.equal(root.get("category"), category.trim()));
            }

            // 4. Фильтр по статусу
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // 5. Фильтр по приоритету
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }

            // 6. Логический признак конфиденциальности
            if (confidential != null) {
                predicates.add(cb.equal(root.get("confidential"), confidential));
            }

            // 7. Признак архива
            if (archived != null) {
                predicates.add(cb.equal(root.get("archived"), archived));
            }

            // 8. Фильтр по тегу
            if (tag != null && !tag.isBlank()) {
                Join<DocumentRequest, String> tagJoin = root.join("tags", JoinType.INNER);
                predicates.add(cb.equal(cb.lower(tagJoin), tag.trim().toLowerCase()));
                if (query != null) {
                    query.distinct(true);
                }
            }

            // 9. Фильтр по вычисляемому показателю (estimatedDays)
            if (estimatedDays != null) {
                predicates.add(cb.equal(root.get("estimatedDays"), estimatedDays));
            }
            if (minEstimatedDays != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("estimatedDays"), minEstimatedDays));
            }
            if (maxEstimatedDays != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("estimatedDays"), maxEstimatedDays));
            }

            // 10. Фильтр по числовому полю (pageCount) с операциями сравнения
            if (pages != null && pageOp != null) {
                switch (pageOp.trim().toLowerCase()) {
                    case "gt", ">" -> predicates.add(cb.greaterThan(root.get("pageCount"), pages));
                    case "gte", ">=" -> predicates.add(cb.greaterThanOrEqualTo(root.get("pageCount"), pages));
                    case "lt", "<" -> predicates.add(cb.lessThan(root.get("pageCount"), pages));
                    case "lte", "<=" -> predicates.add(cb.lessThanOrEqualTo(root.get("pageCount"), pages));
                    default -> predicates.add(cb.equal(root.get("pageCount"), pages));
                }
            } else if (pages != null) {
                predicates.add(cb.equal(root.get("pageCount"), pages));
            }

            if (minPages != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("pageCount"), minPages));
            }
            if (maxPages != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("pageCount"), maxPages));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
