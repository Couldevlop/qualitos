package com.openlab.qualitos.quality.academy.domain;

/**
 * Nature du contenu pédagogique d'une leçon (§19.3 — « vidéos + quiz + simulations »).
 */
public enum LessonContentType {
    /** Contenu textuel riche (markdown/HTML assaini côté présentation). */
    TEXT,
    /** Lien vidéo (mediaUrl). */
    VIDEO,
    /** Simulateur interactif (ex. « Construis un Ishikawa »). */
    SIMULATION,
    /** Ressource externe (lien). */
    EXTERNAL
}
