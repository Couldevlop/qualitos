package com.openlab.qualitos.quality.ai.guard;

/**
 * Prompt dépassant la taille maximale admise. Borne l'entrée avant qu'elle
 * n'atteigne le modèle. Mappé en HTTP 413. OWASP LLM04 (charge utile abusive)
 * et durcissement LLM01.
 */
public class AiPromptTooLargeException extends AiGuardException {

    public AiPromptTooLargeException(String message) {
        super(message);
    }
}
