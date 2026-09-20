package com.astro.ai.vector;

import com.astro.ai.model.VectorDocument;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class InMemoryVectorStore implements VectorStore {

    private static final Logger log = LoggerFactory.getLogger(InMemoryVectorStore.class);

    private final EmbeddingEngine embeddingEngine;
    private final List<VectorDocument> documents = new CopyOnWriteArrayList<>();

    public InMemoryVectorStore(EmbeddingEngine embeddingEngine) {
        this.embeddingEngine = embeddingEngine;
    }

    @PostConstruct
    public void initVedicCorpus() {
        log.info("Initializing classical Vedic astrological vector knowledge base...");
        List<VectorDocument> initialCorpus = List.of(
            // --- Parashari Tradition ---
            new VectorDocument("bphs-raja-01", "Dharma-Karmadhipati Raja Yoga",
                "When the lord of the 9th house (Dharma) and the lord of the 10th house (Karma) form a conjunction, mutual aspect, or parivartana (exchange of signs) in an auspicious house, it generates the supreme Raja Yoga granting executive leadership, enduring public renown, and ethical material wealth.",
                "parashari", "yogas", "Brihat Parashara Hora Shastra Ch. 41 Sloka 12"),

            new VectorDocument("bphs-dhana-02", "Maha Dhana Yoga (Wealth Accumulation)",
                "Conjunction or mutual aspect between the lord of the 2nd house (accumulated wealth) and the lord of the 11th house (regular cashflow and highest gains) in a Kendra or Trikona house creates persistent, unassailable financial prosperity across life cycles.",
                "parashari", "wealth", "Brihat Parashara Hora Shastra Ch. 42 Sloka 3"),

            new VectorDocument("bphs-viparita-03", "Viparita Raja Yoga (Harsha, Sarala, Vimala)",
                "When dusthana lords (6th, 8th, 12th) are exclusively situated in dusthana houses (6th, 8th, or 12th) without aspect or association with benefic Kendra/Trikona lords, the native rises to extraordinary victory through adversity, triumphing over formidable adversaries and systemic crises.",
                "parashari", "yogas", "Brihat Parashara Hora Shastra Ch. 43 Sloka 25"),

            new VectorDocument("bphs-dasha-04", "Vimshottari Dasha Activation Principle",
                "The operative planetary ruler of a Mahadasha acts as the supreme orchestrator of human karma during its tenure. Its natural relationship to the ascendant lord, its divisional strength in Navamsa (D9) and Dashamsha (D10), and its SAV ashtakavarga bindus determine whether karma manifests as sovereign elevation or austere discipline.",
                "parashari", "dashas", "Brihat Parashara Hora Shastra Ch. 46 Sloka 5"),

            new VectorDocument("bphs-shadbala-05", "Shadbala Planetary Strength Hierarchy",
                "A planet possessing comprehensive Shadbala exceeding 1.0 Rupa (especially in Sthanabala and Digbala) produces fearless, unhindered manifestation of its house significations even during adversarial transits.",
                "parashari", "evaluation", "Brihat Parashara Hora Shastra Ch. 27 Sloka 19"),

            // --- Jaimini Tradition ---
            new VectorDocument("jaimini-karaka-01", "Atmakaraka Soul Destiny",
                "The planet with the highest advanced degrees in any zodiac sign becomes the Atmakaraka (soul significator). The sign occupied by the Atmakaraka in the Navamsa chart (Karakamsa Lagna) reveals the primordial purpose of the soul's present earthly incarnation and spiritual liberation path.",
                "jaimini", "destiny", "Jaimini Upadesha Sutras Ch. 1 Pada 2 Sutra 1"),

            new VectorDocument("jaimini-amatyakaraka-02", "Amatyakaraka Career & High Office",
                "The planet holding the second highest degree is the Amatyakaraka (counselor/minister). Association of the Atmakaraka and Amatyakaraka in Kendra or 1st/5th/9th houses produces imperial authority, strategic executive influence, and prestigious corporate stewardship.",
                "jaimini", "career", "Jaimini Upadesha Sutras Ch. 1 Pada 2 Sutra 14"),

            new VectorDocument("jaimini-arudha-03", "Arudha Lagna (External Perception & Maya)",
                "Arudha Lagna (AL) mirrors how the native is perceived in the commercial marketplace and societal fabric. Auspicious planets in the 11th from Arudha Lagna generate tangible financial inflows, regardless of the native's inner emotional state.",
                "jaimini", "wealth", "Jaimini Upadesha Sutras Ch. 1 Pada 3 Sutra 2"),

            // --- Bhrigu Nandi Nadi Tradition ---
            new VectorDocument("nadi-trines-01", "Directional Trinal Harmony (1-5-9 Vectors)",
                "Planets situated in mutual 1st, 5th, and 9th positions from one another are situated in the same elemental vector (Dharma Fire, Artha Earth, Kama Air, or Moksha Water). They combine their planetary energies directly without requiring house cusp lordships.",
                "nadi", "destiny", "Bhrigu Nandi Nadi Ch. 1 Sutra 1"),

            new VectorDocument("nadi-saturn-mercury-02", "Saturn and Mercury Synthesis (Karmic Intellectual Enterprise)",
                "When Karma Karaka Saturn associates with Mercury in elemental trine or conjunction, the native's destiny involves trade, publishing, algorithmic calculation, engineering architecture, or autonomous digital commerce rather than physical servile labor.",
                "nadi", "career", "Bhrigu Nandi Nadi Ch. 3 Sutra 18"),

            new VectorDocument("nadi-jupiter-saturn-03", "Dharma-Karmadhipati Nadi Conjunction",
                "The union of Jiva Karaka Jupiter (Soul & Divine Grace) with Karma Karaka Saturn forms the greatest spiritual and professional synthesis, conferring a life devoted to noble public institutions, ethical mentorship, and societal transformation.",
                "nadi", "yogas", "Bhrigu Nandi Nadi Ch. 4 Sutra 7"),

            new VectorDocument("nadi-venus-ketu-04", "Dhan-Moksha Yoga (Venus and Ketu)",
                "The combination of Venus (luxury, wealth, art) with Ketu (esoteric liberation) enables the native to decode subtle hidden knowledge, unlock unconventional riches through digital or spiritual media, and remain detached from material avarice.",
                "nadi", "wealth", "Bhrigu Nandi Nadi Ch. 5 Sutra 22"),

            // --- Lal Kitab Tradition ---
            new VectorDocument("lalkitab-pitra-01", "Pitra Rin (Ancestral Debt) & Remedial Action",
                "When Jupiter or the 9th house is hemmed in or afflicted by malefic nodes (Rahu-Ketu) or debilitated planets, it signals unpaid ancestral promises. Remedial service of planting Peepal trees or contributing to sacred educational trusts dissolves the blockage.",
                "lal_kitab", "remedies", "Lal Kitab (1952 Edition) Chapter on Debts"),

            new VectorDocument("lalkitab-mercury-02", "Budh (Mercury) Restoration Protocol",
                "An afflicted Mercury creates communication hurdles and business volatility. The classic Lal Kitab prescription involves piercing copper coins and floating them in flowing fresh river water on Wednesdays during Shukla Paksha.",
                "lal_kitab", "remedies", "Lal Kitab Chapter on Planetary Remedies"),

            // --- Phaladeepika & Saravali ---
            new VectorDocument("phala-panchamahapurusha-01", "Pancha Mahapurusha Yogas",
                "Non-luminary planets in own sign or exaltation in a Kendra house form Mahapurusha Yogas: Mars produces Ruchaka (martial bravery), Mercury produces Bhadra (intellectual royalty), Jupiter produces Hamsa (saintly wisdom), Venus produces Malavya (aesthetic mastery & opulence), and Saturn produces Sasa (enduring sovereign command).",
                "parashari", "yogas", "Phaladeepika Ch. 6 Slokas 1-4"),

            new VectorDocument("saravali-neechabhanga-02", "Neechabhanga Raja Yoga (Debilitation Cancellation)",
                "When a debilitated planet has its sign dispositor or exaltation lord situated in a Kendra from Lagna or Moon, the initial hardships undergo sudden transmutation into supreme executive mastery and royal status in the second half of life.",
                "parashari", "yogas", "Saravali Ch. 26 Slokas 12-15")
        );

        addAll(initialCorpus);
        log.info("Successfully embedded and indexed {} classical Vedic documents in vector memory.", documents.size());
    }

    @Override
    public void add(VectorDocument document) {
        if (document.getEmbedding() == null || document.getEmbedding().length == 0) {
            String combinedText = document.getTitle() + " " + document.getContent();
            document.setEmbedding(embeddingEngine.embed(combinedText));
        }
        documents.add(document);
    }

    @Override
    public void addAll(List<VectorDocument> docs) {
        for (VectorDocument doc : docs) {
            add(doc);
        }
    }

    @Override
    public List<VectorDocument> similaritySearch(float[] queryVector, int topK, String traditionFilter, String categoryFilter) {
        List<VectorDocument> matches = new ArrayList<>();

        for (VectorDocument doc : documents) {
            if (traditionFilter != null && !traditionFilter.equalsIgnoreCase("all") && !traditionFilter.equalsIgnoreCase(doc.getTradition())) {
                continue;
            }
            if (categoryFilter != null && !categoryFilter.equalsIgnoreCase("all") && !categoryFilter.equalsIgnoreCase(doc.getCategory())) {
                continue;
            }

            double similarity = embeddingEngine.cosineSimilarity(queryVector, doc.getEmbedding());
            VectorDocument scoredDoc = new VectorDocument(doc.getId(), doc.getTitle(), doc.getContent(), doc.getTradition(), doc.getCategory(), doc.getSourceCitation());
            scoredDoc.setEmbedding(doc.getEmbedding());
            scoredDoc.setMetadata(doc.getMetadata());
            scoredDoc.setScore(Math.round(similarity * 10000.0) / 10000.0);
            matches.add(scoredDoc);
        }

        matches.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return matches.subList(0, Math.min(topK, matches.size()));
    }

    @Override
    public int size() {
        return documents.size();
    }
}
