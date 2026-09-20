package com.astro.ai.vector;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

@Component
public class EmbeddingEngine {

    public static final int DIMENSIONS = 128;

    /**
     * Generates a high-density, normalized 128-dimensional embedding vector
     * using semantic astrological token weighting and hash projection.
     */
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            return new float[DIMENSIONS];
        }

        float[] vector = new float[DIMENSIONS];
        String normalized = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s]", " ");
        String[] tokens = normalized.split("\\s+");

        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            if (token.isBlank()) continue;

            // Semantic weight multiplier for classical astrological keywords
            float weight = computeTokenWeight(token);

            // Hash token to multiple dimensions for dense projection
            byte[] hash = sha256(token);
            for (int d = 0; d < 8; d++) {
                int index = (Byte.toUnsignedInt(hash[d * 2]) | (Byte.toUnsignedInt(hash[d * 2 + 1]) << 8)) % DIMENSIONS;
                float sign = ((hash[d] & 1) == 0) ? 1.0f : -1.0f;
                vector[index] += sign * weight;
            }

            // Word pair / bigram projection for contextual semantic continuity
            if (i < tokens.length - 1) {
                String bigram = token + "_" + tokens[i + 1];
                byte[] bgHash = sha256(bigram);
                int bgIndex = Byte.toUnsignedInt(bgHash[0]) % DIMENSIONS;
                vector[bgIndex] += 1.5f * weight;
            }
        }

        // L2-Normalize vector so dot product equals exact cosine similarity
        double norm = 0.0;
        for (float val : vector) {
            norm += val * val;
        }
        norm = Math.sqrt(norm);
        if (norm > 1e-9) {
            for (int i = 0; i < DIMENSIONS; i++) {
                vector[i] = (float) (vector[i] / norm);
            }
        }

        return vector;
    }

    public double cosineSimilarity(float[] vecA, float[] vecB) {
        if (vecA == null || vecB == null || vecA.length != vecB.length) return 0.0;
        double dot = 0.0;
        for (int i = 0; i < vecA.length; i++) {
            dot += vecA[i] * vecB[i];
        }
        return Math.max(-1.0, Math.min(1.0, dot));
    }

    private float computeTokenWeight(String token) {
        return switch (token) {
            case "jupiter", "guru", "saturn", "shani", "mercury", "budh", "venus", "shukra",
                 "mars", "mangal", "sun", "surya", "moon", "chandra", "rahu", "ketu" -> 3.0f;
            case "lagna", "ascendant", "house", "dasha", "mahadasha", "transit", "gochar",
                 "nakshatra", "navamsa", "varga", "shadbala", "ashtakavarga" -> 2.5f;
            case "raja", "dhana", "yoga", "bhrigu", "nadi", "parashara", "jaimini", "karma",
                 "pitra", "remedy", "gemstone", "rashi", "aries", "taurus", "gemini", "cancer",
                 "leo", "virgo", "libra", "scorpio", "sagittarius", "capricorn", "aquarius", "pisces" -> 2.2f;
            case "career", "wealth", "marriage", "health", "longevity", "destiny", "spiritual" -> 2.0f;
            default -> 1.0f;
        };
    }

    private byte[] sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm missing", e);
        }
    }
}
