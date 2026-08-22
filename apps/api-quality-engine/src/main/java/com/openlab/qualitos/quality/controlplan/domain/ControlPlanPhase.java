package com.openlab.qualitos.quality.controlplan.domain;

/**
 * Phase de vie couverte par le plan, au sens AIAG.
 *
 * <p>Un même produit en porte légitimement plusieurs en même temps : la
 * pré-série contrôle davantage que la série, parce que le procédé n'a pas
 * encore fait ses preuves.
 */
public enum ControlPlanPhase { PROTOTYPE, PRE_LAUNCH, PRODUCTION }
