package com.openlab.qualitos.quality.docs;

/** Un fichier au-delà de la taille permise, ou qui dépasserait le quota du client. */
public class DocumentFileTooLargeException extends RuntimeException {
    public DocumentFileTooLargeException(String message) { super(message); }
}
