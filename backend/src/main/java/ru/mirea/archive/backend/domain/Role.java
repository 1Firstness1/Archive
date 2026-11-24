package ru.mirea.archive.backend.domain;

public enum Role {
    VIEWER,
    EDITOR,
    ADMIN;

    public String toAuthority() {
        return "ROLE_" + this.name();
    }
}
