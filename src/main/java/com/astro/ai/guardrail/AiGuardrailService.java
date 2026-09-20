package com.astro.ai.guardrail;

import com.astro.ai.model.GuardrailResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class AiGuardrailService {

    private static final Logger log = LoggerFactory.getLogger(AiGuardrailService.class);

    private static final Pattern INJECTION_PATTERNS = Pattern.compile(
        "(?i)(ignore\\s+(all\\s+)?previous\\s+instructions"
        + "|system\\s+prompt\\s+override"
        + "|you\\s+are\\s+now\\s+in\\s+(dan|developer|jailbreak|unrestricted)\\s+mode"
        + "|bypass\\s+(safety|content|all)\\s+(filters|rules|guardrails)"
        + "|disregard\\s+(all\\s+)?prior\\s+(directives|constraints)"
        + "|print\\s+(the\\s+)?system\\s+prompt"
        + "|reveal\\s+(the\\s+)?hidden\\s+instructions"
        + "|\\bDAN\\s+mode\\b"
        + "|<\\s*system\\s*>"
        + "|\\[\\s*INST\\s*\\])"
    );

    private static final Pattern FATALISTIC_DEATH_PATTERNS = Pattern.compile(
        "(?i)\\b(exact\\s+date\\s+of\\s+death|when\\s+will\\s+i\\s+die|how\\s+will\\s+i\\s+die"
        + "|predict\\s+my\\s+death|suicide|end\\s+my\\s+life)\\b"
    );

    private static final Pattern MEDICAL_DIAGNOSIS_PATTERNS = Pattern.compile(
        "(?i)\\b(do\\s+i\\s+have\\s+cancer|diagnose\\s+my\\s+disease|cure\\s+my\\s+tumor"
        + "|stop\\s+taking\\s+my\\s+medication|medical\\s+treatment\\s+by\\s+astrology)\\b"
    );

    private static final Pattern FINANCIAL_GUARANTEE_PATTERNS = Pattern.compile(
        "(?i)\\b(guaranteed\\s+stock\\s+pick|insider\\s+trading|guaranteed\\s+crypto\\s+profit"
        + "|sure-shot\\s+lottery\\s+number)\\b"
    );

    public GuardrailResult validateInput(String userQuery) {
        if (userQuery == null || userQuery.isBlank()) {
            return GuardrailResult.block("Empty query", List.of("Query cannot be blank"));
        }

        // 1. Check Prompt Injection
        if (INJECTION_PATTERNS.matcher(userQuery).find()) {
            log.warn("Prompt injection attempt intercepted: {}", userQuery);
            return GuardrailResult.block("Prompt injection or instruction override attempt detected",
                List.of("Violates safety boundary: System prompt override / Jailbreak signature detected"));
        }

        List<String> notices = new ArrayList<>();

        // 2. Fatalistic Death Prediction Check
        if (FATALISTIC_DEATH_PATTERNS.matcher(userQuery).find()) {
            notices.add("Death prediction requested: Classical Jyotish ethical codes prohibit fatalistic death pronouncements.");
            String framed = sanitizeForDharmicEthics(userQuery,
                "Classical Parashari astrology teaches that longevity (*Ayurdaya*) is a dynamic interaction of karma, lifestyle, and divine grace. Astrological readings must focus on vitality preservation, health stewardship, and spiritual purpose rather than fatalistic timelines.");
            return GuardrailResult.framed(framed, "Fatalistic longevity query transformed into vitality stewardship.", notices);
        }

        // 3. Medical Diagnosis Check
        if (MEDICAL_DIAGNOSIS_PATTERNS.matcher(userQuery).find()) {
            notices.add("Medical diagnosis requested: Astrology is an archetypal energy mapping, not clinical diagnosis.");
            String framed = sanitizeForDharmicEthics(userQuery,
                "Astrological planetary houses (e.g. 6th house of ailments) indicate energetic predispositions and stress patterns, not clinical diagnoses. Please consult licensed medical professionals for health diagnoses and therapies.");
            return GuardrailResult.framed(framed, "Medical query transformed to energetic constitution reflection with healthcare disclaimer.", notices);
        }

        // 4. Financial Guarantee Check
        if (FINANCIAL_GUARANTEE_PATTERNS.matcher(userQuery).find()) {
            notices.add("Financial guarantee requested: Astrology indicates macro opportunity windows, not specific market guarantees.");
            String framed = sanitizeForDharmicEthics(userQuery,
                "Planetary wealth houses (2nd & 11th) map enterprise timing and risk appetite, but do not provide speculative financial advice or market guarantees. Practice disciplined financial risk management.");
            return GuardrailResult.framed(framed, "Financial query framed with risk disclosure.", notices);
        }

        return GuardrailResult.pass(cleanInput(userQuery));
    }

    public String cleanInput(String text) {
        if (text == null) return "";
        // Strip control characters, normalize excessive whitespace
        return text.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "").trim();
    }

    public String sanitizeOutput(String llmOutput) {
        if (llmOutput == null) return "";
        // Ensure standard ethical disclaimer is present if high-risk topics discussed
        String clean = llmOutput;
        if (clean.contains("guarantee") || clean.contains("100% certainty") || clean.contains("will definitely die")) {
            clean = clean.replaceAll("(?i)\\b(guarantee|100% certainty)\\b", "high traditional correlation")
                         .replaceAll("(?i)\\bwill definitely die\\b", "enters a transformative life cycle");
        }
        return clean;
    }

    private String sanitizeForDharmicEthics(String query, String guidance) {
        return query + "\n\n[Ethical Jyotish Guideline: " + guidance + "]";
    }
}
