package com.openlab.qualitos.quality.audit;

/**
 * Conclusion portée par le rapport d'audit (ISO 19011 §6.5).
 *
 * <p>Trois issues, pas davantage : c'est ce qu'un organisme certificateur ou une
 * direction attend en dernière ligne d'un rapport. Pour un audit de
 * certification, elles se lisent « certification recommandée », « recommandée
 * avec réserves » et « non recommandée » ; pour un audit interne, « système
 * satisfaisant », « satisfaisant avec réserves », « non satisfaisant ».
 */
public enum AuditConclusion { FAVORABLE, FAVORABLE_WITH_RESERVATIONS, UNFAVORABLE }
