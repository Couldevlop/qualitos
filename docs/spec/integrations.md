# QualitOS — Integrations tierces

> Chapitre de la specification produit, extrait de `CLAUDE.md` pour
> n alourdir que les sessions qui en ont besoin. La numerotation des sections
> est celle de la specification.

---

## 13. Intégrations tierces (interopérabilité)

### 13.1 API REST publique

- OpenAPI 3.1 documentée, versionnée (`/api/v1/`).
- Authentification OAuth2 client_credentials.
- Pagination cursor-based, idempotency keys, rate-limiting par tenant.

### 13.2 Webhooks sortants

- Événements souscriptibles : `cycle.created`, `audit.signed`, `nonconformity.detected`, `kpi.threshold.breached`, etc.
- Signatures HMAC-SHA256 + horodatage anti-replay.
- Retry exponentiel, DLQ Kafka.

### 13.3 Connecteurs natifs (V1)

- **ITSM** : ServiceNow, Jira Service Management, Zendesk → import incidents, export actions.
- **ERP** : SAP, Oracle Fusion, Microsoft Dynamics → indicateurs production, achats, fournisseurs.
- **MES / IoT** : OPC-UA, MQTT, Sparkplug B → données capteurs pour DMAIC/SPC.
- **EHR/HIS** (santé) : HL7 FHIR R5, DICOM.
- **Bureautique** : Microsoft 365, Google Workspace.
- **Communication** : Slack, Teams, Mattermost.
- **GED** : SharePoint, Alfresco, Nextcloud, Confluence.
- **CMMS / GMAO** : Maximo, SAP PM, Carl Source.
- **PLM** : Windchill, Teamcenter, Aras.
- **BI** : Power BI, Tableau, Qlik (export embed).

### 13.4 Keycloak SPI LDAP custom

- SPI Java implémentant `UserStorageProviderFactory` pour brancher des annuaires LDAP non standards.
- Mapping d'attributs configurable, cache local, fallback transparent.
- Support **Active Directory**, **OpenLDAP**, **FreeIPA** out-of-the-box.

---

