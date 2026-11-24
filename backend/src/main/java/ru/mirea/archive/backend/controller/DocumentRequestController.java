package ru.mirea.archive.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.mirea.archive.backend.domain.Priority;
import ru.mirea.archive.backend.domain.RequestStatus;
import ru.mirea.archive.backend.dto.*;
import ru.mirea.archive.backend.error.BadRequestException;
import ru.mirea.archive.backend.security.UserPrincipal;
import ru.mirea.archive.backend.service.BulkOperationService;
import ru.mirea.archive.backend.service.CsvService;
import ru.mirea.archive.backend.service.DocumentRequestService;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@RestController
@RequestMapping("/api/document-requests")
@Tag(name = "Запросы архивных документов", description = "Основная сущность информационной системы")
public class DocumentRequestController {

    private final DocumentRequestService documentRequestService;
    private final BulkOperationService bulkOperationService;
    private final CsvService csvService;

    public DocumentRequestController(
            DocumentRequestService documentRequestService,
            BulkOperationService bulkOperationService,
            CsvService csvService
    ) {
        this.documentRequestService = documentRequestService;
        this.bulkOperationService = bulkOperationService;
        this.csvService = csvService;
    }

    @GetMapping
    @Operation(summary = "Список запросов с фильтрами, сортировкой и пагинацией")
    public ResponseEntity<Page<DocumentRequestDto>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Boolean confidential,
            @RequestParam(required = false) Boolean archived,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Integer estimatedDays,
            @RequestParam(required = false) Integer minEstimatedDays,
            @RequestParam(required = false) Integer maxEstimatedDays,
            @RequestParam(required = false) String pageOp,
            @RequestParam(required = false) Integer pages,
            @RequestParam(required = false) Integer minPages,
            @RequestParam(required = false) Integer maxPages,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return ResponseEntity.ok(documentRequestService.listRequests(
                search, userId, category, status, priority,
                confidential, archived, tag, estimatedDays,
                minEstimatedDays, maxEstimatedDays,
                pageOp, pages, minPages, maxPages,
                page, size, sortBy, sortDir
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получение карточки запроса по UUID")
    public ResponseEntity<DocumentRequestDto> getById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(documentRequestService.getByUuid(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN')")
    @Operation(summary = "Создание нового запроса на архивный документ")
    public ResponseEntity<DocumentRequestDto> create(
            @Valid @RequestBody CreateDocumentRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentRequestService.create(request, principal.getUsername()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN')")
    @Operation(summary = "Изменение запроса с проверкой версии (оптимистичная блокировка)")
    public ResponseEntity<DocumentRequestDto> update(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateDocumentRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(documentRequestService.update(id, request, principal.getUsername()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN')")
    @Operation(summary = "Архивирование запроса")
    public ResponseEntity<DocumentRequestDto> archive(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(documentRequestService.archive(id, principal.getUsername()));
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN')")
    @Operation(summary = "Восстановление запроса из архива")
    public ResponseEntity<DocumentRequestDto> restore(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(documentRequestService.restore(id, principal.getUsername()));
    }

    @DeleteMapping("/{id}/permanent")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Окончательное удаление запроса (только для Администратора)")
    public ResponseEntity<Void> permanentDelete(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        documentRequestService.permanentDelete(id, principal.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN')")
    @Operation(summary = "Массовая обработка записей по списку UUID")
    public ResponseEntity<BulkOperationResult> bulkOperation(
            @Valid @RequestBody BulkOperationRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(bulkOperationService.execute(request, principal.getUsername()));
    }

    @GetMapping("/export")
    @Operation(summary = "Экспорт данных в формате CSV с учётом текущих фильтров")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Boolean confidential,
            @RequestParam(required = false) Boolean archived,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Integer estimatedDays,
            @RequestParam(required = false) Integer minEstimatedDays,
            @RequestParam(required = false) Integer maxEstimatedDays,
            @RequestParam(required = false) String pageOp,
            @RequestParam(required = false) Integer pages,
            @RequestParam(required = false) Integer minPages,
            @RequestParam(required = false) Integer maxPages
    ) {
        byte[] csv = csvService.exportCsv(
                search, userId, category, status, priority,
                confidential, archived, tag, estimatedDays,
                minEstimatedDays, maxEstimatedDays,
                pageOp, pages, minPages, maxPages
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", "document_requests.csv");

        return ResponseEntity.ok().headers(headers).body(csv);
    }

    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN')")
    @Operation(summary = "Предварительная проверка CSV перед импортом")
    public ResponseEntity<CsvPreviewResponse> importPreviewMultipart(@RequestParam("file") MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            return ResponseEntity.ok(csvService.previewCsv(is));
        } catch (IOException e) {
            throw new BadRequestException("Не удалось прочитать загруженный файл: " + e.getMessage());
        }
    }

    @PostMapping(value = "/import/preview", consumes = MediaType.TEXT_PLAIN_VALUE)
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN')")
    @Operation(summary = "Предварительная проверка CSV из тела запроса")
    public ResponseEntity<CsvPreviewResponse> importPreviewText(@RequestBody String csvContent) {
        InputStream is = new java.io.ByteArrayInputStream(csvContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return ResponseEntity.ok(csvService.previewCsv(is));
    }

    @PostMapping("/import/apply")
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN')")
    @Operation(summary = "Применение импорта CSV с выбором режима")
    public ResponseEntity<CsvApplyResponse> importApply(
            @Valid @RequestBody CsvApplyRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(csvService.applyImport(request, principal.getUsername()));
    }
}
