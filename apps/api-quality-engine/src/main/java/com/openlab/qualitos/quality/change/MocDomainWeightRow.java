package com.openlab.qualitos.quality.change;

import com.openlab.qualitos.quality.change.domain.ImpactDomain;
import jakarta.persistence.*;

import java.util.UUID;

/** Le poids d'un domaine d'impact pour un client. */
@Entity
@Table(name = "moc_domain_weights",
        uniqueConstraints = @UniqueConstraint(name = "uq_moc_domain_weights", columnNames = {"tenant_id", "domain"}))
public class MocDomainWeightRow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ImpactDomain domain;

    @Column(nullable = false)
    private int weight;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public ImpactDomain getDomain() { return domain; }
    public void setDomain(ImpactDomain domain) { this.domain = domain; }
    public int getWeight() { return weight; }
    public void setWeight(int weight) { this.weight = weight; }
}
