package com.openlab.qualitos.quality.controlplan.domain;

/**
 * Ce que la ligne surveille : une caractéristique du produit (une cote, un
 * couple de serrage constaté) ou du procédé (une température de four, une
 * pression). La distinction pilote le moment du contrôle.
 */
public enum CharacteristicType { PRODUCT, PROCESS }
