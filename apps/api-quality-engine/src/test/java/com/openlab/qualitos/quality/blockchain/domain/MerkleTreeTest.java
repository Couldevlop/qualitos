package com.openlab.qualitos.quality.blockchain.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MerkleTreeTest {

    @Test
    void single_leaf_rootIsSha256OfLeaf() {
        String leaf = "abc";
        assertThat(MerkleTree.root(List.of(leaf))).isEqualTo(MerkleTree.sha256(leaf));
    }

    @Test
    void same_input_produces_same_root() {
        List<String> leaves = List.of("a", "b", "c", "d");
        assertThat(MerkleTree.root(leaves)).isEqualTo(MerkleTree.root(leaves));
    }

    @Test
    void different_input_produces_different_root() {
        List<String> a = List.of("a", "b", "c", "d");
        List<String> b = List.of("a", "b", "c", "e");
        assertThat(MerkleTree.root(a)).isNotEqualTo(MerkleTree.root(b));
    }

    @Test
    void odd_count_duplicates_last_leaf_consistently() {
        // 3 feuilles ⇒ niveau 1 a 2 hashes (avec duplication de la dernière feuille
        // au niveau des feuilles). Le résultat doit rester stable.
        String r1 = MerkleTree.root(List.of("a", "b", "c"));
        String r2 = MerkleTree.root(List.of("a", "b", "c"));
        assertThat(r1).isEqualTo(r2);
        assertThat(r1).matches("^[0-9a-f]{64}$");
    }

    @Test
    void root_isHex64() {
        assertThat(MerkleTree.root(List.of("x"))).matches("^[0-9a-f]{64}$");
        assertThat(MerkleTree.root(List.of("x", "y", "z"))).matches("^[0-9a-f]{64}$");
    }

    @Test
    void empty_or_null_throws() {
        assertThatThrownBy(() -> MerkleTree.root(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MerkleTree.root(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void order_matters() {
        assertThat(MerkleTree.root(List.of("a", "b")))
                .isNotEqualTo(MerkleTree.root(List.of("b", "a")));
    }
}
