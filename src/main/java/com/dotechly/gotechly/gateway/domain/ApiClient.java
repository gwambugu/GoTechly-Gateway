package com.dotechly.gotechly.gateway.domain;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * A machine-to-machine credential: a {@code clientId} / {@code clientSecret} pair exchanged for a
 * JWT at {@code POST /api/auth/token}.
 *
 * <p>Deliberately not a {@link User}. An API client has no password reset, no email, no AD identity
 * and no place in the human user lists — and every token it produces carries exactly one authority,
 * {@code ROLE_API_CLIENT}.
 *
 * <p>Only the BCrypt hash of the secret is ever stored. The plaintext is returned once at creation
 * and is unrecoverable afterwards; losing it means rotating.
 */
@Table("api_client")
public class ApiClient implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private Long id;

    @NotNull
    @Size(max = 64)
    @Column("client_id")
    private String clientId;

    /** BCrypt hash — never serialised, never returned by any endpoint. */
    @JsonIgnore
    @NotNull
    @Size(min = 60, max = 60)
    @Column("client_secret_hash")
    private String clientSecretHash;

    @NotNull
    @Size(max = 100)
    @Column("name")
    private String name;

    @Size(max = 255)
    @Column("description")
    private String description;

    @NotNull
    @Column("enabled")
    private boolean enabled = true;

    /** {@code null} means the credential does not expire on its own. */
    @Column("expires_at")
    private LocalDateTime expiresAt;

    @Size(max = 254)
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    private LocalDateTime createdDate;

    @Column("last_used_date")
    private LocalDateTime lastUsedDate;

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

    public String getClientSecretHash() {
        return clientSecretHash;
    }

    public void setClientSecretHash(String clientSecretHash) {
        this.clientSecretHash = clientSecretHash;
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

    /** Usable for authentication right now: not revoked and not past its expiry. */
    public boolean isUsableAt(LocalDateTime when) {
        return enabled && (expiresAt == null || expiresAt.isAfter(when));
    }
}
