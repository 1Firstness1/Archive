package ru.mirea.archive.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ru.mirea.archive.backend.domain.Role;

public class CreateAdminUserRequest {

    @NotBlank(message = "Логин обязателен")
    @Size(min = 3, max = 50, message = "Логин должен содержать от 3 до 50 символов")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "Логин может содержать только латинские буквы, цифры, дефис и подчеркивание")
    private String username;

    @NotBlank(message = "ФИО обязательно")
    @Size(min = 2, max = 200, message = "ФИО должно содержать от 2 до 200 символов")
    private String fullName;

    @Email(message = "Некорректный формат email")
    @Size(max = 150, message = "Email не должен превышать 150 символов")
    private String email;

    @NotNull(message = "Роль обязательна")
    private Role role = Role.VIEWER;

    public CreateAdminUserRequest() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
