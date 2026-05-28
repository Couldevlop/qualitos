package com.openlab.qualitos.blockchain.anchor;

import org.hyperledger.fabric.client.CommitException;
import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.GatewayException;
import org.hyperledger.fabric.client.Status;
import org.hyperledger.fabric.client.SubmittedTransaction;
import org.hyperledger.fabric.client.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * Invoque le chaincode {@code qualitos-anchor} via le Fabric Gateway SDK.
 *
 * <p>{@code anchor} utilise le flux propose → endorse → submitAsync pour récupérer
 * le <b>txId Fabric</b> et attendre la confirmation de commit. {@code verify}
 * évalue (lecture, pas de commit). RGPD §11.3 : seul un hash (Merkle root) transite.
 */
@Service
public class FabricAnchorService {

    private static final Logger log = LoggerFactory.getLogger(FabricAnchorService.class);

    private final Contract contract;

    public FabricAnchorService(Contract contract) {
        this.contract = contract;
    }

    public AnchorResult anchor(String tenantId, String merkleRoot, String timestamp, int eventCount) {
        try {
            Transaction txn = contract.newProposal("AnchorAudit")
                    .addArguments(tenantId, merkleRoot, timestamp, Integer.toString(eventCount))
                    .build()
                    .endorse();
            SubmittedTransaction submitted = txn.submitAsync();
            String txId = submitted.getTransactionId();
            byte[] result = submitted.getResult();

            Status status = submitted.getStatus();   // bloque jusqu'au commit
            if (!status.isSuccessful()) {
                throw new FabricInvocationException(
                        "commit rejeté (code=" + status.getCode() + ") tx=" + txId);
            }
            log.info("[fabric] anchored tenant={} root={} → tx={}", tenantId, merkleRoot, txId);
            return new AnchorResult(txId, new String(result, StandardCharsets.UTF_8));
        } catch (GatewayException | CommitException e) {
            throw new FabricInvocationException("ancrage Fabric échoué : " + e.getMessage(), e);
        }
    }

    public String verify(String tenantId, String merkleRoot) {
        try {
            byte[] result = contract.evaluateTransaction("VerifyEvidence", tenantId, merkleRoot);
            return new String(result, StandardCharsets.UTF_8);
        } catch (GatewayException e) {
            throw new FabricInvocationException("vérification Fabric échouée : " + e.getMessage(), e);
        }
    }

    /** Résultat d'un ancrage : txId Fabric + enregistrement JSON renvoyé par le chaincode. */
    public record AnchorResult(String txId, String recordJson) {}
}
