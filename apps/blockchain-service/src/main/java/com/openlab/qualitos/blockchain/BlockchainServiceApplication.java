package com.openlab.qualitos.blockchain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Passerelle Hyperledger Fabric de QualitOS (ADR 0012 Phase B). Expose à l'engine
 * (api-quality-engine) un ancrage et une vérification des Merkle roots d'audit via
 * le chaincode {@code qualitos-anchor}. Le SDK Fabric est isolé ici.
 */
@SpringBootApplication
public class BlockchainServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlockchainServiceApplication.class, args);
    }
}
