// Package main is the qualitos-anchor chaincode: a permissioned Hyperledger
// Fabric smart contract that notarises QualitOS audit Merkle roots.
//
// RGPD / CLAUDE.md §11.3: ONLY hashes go on-chain — never personal data.
// A record holds a Merkle root (SHA-256 hash of a batch of audit events), the
// tenant id (an organisation UUID, not a natural person), a client timestamp,
// the event count and the Fabric transaction id.
package main

import (
	"encoding/json"
	"fmt"

	"github.com/hyperledger/fabric-contract-api-go/contractapi"
)

// objectType namespaces composite keys in world state.
const objectType = "anchor"

// AnchorContract exposes AnchorAudit / VerifyEvidence (CLAUDE.md §11.3).
type AnchorContract struct {
	contractapi.Contract
}

// AnchorRecord is the on-chain notarisation record. Hashes only — no PII.
type AnchorRecord struct {
	MerkleRoot string `json:"merkleRoot"`
	TenantID   string `json:"tenantId"`
	AnchoredAt string `json:"anchoredAt"` // client RFC3339 timestamp
	EventCount int    `json:"eventCount"`
	TxID       string `json:"txId"` // Fabric transaction id (set on first anchor)
}

// AnchorAudit notarises a Merkle root for a tenant. Idempotent: anchoring an
// already-anchored (tenant, root) pair returns the ORIGINAL record unchanged,
// preserving first-write audit semantics.
func (c *AnchorContract) AnchorAudit(
	ctx contractapi.TransactionContextInterface,
	tenantID string,
	merkleRoot string,
	timestamp string,
	eventCount int,
) (*AnchorRecord, error) {
	if tenantID == "" || merkleRoot == "" {
		return nil, fmt.Errorf("tenantId and merkleRoot are required")
	}

	key, err := ctx.GetStub().CreateCompositeKey(objectType, []string{tenantID, merkleRoot})
	if err != nil {
		return nil, fmt.Errorf("composite key: %w", err)
	}

	existing, err := ctx.GetStub().GetState(key)
	if err != nil {
		return nil, fmt.Errorf("read state: %w", err)
	}
	if existing != nil {
		var rec AnchorRecord
		if err := json.Unmarshal(existing, &rec); err != nil {
			return nil, fmt.Errorf("decode existing record: %w", err)
		}
		return &rec, nil // already anchored — idempotent
	}

	rec := AnchorRecord{
		MerkleRoot: merkleRoot,
		TenantID:   tenantID,
		AnchoredAt: timestamp,
		EventCount: eventCount,
		TxID:       ctx.GetStub().GetTxID(),
	}
	b, err := json.Marshal(rec)
	if err != nil {
		return nil, fmt.Errorf("encode record: %w", err)
	}
	if err := ctx.GetStub().PutState(key, b); err != nil {
		return nil, fmt.Errorf("write state: %w", err)
	}
	return &rec, nil
}

// VerifyEvidence returns the notarisation record for a (tenant, root) pair, or an
// error if it was never anchored. Tenant-scoped: a tenant can only verify its own
// roots (OWASP A01 — the composite key embeds the tenant id).
func (c *AnchorContract) VerifyEvidence(
	ctx contractapi.TransactionContextInterface,
	tenantID string,
	merkleRoot string,
) (*AnchorRecord, error) {
	key, err := ctx.GetStub().CreateCompositeKey(objectType, []string{tenantID, merkleRoot})
	if err != nil {
		return nil, fmt.Errorf("composite key: %w", err)
	}
	data, err := ctx.GetStub().GetState(key)
	if err != nil {
		return nil, fmt.Errorf("read state: %w", err)
	}
	if data == nil {
		return nil, fmt.Errorf("not anchored: tenant=%s root=%s", tenantID, merkleRoot)
	}
	var rec AnchorRecord
	if err := json.Unmarshal(data, &rec); err != nil {
		return nil, fmt.Errorf("decode record: %w", err)
	}
	return &rec, nil
}

func main() {
	cc, err := contractapi.NewChaincode(&AnchorContract{})
	if err != nil {
		panic(fmt.Sprintf("create qualitos-anchor chaincode: %v", err))
	}
	if err := cc.Start(); err != nil {
		panic(fmt.Sprintf("start qualitos-anchor chaincode: %v", err))
	}
}
