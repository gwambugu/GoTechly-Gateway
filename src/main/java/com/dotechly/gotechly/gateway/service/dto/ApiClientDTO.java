package com.dotechly.gotechly.gateway.service.dto;

import com.dotechly.gotechly.gateway.domain.ApiClient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * An API client as seen by the admin API. Carries no secret in any form — not the plaintext (which
 * is not stored) and not the hash.
 */
public class ApiClientDTO {

    private Long id;

    private String clientId;

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 255)
    private String description;

    private boolean enabled;

    private LocalDateTime expiresAt;

    private String createdBy;

    private LocalDateTime createdDate;

    private LocalDateTime lastUsedDate;

    /** True once {@code expiresAt} has passed — the row stays, but the credential no longer works. */
    private boolean expired;

    public ApiClientDTO() {
        // for Jackson
    }

    public ApiClientDTO(ApiClient client) {
        this.id = client.getId();
        this.clientId = client.getClientId();
        this.name = client.getName();
        this.description = client.getDescription();
        this.enabled = client.isEnabled();
        this.expiresAt = client.getExpiresAt();
        this.createdBy = client.getCreatedBy();
        this.createdDate = client.getCreatedDate();
        this.lastUsedDate = client.getLastUsedDate();
        this.expired = client.getExpiresAt() != null && !client.getExpiresAt().isAfter(LocalDateTime.now());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getLastUsedDate() {
        return lastUsedDate;
    }

    public void setLastUsedDate(LocalDateTime lastUsedDate) {
        this.lastUsedDate = lastUsedDate;
    }

    public boolean isExpired() {
        return expired;
    }

    public void setExpired(boolean expired) {
        this.expired = expired;
    }
}
