package com.exercise.registry.integration;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import com.exercise.registry.tenant.Tenant;

@Entity 
@Table(name = "integrations")
public class Integration {
    @Id 
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private IntegrationProvider provider;

    @Column(name = "credential_ref", nullable = false, length = 255)
    private String credentialRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private IntegrationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version 
    @Column(nullable = false)
    private long version;

    protected Integration() {
        // JPA only
    }

    public Integration(Tenant tenant, String displayName, 
        IntegrationProvider provider, String credentialRef
    ) {
        this.id = UUID.randomUUID();
        this.tenant = tenant;
        this.displayName = displayName;
        this.provider = provider;
        this.credentialRef = credentialRef;
        this.status = IntegrationStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void update(String displayName, IntegrationProvider provider, 
        String credentialRef, IntegrationStatus status
    ) {
        this.displayName = displayName;
        this.provider = provider;
        this.credentialRef = credentialRef;
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }
    public Tenant getTenant() {
        return tenant;
    }
    public String getDisplayName() {
        return displayName;
    }
    public IntegrationProvider getProvider() {
        return provider;
    }
    public String getCredentialRef() {
        return credentialRef;
    }
    public IntegrationStatus getStatus() {
        return status;
    }
    public Instant getCreatedAt() {
        return createdAt;
    }
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    public long getVersion() {
        return version;
    }
}
