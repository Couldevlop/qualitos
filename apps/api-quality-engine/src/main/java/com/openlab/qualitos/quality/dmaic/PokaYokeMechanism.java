package com.openlab.qualitos.quality.dmaic;

/**
 * Mécanisme physique/logique d'un Poka-Yoke.
 * Inspiré des classifications industrielles (Shingo).
 */
public enum PokaYokeMechanism {
    PHYSICAL_SHAPE,      // Forme empêchant montage incorrect
    INTERLOCK,           // Verrouillage mécanique/électrique
    LIMIT_SWITCH,        // Fin de course capteur
    SENSOR,              // Capteur (présence, T°, poids, ...)
    VISION,              // Caméra + détection d'image
    CHECKLIST,           // Liste de vérification obligatoire
    COLOR_CODING,        // Codage couleur
    POSITION_REFERENCE,  // Gabarit positionnel
    COUNTER,             // Compteur d'opérations
    SOFTWARE_VALIDATION, // Contrôle logiciel des entrées
    OTHER
}
