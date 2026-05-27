package com.openlab.qualitos.crypto.infrastructure;

import com.openlab.qualitos.crypto.domain.model.KemAlgorithm;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BouncyCastleKemProviderTest {

  @Test
  void encapsulateThenDecapsulateYieldsSameSecret() {
    BouncyCastleKemProvider provider = new BouncyCastleKemProvider();

    byte[] keyPair = provider.generateKeyPair();
    byte[] publicKey = BouncyCastleKemProvider.publicKeyOf(keyPair);
    byte[] privateKey = BouncyCastleKemProvider.privateKeyOf(keyPair);

    byte[] encapsulated = provider.encapsulate(publicKey);
    byte[] senderSecret = BouncyCastleKemProvider.sharedSecretOf(encapsulated);
    byte[] ciphertext = BouncyCastleKemProvider.ciphertextOf(encapsulated);

    byte[] receiverSecret = provider.decapsulate(privateKey, ciphertext);

    assertThat(provider.algorithm()).isEqualTo(KemAlgorithm.ML_KEM_768);
    assertThat(senderSecret).hasSize(32);
    assertThat(receiverSecret).isEqualTo(senderSecret);
  }

  @Test
  void differentKeyPairsProduceDifferentSecrets() {
    BouncyCastleKemProvider provider = new BouncyCastleKemProvider();
    byte[] kpA = provider.generateKeyPair();
    byte[] kpB = provider.generateKeyPair();

    byte[] encA = provider.encapsulate(BouncyCastleKemProvider.publicKeyOf(kpA));
    byte[] encB = provider.encapsulate(BouncyCastleKemProvider.publicKeyOf(kpB));

    assertThat(BouncyCastleKemProvider.sharedSecretOf(encA))
        .isNotEqualTo(BouncyCastleKemProvider.sharedSecretOf(encB));
  }
}
