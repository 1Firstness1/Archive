package ru.mirea.archive.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.archive.backend.domain.ArchiveUser;
import ru.mirea.archive.backend.domain.DocumentRequest;
import ru.mirea.archive.backend.domain.Priority;
import ru.mirea.archive.backend.domain.RequestStatus;
import ru.mirea.archive.backend.repository.ArchiveUserRepository;
import ru.mirea.archive.backend.repository.DocumentRequestRepository;
import ru.mirea.archive.backend.repository.DocumentRequestSpecification;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class DocumentRequestFilterTest {

    @Autowired
    private DocumentRequestRepository documentRequestRepository;

    @Autowired
    private ArchiveUserRepository archiveUserRepository;

    private ArchiveUser user1;
    private ArchiveUser user2;

    @BeforeEach
    void setUp() {
        documentRequestRepository.deleteAll();
        archiveUserRepository.deleteAll();

        user1 = archiveUserRepository.save(new ArchiveUser("Иванов Иван", "ivanov@test.ru", "+79991112233", "Юридический отдел", true));
        user2 = archiveUserRepository.save(new ArchiveUser("Петрова Анна", "petrova@test.ru", "+79992223344", "Бухгалтерия", true));

        // Документ 1: Приказ, открытый, 15 страниц, NEW, LOW, тег: приказ
        DocumentRequest doc1 = new DocumentRequest(user1, "REQ-2026-1001", "Приказ о назначении премий", "Годовой приказ", "Приказы", Priority.LOW, 15, false);
        doc1.setStatus(RequestStatus.NEW);
        doc1.setTags(Set.of("приказ", "финансы"));
        documentRequestRepository.save(doc1);

        // Документ 2: Договор, конфиденциальный, 50 страниц, IN_PROGRESS, HIGH, тег: договор
        DocumentRequest doc2 = new DocumentRequest(user2, "REQ-2026-1002", "Договор аренды помещений", "Договор с арендодателем", "Договоры", Priority.HIGH, 50, true);
        doc2.setStatus(RequestStatus.IN_PROGRESS);
        doc2.setTags(Set.of("договор", "недвижимость"));
        documentRequestRepository.save(doc2);

        // Документ 3: Архивный отчет, открытый, 5 страниц, COMPLETED, CRITICAL, архив=true
        DocumentRequest doc3 = new DocumentRequest(user1, "REQ-2026-1003", "Архивный отчет за 2020 год", "Итоговый отчет", "Отчеты", Priority.CRITICAL, 5, false);
        doc3.setStatus(RequestStatus.COMPLETED);
        doc3.setArchived(true);
        doc3.setTags(Set.of("отчет"));
        documentRequestRepository.save(doc3);
    }

    @Test
    @DisplayName("3.1. Свободный текстовый поиск по названию и заявителю")
    void testFreeTextSearch() {
        // Поиск "премий" -> находит Документ 1
        Specification<DocumentRequest> specTitle = DocumentRequestSpecification.filterBy(
                "премий", null, null, null, null, null, null, null, null, null, null, null, null, null, null
        );
        Page<DocumentRequest> pageTitle = documentRequestRepository.findAll(specTitle, PageRequest.of(0, 10));
        assertThat(pageTitle.getContent()).hasSize(1);
        assertThat(pageTitle.getContent().get(0).getRequestNumber()).isEqualTo("REQ-2026-1001");

        // Поиск по ФИО заявителя "Петрова" -> находит Документ 2
        Specification<DocumentRequest> specUser = DocumentRequestSpecification.filterBy(
                "Петрова", null, null, null, null, null, null, null, null, null, null, null, null, null, null
        );
        Page<DocumentRequest> pageUser = documentRequestRepository.findAll(specUser, PageRequest.of(0, 10));
        assertThat(pageUser.getContent()).hasSize(1);
        assertThat(pageUser.getContent().get(0).getRequestNumber()).isEqualTo("REQ-2026-1002");
    }

    @Test
    @DisplayName("3.2. Фильтр по числовому полю (pageCount) с операциями сравнения (gte, lt)")
    void testPageCountComparisonFilter() {
        // Страниц >= 20 -> Документ 2 (50 стр)
        Specification<DocumentRequest> specGte = DocumentRequestSpecification.filterBy(
                null, null, null, null, null, null, null, null, null, null, null, "gte", 20, null, null
        );
        Page<DocumentRequest> pageGte = documentRequestRepository.findAll(specGte, PageRequest.of(0, 10));
        assertThat(pageGte.getContent()).hasSize(1);
        assertThat(pageGte.getContent().get(0).getPageCount()).isEqualTo(50);

        // Страниц < 20 -> Документ 1 (15 стр) и Документ 3 (5 стр)
        Specification<DocumentRequest> specLt = DocumentRequestSpecification.filterBy(
                null, null, null, null, null, null, null, null, null, null, null, "lt", 20, null, null
        );
        Page<DocumentRequest> pageLt = documentRequestRepository.findAll(specLt, PageRequest.of(0, 10));
        assertThat(pageLt.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("3.3. Фильтр по статусу, конфиденциальности и тегам")
    void testStatusAndConfidentialityFilter() {
        // Конфиденциальные -> Документ 2
        Specification<DocumentRequest> specConf = DocumentRequestSpecification.filterBy(
                null, null, null, null, null, true, null, null, null, null, null, null, null, null, null
        );
        Page<DocumentRequest> pageConf = documentRequestRepository.findAll(specConf, PageRequest.of(0, 10));
        assertThat(pageConf.getContent()).hasSize(1);
        assertThat(pageConf.getContent().get(0).isConfidential()).isTrue();

        // Тег "финансы" -> Документ 1
        Specification<DocumentRequest> specTag = DocumentRequestSpecification.filterBy(
                null, null, null, null, null, null, null, "финансы", null, null, null, null, null, null, null
        );
        Page<DocumentRequest> pageTag = documentRequestRepository.findAll(specTag, PageRequest.of(0, 10));
        assertThat(pageTag.getContent()).hasSize(1);
        assertThat(pageTag.getContent().get(0).getTags()).contains("финансы");
    }

    @Test
    @DisplayName("3.4. Фильтр по архивности")
    void testArchivedFilter() {
        // Только активные -> Документ 1 и Документ 2
        Specification<DocumentRequest> specActive = DocumentRequestSpecification.filterBy(
                null, null, null, null, null, null, false, null, null, null, null, null, null, null, null
        );
        Page<DocumentRequest> pageActive = documentRequestRepository.findAll(specActive, PageRequest.of(0, 10));
        assertThat(pageActive.getContent()).hasSize(2);

        // Только архивные -> Документ 3
        Specification<DocumentRequest> specArchived = DocumentRequestSpecification.filterBy(
                null, null, null, null, null, null, true, null, null, null, null, null, null, null, null
        );
        Page<DocumentRequest> pageArchived = documentRequestRepository.findAll(specArchived, PageRequest.of(0, 10));
        assertThat(pageArchived.getContent()).hasSize(1);
        assertThat(pageArchived.getContent().get(0).isArchived()).isTrue();
    }
}
