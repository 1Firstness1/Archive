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
        if (fullName == null || fullName.isBlank()) {
            throw new BusinessException("ФИО пользователя не может быть пустым.");
        }
        if (email == null || email.isBlank()) {
            throw new BusinessException("Email не может быть пустым.");
        }
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            throw new BusinessException("Некорректный формат email: " + email);
        }
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("Пользователь с email '" + email + "' уже существует.");
        }

        User user = new User(fullName.trim(), email.trim(), phone, department);
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

        if (fullName == null || fullName.isBlank()) {
            throw new BusinessException("ФИО пользователя не может быть пустым.");
        }
        if (email == null || email.isBlank()) {
            throw new BusinessException("Email не может быть пустым.");
        }
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            throw new BusinessException("Некорректный формат email: " + email);
        }
        if (userRepository.existsByEmailExcludingId(email, id)) {
            throw new BusinessException("Email '" + email + "' уже используется другим пользователем.");
        }

        user.setFullName(fullName.trim());
        user.setEmail(email.trim());
        user.setPhone(phone);
        user.setDepartment(department);
        userRepository.update(user);
    }

    public boolean delete(int id) {
        findById(id);
        return userRepository.deleteById(id);
    }

    public List<User> searchByName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("Строка поиска не может быть пустой.");
        }
        return userRepository.searchByName(name);
    }

    public int count() {
        return userRepository.count();
    }

    public boolean existsById(int id) {
        return userRepository.findById(id).isPresent();
    }
}
