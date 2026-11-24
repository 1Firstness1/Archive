package ru.mirea.archive.backend.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.archive.backend.domain.ArchiveUser;
import ru.mirea.archive.backend.domain.DocumentRequest;
import ru.mirea.archive.backend.domain.Priority;
import ru.mirea.archive.backend.domain.RequestStatus;
import ru.mirea.archive.backend.dto.CsvApplyRequest;
import ru.mirea.archive.backend.dto.CsvApplyResponse;
import ru.mirea.archive.backend.dto.CsvPreviewResponse;
import ru.mirea.archive.backend.error.BadRequestException;
import ru.mirea.archive.backend.repository.ArchiveUserRepository;
import ru.mirea.archive.backend.repository.DocumentRequestRepository;
import ru.mirea.archive.backend.repository.DocumentRequestSpecification;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class CsvService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DocumentRequestRepository documentRequestRepository;
    private final ArchiveUserRepository archiveUserRepository;
    private final AuditService auditService;

    public CsvService(
            DocumentRequestRepository documentRequestRepository,
            ArchiveUserRepository archiveUserRepository,
            AuditService auditService
    ) {
        this.documentRequestRepository = documentRequestRepository;
        this.archiveUserRepository = archiveUserRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv(
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
        Specification<DocumentRequest> spec = DocumentRequestSpecification.filterBy(
                search, userId, category, status, priority,
                confidential, archived, tag, estimatedDays,
                minEstimatedDays, maxEstimatedDays,
                pageOp, pages, minPages, maxPages
        );

        List<DocumentRequest> list = documentRequestRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));

        StringWriter sw = new StringWriter();
        // UTF-8 BOM для корректного открытия в Excel
        sw.write('\ufeff');

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader("requestNumber", "documentTitle", "description", "category",
                        "status", "priority", "pageCount", "isConfidential",
                        "estimatedDays", "isArchived", "userId", "userName", "tags", "createdAt")
                .build();

        try (CSVPrinter printer = new CSVPrinter(sw, format)) {
            for (DocumentRequest doc : list) {
                printer.printRecord(
                        doc.getRequestNumber(),
                        doc.getDocumentTitle(),
                        doc.getDescription() != null ? doc.getDescription() : "",
                        doc.getCategory(),
                        doc.getStatus().name(),
                        doc.getPriority().name(),
                        doc.getPageCount(),
                        doc.isConfidential(),
                        doc.getEstimatedDays(),
                        doc.isArchived(),
                        doc.getUser() != null ? doc.getUser().getId() : "",
                        doc.getUser() != null ? doc.getUser().getFullName() : "",
                        String.join(";", doc.getTags()),
                        doc.getCreatedAt() != null ? doc.getCreatedAt().format(FMT) : ""
                );
            }
        } catch (IOException e) {
            throw new RuntimeException("Ошибка формирования CSV: " + e.getMessage(), e);
        }

        return sw.toString().getBytes(StandardCharsets.UTF_8);
    }

    public CsvPreviewResponse previewCsv(InputStream inputStream) {
        CsvPreviewResponse response = new CsvPreviewResponse();
        List<Map<String, String>> previewData = new ArrayList<>();
        List<CsvPreviewResponse.RowError> errors = new ArrayList<>();

        int total = 0;
        int valid = 0;
        int invalid = 0;

        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreHeaderCase(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {

            for (CSVRecord record : parser) {
                total++;
                int rowNum = (int) record.getRecordNumber();
                Map<String, String> rowMap = record.toMap();
                boolean rowValid = true;

                // Валидация обязательных полей
                String title = rowMap.get("documentTitle");
                if (title == null || title.isBlank()) {
                    errors.add(new CsvPreviewResponse.RowError(rowNum, "documentTitle", "Поле 'documentTitle' обязательно"));
                    rowValid = false;
                }

                String category = rowMap.get("category");
                if (category == null || category.isBlank()) {
                    errors.add(new CsvPreviewResponse.RowError(rowNum, "category", "Поле 'category' обязательно"));
                    rowValid = false;
                }

                String userIdStr = rowMap.get("userId");
                if (userIdStr == null || userIdStr.isBlank()) {
                    errors.add(new CsvPreviewResponse.RowError(rowNum, "userId", "Поле 'userId' обязательно"));
                    rowValid = false;
                } else {
                    try {
                        Long uid = Long.parseLong(userIdStr.trim());
                        if (!archiveUserRepository.existsById(uid)) {
                            errors.add(new CsvPreviewResponse.RowError(rowNum, "userId", "Заявитель с ID " + uid + " не существует"));
                            rowValid = false;
                        }
                    } catch (NumberFormatException e) {
                        errors.add(new CsvPreviewResponse.RowError(rowNum, "userId", "ID заявителя должен быть числом"));
                        rowValid = false;
                    }
                }

                String pageCountStr = rowMap.get("pageCount");
                if (pageCountStr != null && !pageCountStr.isBlank()) {
                    try {
                        int pages = Integer.parseInt(pageCountStr.trim());
                        if (pages < 1) {
                            errors.add(new CsvPreviewResponse.RowError(rowNum, "pageCount", "Количество страниц должно быть >= 1"));
                            rowValid = false;
                        }
                    } catch (NumberFormatException e) {
                        errors.add(new CsvPreviewResponse.RowError(rowNum, "pageCount", "Количество страниц должно быть целым числом"));
                        rowValid = false;
                    }
                }

                if (rowValid) {
                    valid++;
                } else {
                    invalid++;
                }

                previewData.add(rowMap);
            }
        } catch (Exception e) {
            throw new BadRequestException("Ошибка чтения CSV файла: " + e.getMessage());
        }

        response.setTotalRows(total);
        response.setValidRows(valid);
        response.setInvalidRows(invalid);
        response.setPreviewData(previewData);
        response.setErrors(errors);
        return response;
    }

    @Transactional
    public CsvApplyResponse applyImport(CsvApplyRequest request, String currentUsername) {
        CsvApplyResponse response = new CsvApplyResponse();
        List<String> errorList = new ArrayList<>();
        int created = 0;
        int updated = 0;
        int skipped = 0;

        for (int i = 0; i < request.getRows().size(); i++) {
            Map<String, String> row = request.getRows().get(i);
            int rowIdx = i + 1;

            try {
                String reqNum = row.get("requestNumber");
                if (reqNum != null) reqNum = reqNum.trim();

                String title = row.get("documentTitle");
                String desc = row.get("description");
                String category = row.get("category");
                String userIdStr = row.get("userId");

                if (title == null || title.isBlank() || category == null || category.isBlank() || userIdStr == null || userIdStr.isBlank()) {
                    skipped++;
                    errorList.add("Строка " + rowIdx + ": пропущена из-за отсутствия обязательных полей");
                    continue;
                }

                Long userId = Long.parseLong(userIdStr.trim());
                ArchiveUser user = archiveUserRepository.findById(userId).orElse(null);
                if (user == null) {
                    skipped++;
                    errorList.add("Строка " + rowIdx + ": заявитель с ID " + userId + " не найден");
                    continue;
                }

                Priority priority = Priority.MEDIUM;
                if (row.get("priority") != null && !row.get("priority").isBlank()) {
                    try {
                        priority = Priority.valueOf(row.get("priority").trim().toUpperCase());
                    } catch (IllegalArgumentException ignored) {}
                }

                RequestStatus status = RequestStatus.NEW;
                if (row.get("status") != null && !row.get("status").isBlank()) {
                    try {
                        status = RequestStatus.valueOf(row.get("status").trim().toUpperCase());
                    } catch (IllegalArgumentException ignored) {}
                }

                int pages = 1;
                if (row.get("pageCount") != null && !row.get("pageCount").isBlank()) {
                    try {
                        pages = Math.max(1, Integer.parseInt(row.get("pageCount").trim()));
                    } catch (NumberFormatException ignored) {}
                }

                boolean confidential = Boolean.parseBoolean(row.get("isConfidential"));
                boolean archived = Boolean.parseBoolean(row.get("isArchived"));

                Set<String> tags = new HashSet<>();
                String tagsStr = row.get("tags");
                if (tagsStr != null && !tagsStr.isBlank()) {
                    for (String t : tagsStr.split(";")) {
                        if (!t.isBlank()) tags.add(t.trim().toLowerCase());
                    }
                }

                final String targetReqNum = reqNum;
                // Проверяем наличие по внешнему идентификатору (номеру запроса)
                Optional<DocumentRequest> existingOpt = (targetReqNum != null && !targetReqNum.isBlank())
                        ? documentRequestRepository.findAll().stream()
                        .filter(d -> d.getRequestNumber().equalsIgnoreCase(targetReqNum))
                        .findFirst()
                        : Optional.empty();

                if (existingOpt.isPresent()) {
                    if (request.getMode() == CsvApplyRequest.ImportMode.CREATE_ONLY) {
                        skipped++;
                        errorList.add("Строка " + rowIdx + ": запрос " + reqNum + " уже существует (режим только создания)");
                        continue;
                    }

                    // Режим UPDATE_EXISTING
                    DocumentRequest existing = existingOpt.get();
                    existing.setUser(user);
                    existing.setDocumentTitle(title.trim());
                    existing.setDescription(desc != null ? desc.trim() : null);
                    existing.setCategory(category.trim());
                    existing.setPriority(priority);
                    existing.setStatus(status);
                    existing.setPageCount(pages);
                    existing.setConfidential(confidential);
                    existing.setArchived(archived);
                    existing.getTags().addAll(tags);
                    existing.recalculateEstimatedDays();

                    documentRequestRepository.save(existing);
                    updated++;

                } else {
                    // Создание новой записи
                    String newReqNum = (reqNum != null && !reqNum.isBlank())
                            ? reqNum
                            : String.format("REQ-%d-%04d", java.time.LocalDate.now().getYear(), new Random().nextInt(9000) + 1000);

                    DocumentRequest newDoc = new DocumentRequest(
                            user,
                            newReqNum,
                            title.trim(),
                            desc != null ? desc.trim() : null,
                            category.trim(),
                            priority,
                            pages,
                            confidential
                    );
                    newDoc.setStatus(status);
                    newDoc.setArchived(archived);
                    newDoc.setTags(tags);

                    documentRequestRepository.save(newDoc);
                    created++;
                }

            } catch (Exception e) {
                errorList.add("Строка " + rowIdx + ": ошибка обработки — " + e.getMessage());
            }
        }

        response.setTotalProcessed(request.getRows().size());
        response.setCreatedCount(created);
        response.setUpdatedCount(updated);
        response.setSkippedCount(skipped);
        response.setErrorCount(errorList.size());
        response.setErrors(errorList);

        auditService.log(currentUsername, "CSV_IMPORT", "DOCUMENT_REQUEST", null,
                "Импорт CSV: создано " + created + ", обновлено " + updated + ", пропущено " + skipped);

        return response;
    }
}
