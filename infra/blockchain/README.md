# QualitOS — Ancrage Hyperledger Fabric (WS4, ADR 0012 Phase B)

Réseau Fabric permissionné + chaincode `qualitos-anchor` qui notarise les
**Merkle roots** des événements d'audit QualitOS. Conforme RGPD (§11.3) : seuls
des **hashes** sont écrits on-chain, jamais de données personnelles.

```
api-quality-engine                 blockchain-service              Fabric
─────────────────────              ──────────────────              ──────
FabricBlockchainAnchorAdapter  →   POST /internal/v1/anchor    →   AnchorAudit(tenant, root, ts, n)
  (@Profile "fabric", HTTP)        (Fabric Gateway SDK, mTLS)       VerifyEvidence(tenant, root)
  repli → SignedAnchorAdapter      GET  /internal/v1/verify    →   chaincode qualitos-anchor (Go)
```

L'engine ne porte **aucune** dépendance Fabric : il appelle `blockchain-service`
en HTTP (cf. `FabricGatewayClient`). Le SDK Fabric (gRPC/protobuf/MSP) est isolé
dans `apps/blockchain-service`.

## Composants

| Élément | Emplacement |
| --- | --- |
| Chaincode Go `qualitos-anchor` | `infra/blockchain/chaincode/qualitos-anchor/` |
| Service passerelle (Spring Boot + Fabric Gateway SDK) | `apps/blockchain-service/` |
| Adapter engine (HTTP, `@Profile("fabric")`) | `apps/api-quality-engine/.../blockchain/infrastructure/FabricBlockchainAnchorAdapter.java` |

## 1. Lancer un réseau Fabric de test

On s'appuie sur le **`test-network` de `fabric-samples`** (référence Hyperledger),
plutôt que de réinventer un compose fragile.

```bash
# Pré-requis : Docker, Go 1.21+, binaires Fabric 2.5
curl -sSL https://bit.ly/2ysbOFE | bash -s -- 2.5.5 1.5.7   # installe fabric-samples + binaires
cd fabric-samples/test-network

# Démarre orderer + 2 peers (Org1, Org2) + crée le canal "qualitos"
./network.sh up createChannel -c qualitos -ca
```

## 2. Déployer le chaincode

```bash
# Depuis fabric-samples/test-network, en pointant le code de ce repo :
./network.sh deployCC \
  -c qualitos \
  -ccn qualitos-anchor \
  -ccp /chemin/vers/qualitOs/infra/blockchain/chaincode/qualitos-anchor \
  -ccl go
```

Vérification rapide (CLI peer) :

```bash
# Ancrer
peer chaincode invoke -C qualitos -n qualitos-anchor \
  -c '{"function":"AnchorAudit","Args":["00000000-0000-0000-0000-000000000099","abcd1234","2026-05-28T10:00:00Z","12"]}' \
  --waitForEvent ...   # (endorsement Org1+Org2, TLS)

# Vérifier
peer chaincode query -C qualitos -n qualitos-anchor \
  -c '{"function":"VerifyEvidence","Args":["00000000-0000-0000-0000-000000000099","abcd1234"]}'
```

## 3. Connecter `blockchain-service`

Renseigner les chemins MSP (générés par `test-network` sous
`organizations/peerOrganizations/org1.example.com/...`) via les variables
d'environnement attendues par `apps/blockchain-service` :

```
FABRIC_PEER_ENDPOINT=localhost:7051
FABRIC_PEER_HOST_ALIAS=peer0.org1.example.com
FABRIC_TLS_CERT=.../org1.example.com/peers/peer0.../tls/ca.crt
FABRIC_MSP_ID=Org1MSP
FABRIC_CERT=.../users/User1@org1.example.com/msp/signcerts/cert.pem
FABRIC_KEY=.../users/User1@org1.example.com/msp/keystore/<sk>
FABRIC_CHANNEL=qualitos
FABRIC_CHAINCODE=qualitos-anchor
```

Puis lancer le service (port 8090) et activer le profil `fabric` sur l'engine :

```bash
# engine : bascule l'ancrage sur Fabric (sinon SignedAnchorAdapter Phase A par défaut)
SPRING_PROFILES_ACTIVE=fabric QUALITOS_BLOCKCHAIN_SERVICE_URL=http://localhost:8090 \
  mvn -pl apps/api-quality-engine spring-boot:run
```

Si `blockchain-service` / le réseau Fabric est indisponible, l'engine **retombe
automatiquement** sur la notarisation signée Phase A (reçus chaînés vérifiables) —
l'ancrage n'est jamais perdu.

## Sécurité (CLAUDE.md §9.8, §11.3)

- **mTLS** obligatoire engine ↔ blockchain-service ↔ peers Fabric en prod.
- **Hashes uniquement** on-chain (Merkle roots) — aucune donnée personnelle.
- **Isolation tenant** : la clé composite du chaincode `(objectType, tenantId, root)`
  empêche un tenant de vérifier le root d'un autre (OWASP A01).
- Idempotence : ré-ancrer un `(tenant, root)` déjà présent renvoie l'enregistrement
  d'origine (sémantique first-write d'audit).
