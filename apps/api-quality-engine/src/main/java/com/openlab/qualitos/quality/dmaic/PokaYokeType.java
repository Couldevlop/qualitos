package com.openlab.qualitos.quality.dmaic;

/**
 * Type de détection du dispositif Poka-Yoke.
 * - PREVENTION : empêche l'erreur de se produire (interlock, fool-proof shape, ...).
 * - DETECTION  : détecte l'erreur après occurrence (alarm, sensor, vision, ...).
 */
public enum PokaYokeType {
    PREVENTION, DETECTION
}
