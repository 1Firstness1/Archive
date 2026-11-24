package ru.mirea.archive.backend.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import ru.mirea.archive.backend.domain.RequestStatus;

import java.util.List;
import java.util.UUID;

public class BulkOperationRequest {

    public enum BulkAction {
        ARCHIVE,
        RESTORE,
        CHANGE_STATUS,
        ADD_TAG,
        REMOVE_TAG
    }

    @NotEmpty(message = "Список UUID не может быть пустым")
    private List<UUID> uuids;

    @NotNull(message = "Действие обязательно")
    private BulkAction action;

    private RequestStatus targetStatus;

    private String tag;

    public BulkOperationRequest() {
    }

    public List<UUID> getUuids() {
        return uuids;
    }

    public void setUuids(List<UUID> uuids) {
        this.uuids = uuids;
    }

    public BulkAction getAction() {
        return action;
    }

    public void setAction(BulkAction action) {
        this.action = action;
    }

    public RequestStatus getTargetStatus() {
        return targetStatus;
    }

    public void setTargetStatus(RequestStatus targetStatus) {
        this.targetStatus = targetStatus;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
