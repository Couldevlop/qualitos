package com.openlab.qualitos.crypto.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KeyMaterialTest {

  @Test
  void acceptsNonEmptyKeys() {
    KeyMaterial km = new KeyMaterial(new byte[]{1}, new byte[]{2});
    assertThat(km.publicKey()).containsExactly(1);
    assertThat(km.privateKey()).containsExactly(2);
  }

  @Test
  void rejectsNullPublicKey() {
    assertThatThrownBy(() -> new KeyMaterial(null, new byte[]{1}))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsEmptyPublicKey() {
    assertThatThrownBy(() -> new KeyMaterial(new byte[]{}, new byte[]{1}))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNullPrivateKey() {
    assertThatThrownBy(() -> new KeyMaterial(new byte[]{1}, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsEmptyPrivateKey() {
    assertThatThrownBy(() -> new KeyMaterial(new byte[]{1}, new byte[]{}))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
