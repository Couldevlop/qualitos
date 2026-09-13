package com.openlab.qualitos.quality.nonconformity.eightd.application;

import com.openlab.qualitos.quality.nonconformity.eightd.domain.EightDSnapshot;

/**
 * Port — met en page le rapport 8D et rend le PDF.
 *
 * <p><b>Contrat déterministe</b>, et ce n'est pas un détail : pour la même
 * instantané et la même URL de vérification, le rendu doit produire les MÊMES
 * octets, à jamais. L'empreinte scellée à l'émission est calculée sur ces octets ;
 * un rendu qui dépendrait de l'horloge, d'une police installée ou d'une lecture en
 * base rendrait la preuve invérifiable dès le lendemain.
 *
 * <p>L'implémentation (PDFBox + ZXing) vit dans l'infrastructure : le domaine et le
 * cas d'usage n'ont aucune dépendance PDF.
 */
public interface EightDPdfRenderPort {

    /**
     * @param snapshot  le contenu figé des huit disciplines
     * @param verifyUrl l'URL publique encodée dans le QR code
     * @return les octets du PDF, non vides
     */
    byte[] render(EightDSnapshot snapshot, String verifyUrl);
}
