package ru.mirea.archive.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.archive.backend.domain.ArchiveUser;
import ru.mirea.archive.backend.dto.ArchiveUserDto;
import ru.mirea.archive.backend.dto.CreateArchiveUserRequest;
import ru.mirea.archive.backend.dto.UpdateArchiveUserRequest;
import ru.mirea.archive.backend.error.DuplicateResourceException;
import ru.mirea.archive.backend.error.ResourceNotFoundException;
import ru.mirea.archive.backend.repository.ArchiveUserRepository;

import java.util.List;

@Service
public class ArchiveUserService {

    private final ArchiveUserRepository archiveUserRepository;
    private final AuditService auditService;

    public ArchiveUserService(ArchiveUserRepository archiveUserRepository, AuditService auditService) {
        this.archiveUserRepository = archiveUserRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ArchiveUserDto> listAll(boolean includeInactive) {
        List<ArchiveUser> list = includeInactive
                ? archiveUserRepository.findAllByOrderByFullNameAsc()
                : archiveUserRepository.findAllByActiveTrueOrderByFullNameAsc();
        return list.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ArchiveUserDto getById(Long id) {
        ArchiveUser user = archiveUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Заявитель архива не найден: " + id));
        return toDto(user);
    }

    @Transactional
    public ArchiveUserDto create(CreateArchiveUserRequest request, String currentUsername) {
        String email = request.getEmail().trim().toLowerCase();
        if (archiveUserRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Заявитель с email '" + email + "' уже существует");
        }

        ArchiveUser user = new ArchiveUser(
                request.getFullName().trim(),
                email,
                request.getPhone() != null ? request.getPhone().trim() : null,
                request.getDepartment() != null ? request.getDepartment().trim() : null,
                request.isActive()
        );
        archiveUserRepository.save(user);

        auditService.log(currentUsername, "DICTIONARY_CREATE_USER", "ARCHIVE_USER", user.getId().toString(),
                "Создан заявитель архива: " + user.getFullName() + " (" + user.getDepartment() + ")");

        return toDto(user);
    }

    @Transactional
    public ArchiveUserDto update(Long id, UpdateArchiveUserRequest request, String currentUsername) {
        ArchiveUser user = archiveUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Заявитель архива не найден: " + id));

        String email = request.getEmail().trim().toLowerCase();
        if (archiveUserRepository.existsByEmailAndIdNot(email, id)) {
            throw new DuplicateResourceException("Email '" + email + "' уже занят другим заявителем");
        }

        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        user.setDepartment(request.getDepartment() != null ? request.getDepartment().trim() : null);
        user.setActive(request.isActive());

        archiveUserRepository.save(user);

        auditService.log(currentUsername, "DICTIONARY_UPDATE_USER", "ARCHIVE_USER", user.getId().toString(),
                "Обновлен заявитель архива: " + user.getFullName() + " (активен: " + user.isActive() + ")");

        return toDto(user);
    }

    @Transactional
    public ArchiveUserDto toggleActive(Long id, String currentUsername) {
        ArchiveUser user = archiveUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Заявитель архива не найден: " + id));

        user.setActive(!user.isActive());
        archiveUserRepository.save(user);

        auditService.log(currentUsername, "DICTIONARY_TOGGLE_USER", "ARCHIVE_USER", user.getId().toString(),
                "Статус активности заявителя " + user.getFullName() + " изменен на: " + user.isActive());

        return toDto(user);
    }

    public ArchiveUserDto toDto(ArchiveUser user) {
        return new ArchiveUserDto(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getDepartment(),
                user.isActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
