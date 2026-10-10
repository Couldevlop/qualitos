package com.openlab.qualitos.quality.authz.web;

import com.openlab.qualitos.quality.authz.domain.Permission;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * L'action qu'un point d'entrée exige, réglable par client (ADR 0078).
 *
 * <p>Remplace les listes de rôles écrites en dur ({@code hasAnyRole(...)}) : le
 * droit se décide dans la base du client, ou à défaut selon le catalogue livré.
 * Vérifiée par {@link PermissionInterceptor} AVANT la lecture du corps de la
 * requête (même raison que l'ADR 0065 : un appelant sans droit reçoit 403, pas
 * 400 sur la forme de ce qu'il envoie).
 *
 * <p>Sur une méthode ou sur sa classe ; celle de la méthode l'emporte.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequiresPermission {

    Permission value();
}
