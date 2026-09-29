package ru.mirea.archive.service;

import ru.mirea.archive.exception.BusinessException;
import ru.mirea.archive.exception.EntityNotFoundException;
import ru.mirea.archive.model.User;
import ru.mirea.archive.repository.UserRepository;

import java.util.List;

public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User create(String fullName, String email, String phone, String department) {
        String trimmedName = fullName != null ? fullName.trim() : "";
        String trimmedEmail = email != null ? email.trim() : "";
        String trimmedPhone = (phone != null && !phone.trim().isEmpty()) ? phone.trim() : null;
        String trimmedDept = (department != null && !department.trim().isEmpty()) ? department.trim() : null;

        if (trimmedName.isEmpty()) {
            throw new BusinessException("ФИО пользователя не может быть пустым.");
        }
        if (trimmedEmail.isEmpty()) {
            throw new BusinessException("Email не может быть пустым.");
        }
        if (!trimmedEmail.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            throw new BusinessException("Некорректный формат email: " + trimmedEmail);
        }
        if (userRepository.existsByEmail(trimmedEmail)) {
            throw new BusinessException("Пользователь с email '" + trimmedEmail + "' уже существует.");
        }

        User user = new User(trimmedName, trimmedEmail, trimmedPhone, trimmedDept);
        return userRepository.save(user);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findById(int id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с ID " + id + " не найден."));
    }

    public void update(int id, String fullName, String email, String phone, String department) {
        User user = findById(id);

        String trimmedName = fullName != null ? fullName.trim() : "";
        String trimmedEmail = email != null ? email.trim() : "";
        String trimmedPhone = (phone != null && !phone.trim().isEmpty()) ? phone.trim() : null;
        String trimmedDept = (department != null && !department.trim().isEmpty()) ? department.trim() : null;

        if (trimmedName.isEmpty()) {
            throw new BusinessException("ФИО пользователя не может быть пустым.");
        }
        if (trimmedEmail.isEmpty()) {
            throw new BusinessException("Email не может быть пустым.");
        }
        if (!trimmedEmail.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            throw new BusinessException("Некорректный формат email: " + trimmedEmail);
        }
        if (userRepository.existsByEmailExcludingId(trimmedEmail, id)) {
            throw new BusinessException("Email '" + trimmedEmail + "' уже используется другим пользователем.");
        }

        user.setFullName(trimmedName);
        user.setEmail(trimmedEmail);
        user.setPhone(trimmedPhone);
        user.setDepartment(trimmedDept);
        userRepository.update(user);
    }

    public boolean delete(int id) {
        findById(id);
        return userRepository.deleteById(id);
    }

    public List<User> searchByName(String name) {
        String trimmed = name != null ? name.trim() : "";
        if (trimmed.isEmpty()) {
            throw new BusinessException("Строка поиска не может быть пустой.");
        }
        return userRepository.searchByName(trimmed);
    }

    public int count() {
        return userRepository.count();
    }

    public boolean existsById(int id) {
        return userRepository.findById(id).isPresent();
    }
}
