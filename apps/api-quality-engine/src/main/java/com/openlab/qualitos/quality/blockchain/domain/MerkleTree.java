package com.openlab.qualitos.quality.blockchain.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Arbre de Merkle SHA-256 sur des chaînes hex. Logique pure — testable
 * sans Spring. Convention :
 *  - Les feuilles sont hashées (sha256(hex)) avant la première fusion.
 *  - Niveau impair : la dernière feuille est dupliquée (style Bitcoin).
 *  - Une seule feuille → root = sha256(leaf).
 */
public final class MerkleTree {

    private MerkleTree() {}

    public static String root(List<String> leavesHex) {
        if (leavesHex == null || leavesHex.isEmpty()) {
            throw new IllegalArgumentException("Merkle root requires at least one leaf");
        }
        List<String> level = new ArrayList<>(leavesHex.size());
        for (String leaf : leavesHex) level.add(sha256(leaf));
        while (level.size() > 1) {
            List<String> next = new ArrayList<>((level.size() + 1) / 2);
            for (int i = 0; i < level.size(); i += 2) {
                String left = level.get(i);
                String right = i + 1 < level.size() ? level.get(i + 1) : left;
                next.add(sha256(left + right));
            }
            level = next;
        }
        return level.get(0);
    }

    static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(d.length * 2);
            for (byte b : d) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
