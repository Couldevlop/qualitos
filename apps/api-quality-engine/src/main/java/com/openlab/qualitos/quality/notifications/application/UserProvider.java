package com.openlab.qualitos.quality.notifications.application;

/** Fournit l'identifiant de l'utilisateur authentifié courant (sub du JWT). */
public interface UserProvider {

    /** Sujet (sub) du JWT courant ; lève si absent (requête non authentifiée). */
    String requireUserId();
}
