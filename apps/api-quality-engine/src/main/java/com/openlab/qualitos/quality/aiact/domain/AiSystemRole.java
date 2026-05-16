package com.openlab.qualitos.quality.aiact.domain;

/**
 * Rôle de l'organisation vis-à-vis du système d'IA (Art. 3 AI Act).
 * <ul>
 *   <li>PROVIDER — fournisseur qui développe ou fait développer un système.</li>
 *   <li>DEPLOYER — déployeur qui utilise un système sous son autorité (Art. 26).</li>
 *   <li>IMPORTER — importateur dans l'UE depuis un pays tiers.</li>
 *   <li>DISTRIBUTOR — distributeur (autre que provider/importer).</li>
 * </ul>
 */
public enum AiSystemRole {
    PROVIDER,
    DEPLOYER,
    IMPORTER,
    DISTRIBUTOR
}
