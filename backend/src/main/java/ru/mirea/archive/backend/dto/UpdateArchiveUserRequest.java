package ru.mirea.archive.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateArchiveUserRequest {

    @NotBlank(message = "ФИО заявителя обязательно")
    @Size(min = 2, max = 200, message = "ФИО должно содержать от 2 до 200 символов")
    private String fullName;

    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный формат email")
    @Size(max = 150, message = "Email не должен превышать 150 символов")
    private String email;

    @Size(max = 30, message = "Телефон не должен превышать 30 символов")
    private String phone;

    @Size(max = 150, message = "Отдел не должен превышать 150 символов")
    private String department;

    private boolean active;

    public UpdateArchiveUserRequest() {
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
