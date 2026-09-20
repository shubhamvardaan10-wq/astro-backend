package com.astro.prediction;

import java.util.Map;

/** Vimshottari Dasha period interpretation texts. */
public final class DashaTexts {
    private DashaTexts() {}

    public static String getMaha(String lord) {
        return MAHA.getOrDefault(lord, "The " + lord + " Mahadasha activates the themes and significations of " + lord + " in your chart for this period.");
    }

    public static String getAntar(String mahaLord, String antarLord) {
        return "Within the " + mahaLord + " Mahadasha, the sub-period of " + antarLord +
               " brings a blending of both planets' significations. " +
               ANTAR_BLEND.getOrDefault(mahaLord + "_" + antarLord,
               antarLord + "'s energies now operate as a sub-theme within the broader " + mahaLord + " framework, " +
               "highlighting the houses ruled by both planets in your specific chart. " +
               "This is a productive period for pursuits aligned with both planetary natures.");
    }

    static final Map<String, String> MAHA = Map.ofEntries(
        Map.entry("Sun",
            "The Sun Mahadasha (6 years) illuminates every area of life with solar themes: authority, self-expression, career, government connection, and the relationship with the father and masculine authority figures. " +
            "This is a period for stepping into visibility — for claiming positions of leadership that your prior preparation has made you ready for. " +
            "Health and vitality are emphasized, and the body responds powerfully to conscious attention and lifestyle management. " +
            "If the Sun is well-placed in your natal chart, this period brings recognition, career advancement, and the fulfillment of long-held aspirations for achievement and respect. " +
            "If afflicted, it may bring challenges with authority, ego conflicts, or health matters related to the heart and circulatory system. " +
            "The Sun Mahadasha rewards authenticity: the more genuinely you inhabit your own authority rather than performing it, the more effectively this period's solar gifts manifest. " +
            "Government, public life, and any arena requiring visible, dignified leadership are particularly activated. " +
            "Spiritual practice oriented toward the Sun — dawn meditation, solar salutations, and working with father-wound healing — produces particularly powerful results during this Mahadasha."),

        Map.entry("Moon",
            "The Moon Mahadasha (10 years) turns the entire life toward lunar themes: emotional life, the mother, home and property, water-related matters, the public and popular recognition, and the deep underground rivers of feeling and intuition that shape behavior far below conscious awareness. " +
            "This can be a deeply rich period of emotional development, intuitive awakening, and the cultivation of genuine inner security independent of outer circumstance. " +
            "Relationships with women, with water, with the past, and with ancestral patterns all become especially vivid and significant during this decade. " +
            "If the Moon is well-placed in your natal chart, this period brings emotional abundance, popularity, home acquisition, and the nurturing of deeply satisfying personal bonds. " +
            "If afflicted, it may bring emotional turbulence, health challenges related to the stomach and lymphatic system, or periods of lunar-flavored melancholy that invite deeper self-inquiry. " +
            "The Moon Mahadasha asks the essential question: do you have a genuine home within yourself — a place of inner steadiness that remains stable regardless of external tides? " +
            "Creative work, intuitive development, and the healing of maternal and ancestral wounds are powerfully supported during this period."),

        Map.entry("Mars",
            "The Mars Mahadasha (7 years) activates the chart with fierce, competitive, courageous, and executive energy — a period for action, initiative, the overcoming of obstacles, and the bold pursuit of goals that have previously been approached too tentatively. " +
            "Physical vitality is high; the body wants to be challenged and responds powerfully to athletic training, disciplined exercise, and the satisfaction of physical mastery. " +
            "Competitive environments, entrepreneurial challenges, and situations requiring decisive executive leadership suit this Mahadasha's essential character. " +
            "If Mars is well-placed, this period brings remarkable accomplishments through bold, disciplined action — properties purchased, enemies overcome, and goals achieved through sheer applied force and competitive superiority. " +
            "If afflicted, Mars Mahadasha may bring accidents, conflicts, legal challenges, or the consequences of impulsive action that bypassed necessary deliberation. " +
            "The essential practice during Mars Mahadasha is channeling aggressive energy constructively: physical exercise, competitive sport, and disciplined courage rather than reactive conflict. " +
            "Legal matters, surgical interventions if needed, property transactions, and competitive professional challenges are all domains where Mars Mahadasha provides genuine advantage."),

        Map.entry("Rahu",
            "The Rahu Mahadasha (18 years) is one of the most transformatively significant periods in a life — a long, intense immersion in the themes of ambition, desire, unconventional experience, and the relentless karmic drive toward the soul's evolutional edge. " +
            "Rahu amplifies whatever it touches: successes may be remarkable and sudden; challenges may be unusual or disorienting in their specific texture. " +
            "Foreign travel, foreign connections, technological innovation, and unconventional paths often characterize this Mahadasha's most significant developments. " +
            "The material world becomes highly engaging — professional ambition, social networking, and the pursuit of specific desires all intensify. " +
            "If Rahu is well-placed, this period can produce extraordinary achievements, international recognition, and the fulfillment of previously impossible-seeming aspirations. " +
            "If afflicted, it may bring confusion about authentic direction, health matters of unusual character, or experiences of deception — by others or by one's own projections — that demand discernment. " +
            "The spiritual practice for Rahu Mahadasha involves maintaining connection to your authentic dharmic values while the world's seductive amplification of desire tests your capacity for discernment and detachment."),

        Map.entry("Jupiter",
            "The Jupiter Mahadasha (16 years) is traditionally considered the most inherently fortunate of all Mahadashas — a sustained period of expansion, wisdom, grace, abundance, and the deepening of everything that is genuinely good and meaningful in your life. " +
            "Teachers, mentors, guides, and benefactors appear with unusual regularity and generosity during this period. " +
            "Higher education, philosophical inquiry, spiritual practice, and the teaching of wisdom to others are all powerfully supported. " +
            "Children, if desired, often come during Jupiter Mahadasha, and marriages made during this period tend toward depth and longevity. " +
            "If Jupiter is well-placed, this Mahadasha brings professional advancement, financial growth, philosophical maturity, and the genuine experience of divine grace operating in everyday life. " +
            "If afflicted, Jupiter's excess can manifest as overindulgence, philosophical overconfidence, or expansion beyond sustainable bounds — the wisdom of discernment must accompany the abundance of opportunity. " +
            "The spiritual practice for Jupiter Mahadasha involves gratitude, generosity, and the willingness to share what the universe has given so generously with those who have received less."),

        Map.entry("Saturn",
            "The Saturn Mahadasha (19 years) is the longest of all Mahadashas and operates with Saturn's characteristic combination of discipline, karmic accountability, hard work, and the slow but certain rewarding of authentic effort over time. " +
            "This is rarely a period of effortless grace — Saturn insists on real work, real accountability, and the genuine confrontation with whatever has been avoided, postponed, or constructed on insufficient foundations. " +
            "Career, responsibility, social obligations, and the systematic building of long-term structures all come into sharp focus. " +
            "If Saturn is well-placed, particularly in its own signs or exaltation, this Mahadasha produces remarkable professional achievement, political authority, and the satisfaction of having built something genuinely durable through one's own patient sustained effort. " +
            "If afflicted, this period may involve illness, legal challenges, career setbacks, or the dissolution of structures built on insufficient integrity — each a Saturn-flavored invitation to build more authentically. " +
            "The essential spiritual practice for Saturn Mahadasha is non-resistance to necessary discipline: embracing the necessary work, the karmic accountability, and the slow authentic building of what will genuinely last. " +
            "Service to the elderly, the poor, and the marginalized activates Saturn's protective grace during this long period."),

        Map.entry("Mercury",
            "The Mercury Mahadasha (17 years) activates the chart with intellectual, communicative, commercial, and analytically precise energy — a sustained period for learning, writing, teaching, trading, and the development of communicative skills that create genuine professional and personal value. " +
            "The mind is exceptionally active during this Mahadasha, absorbing information across diverse domains with unusual facility and translating intellectual capacity into tangible results through writing, speaking, analysis, and commercial enterprise. " +
            "If Mercury is well-placed, this period brings educational achievement, communicative success, commercial prosperity, and the genuine satisfaction of intellectual mastery in fields that matter to you. " +
            "Business, trading, media, technology, consulting, and education are particularly activated professional domains. " +
            "Relationships with siblings, neighbors, and close associates carry heightened significance during this period. " +
            "If afflicted, Mercury Mahadasha may bring nervous exhaustion, communicative misunderstandings that create relationship or professional complications, or a tendency toward excessive mental activity that resists the grounding and rest necessary for genuine creativity. " +
            "The spiritual practice for Mercury Mahadasha involves the use of communicative gifts in service of genuine wisdom — writing and speaking that illuminate rather than merely inform."),

        Map.entry("Ketu",
            "The Ketu Mahadasha (7 years) is one of the most spiritually significant periods available in the Vimshottari Dasha system — a time of deep inward turning, spiritual awakening, unexpected liberation from what has been holding the soul in familiar but limiting patterns, and the activation of past-life wisdom that has been dormant. " +
            "External ambitions tend to feel less compelling than usual during this period, and the inner life — meditation, spiritual practice, solitude, and the investigation of consciousness — draws with unusual magnetic force. " +
            "Unexpected separations from people, circumstances, or identities that have become too familiar and limiting may occur — not as punishment but as Ketu's characteristic form of liberating grace. " +
            "If Ketu is well-placed, this Mahadasha can produce remarkable spiritual development, mystical opening, and the clarification of authentic life purpose that persists with great force into subsequent Mahadashas. " +
            "If afflicted, it may bring confusion, health matters of unusual character, or a sense of directionlessness that ultimately serves the deeper purpose of releasing what was preventing authentic spiritual development. " +
            "The essential practice for Ketu Mahadasha: meditation, solitude, pilgrimages, and the courageous releasing of whatever no longer serves the soul's most authentic unfolding."),

        Map.entry("Venus",
            "The Venus Mahadasha (20 years) — the longest available and traditionally among the most materially and relationally abundant periods — activates the chart with Venusian themes: love, beauty, artistic creativity, material prosperity, relationship, and the genuine enjoyment of life's sensory and relational gifts. " +
            "This is a period for love in all its dimensions: romantic partnership, artistic beauty, genuine friendship, aesthetic pleasure, and the experience of grace and refinement in daily life. " +
            "Financial prosperity tends to increase during Venus Mahadasha, particularly through artistic, aesthetic, relational, or luxury-oriented enterprises. " +
            "Marriage, significant partnerships, and the deepening of intimate relationships are all powerfully supported. " +
            "If Venus is well-placed, this Mahadasha brings romantic fulfillment, artistic achievement, material abundance, and the genuine experience of beauty as a spiritual path. " +
            "If afflicted, it may bring relationship complications, overindulgence in pleasure at the expense of deeper development, or the pursuit of superficial beauty over authentic inner cultivation. " +
            "The spiritual practice for Venus Mahadasha involves experiencing beauty — in relationship, in art, in nature, in the quality of daily life — as a form of genuine devotion and an authentic approach to the divine.")
    );

    static final Map<String, String> ANTAR_BLEND = Map.ofEntries(
        Map.entry("Jupiter_Saturn", "Jupiter's expansion tempered by Saturn's discipline creates a particularly productive period for long-term career building, sustained philosophical study, and the patient construction of abundance through wisdom-guided effort."),
        Map.entry("Saturn_Jupiter", "Saturn's structure illuminated by Jupiter's grace makes this a period of disciplined expansion — building something genuinely significant through the combination of patient strategy and philosophical inspiration."),
        Map.entry("Sun_Moon",       "The solar authority and lunar emotional life come into dialogue — a period for integrating outer achievement with inner emotional authenticity, balancing career visibility with genuine personal fulfillment."),
        Map.entry("Moon_Sun",       "Emotional depth and solar clarity combine — a time for important decisions that integrate feeling and reason, home and career, inner life and outer expression."),
        Map.entry("Venus_Jupiter",  "A period of particularly abundant grace — beauty, wisdom, and genuine good fortune flow together. Relationships, artistic projects, and spiritual practice all prosper through the combined benefic influence."),
        Map.entry("Jupiter_Venus",  "Jupiter's wisdom and Venus's beauty combine for a period of genuine philosophical and aesthetic flourishing. Love, creativity, and the experience of divine grace in ordinary life are the hallmarks."),
        Map.entry("Mars_Saturn",    "The most demanding of antardasha combinations — courage confronts discipline, urgency confronts patience. The productive channel is sustained disciplined effort toward ambitious goals through consistent strategic action."),
        Map.entry("Saturn_Mars",    "Disciplined energy applied to competitive goals produces remarkable results when the impulsive reactive aspect of Mars is channeled through Saturn's patient strategic framework. A period for hard, effective work."),
        Map.entry("Rahu_Jupiter",   "Expansive karmic ambition guided by philosophical wisdom. Foreign opportunities, higher learning, and the pursuit of ambitious growth through ethical, wisdom-guided channels characterize this productive sub-period."),
        Map.entry("Ketu_Venus",     "Spiritual depth and aesthetic sensitivity combine — a period when beauty becomes a spiritual practice and creative work reaches toward transcendence. Excellent for artistic depth, meditation, and genuine inner cultivation alongside outer relationship.")
    );
}
