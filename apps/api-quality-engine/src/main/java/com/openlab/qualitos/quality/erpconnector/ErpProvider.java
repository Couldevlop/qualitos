package com.openlab.qualitos.quality.erpconnector;

/**
 * ERP providers supportés en V1 (CLAUDE.md §13.3 : « ERP → indicateurs production,
 * achats, fournisseurs »).
 *
 * Chaque provider est couvert par un bean qui satisfait {@link ErpProviderClient}.
 * On ne dépend d'AUCUN SDK propriétaire lourd : chaque client est un appel REST/OData
 * générique paramétrable par baseUrl + chemins (defaults raisonnables par provider).
 *
 * Extension future : INFOR, ODOO, NETSUITE… (un nouveau bean SPI suffit).
 */
public enum ErpProvider {
    /** SAP S/4HANA — API OData (V2/V4), auth Basic. */
    SAP,
    /** Oracle Fusion Cloud — REST API, auth Basic/Bearer. */
    ORACLE_FUSION,
    /** Microsoft Dynamics 365 — OData v4, auth Bearer. */
    DYNAMICS
}
