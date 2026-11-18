# QualitOS — Architecture technique - vue d ensemble

> Chapitre de la specification produit, extrait de `CLAUDE.md` pour
> n alourdir que les sessions qui en ont besoin. La numerotation des sections
> est celle de la specification.

---

### 10.1 Vue d'ensemble (modulith → microservices)

```
┌─────────────────────────────────────────────────────────────┐
│  Angular 18 SPA + PWA (Material 3, Design System QualitOS)  │
└──────────────────────────────┬──────────────────────────────┘
                               │ HTTPS / mTLS
┌──────────────────────────────▼──────────────────────────────┐
│         API Gateway (Spring Cloud Gateway + WAF)            │
│   Rate-limit · OAuth2/OIDC · Validation OpenAPI · CORS      │
└──┬────────────┬────────────┬────────────┬─────────┬─────────┘
   │            │            │            │         │
┌──▼──┐     ┌───▼───┐    ┌───▼───┐    ┌───▼───┐ ┌───▼────┐
│Core │     │Quality│    │ IA    │    │Block- │ │Industry│
│API  │     │Engine │    │Service│    │chain  │ │Adapter │
│J25  │     │J25    │    │Python │    │Service│ │Service │
└──┬──┘     └───┬───┘    └───┬───┘    └───┬───┘ └───┬────┘
   │            │            │            │         │
   └────┬───────┴────────────┴────────────┴─────────┘
        │
   ┌────▼────────┬───────────┬───────────┬─────────────┐
   │ PostgreSQL  │ MongoDB 7 │ Redis 7   │ TimescaleDB │
   │ + RLS multi-│ (docs/IA  │ (cache,   │ (KPI time-  │
   │ tenant      │ logs)     │ sessions) │ series)     │
   └─────────────┴───────────┴───────────┴─────────────┘
        │
   ┌────▼─────────┐  ┌──────────────┐  ┌────────────────┐
   │ Keycloak 25  │  │ Kafka 3.8    │  │ Hyperledger    │
   │ + SPI LDAP   │  │ + Schema Reg │  │ Fabric 2.5     │
   │ + OIDC/OAuth2│  │ (event src)  │  │                │
   └──────────────┘  └──────────────┘  └────────────────┘
        │
   ┌────▼──────────────────────────────────────────────────┐
   │  Connecteurs : MES (OPC-UA, MQTT) · ERP (SAP, Oracle) │
   │  ITSM (ServiceNow, Jira) · EHR (HL7 FHIR) · IoT       │
   │  GED (SharePoint, Alfresco) · Comm (Slack, Teams)     │
   └───────────────────────────────────────────────────────┘
```

