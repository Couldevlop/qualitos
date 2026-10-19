package com.openlab.qualitos.quality.docs;

/** Un fichier refusé au dépôt : type non admis, contenu qui ne correspond pas, fichier vide. */
public class DocumentFileRejectedException extends RuntimeException {
    public DocumentFileRejectedException(String message) { super(message); }
}
