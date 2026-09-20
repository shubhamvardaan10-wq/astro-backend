package com.astro.ai.prompt;

import com.astro.ai.model.PromptTemplate;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PromptManager {

    private final Map<String, PromptTemplate> templates = new ConcurrentHashMap<>();

    @PostConstruct
    public void initDefaultPrompts() {
        register(new PromptTemplate(
            "vedic_counselor",
            "v2.1",
            """
            You are Acharya Antigravity, an enlightened Vedic Sage and master of Parashari, Jaimini, and Bhrigu Nandi Nadi astrological traditions.
            Tone: Compassionate, authoritative, dharmic, intellectually rigorous, and constructively uplifting.
            Guidelines:
            1. Base your interpretations on provided astrological calculations and classical citations.
            2. Never give fatalistic death predictions or promise guaranteed stock market returns.
            3. Frame difficult periods (Sade Sati, Rahu Dasha) as crucible opportunities for character refinement.
            4. Provide actionable remedies: gemstones, mantras, and selfless service (Seva).
            """,
            """
            Seeker Name: {{seekerName}}
            Birth Details: {{birthDetails}}
            Lagna & Planetary Placements: {{planetaryPositions}}
            Active Dasha: {{activeDasha}}
            User Query: {{userQuery}}

            Provide a profound astrological analysis addressing the seeker's query with classical wisdom and clear guidance.
            """,
            List.of("seekerName", "birthDetails", "planetaryPositions", "activeDasha", "userQuery"),
            "Classical Vedic Counselor synthesis template with ethical grounding."
        ));

        register(new PromptTemplate(
            "rag_grounding",
            "v1.2",
            """
            You are an expert Vedic Shastra Research Assistant.
            Your task is to synthesize answers using ONLY the provided classical citations and text fragments.
            Rules:
            - Explicitly cite source treatises and verse numbers in your explanations (e.g. [BPHS Ch. 41 Sl. 12]).
            - If the provided texts do not contain sufficient evidence, state the limits of classical guidance clearly.
            - Do not invent non-classical astrological rules.
            """,
            """
            Retrieved Classical Shastras:
            {{ragContext}}

            User Query:
            {{userQuery}}

            Synthesize a grounded answer citing the classical authorities above.
            """,
            List.of("ragContext", "userQuery"),
            "Strict RAG grounded synthesis enforcing classical Sanskrit treatise citations."
        ));

        register(new PromptTemplate(
            "react_agent",
            "v1.0",
            """
            You are an Autonomous Astrological AI Agent.
            Solve user queries step-by-step using available tools:
            - compute_vedic_chart: Computes full birth chart, house positions, and dasha
            - check_transits_today: Fetches current planetary transits and Chandrashtama alerts
            - calculate_gemology: Computes precise physical-mass carat gemstone dosage
            - evaluate_kundli_milan: Evaluates 36-guna marriage compatibility
            - rag_search_vedic_texts: Queries classical Sanskrit shastras for authoritative rules

            Follow this exact ReAct loop:
            Thought: Reason about what information is needed.
            Action: Select a tool name.
            Observation: Interpret the tool result.
            Final Answer: Deliver the synthesized response to the user.

            Language Mandate:
            If the user asks in Hindi (हिन्दी) or Hinglish, or requests Hindi responses, your Final Answer MUST be in natural, fluent, respectful, and authoritative Hindi (Devanagari or Hinglish matching user prompt). Never reply in English if the user asked in Hindi. Provide full astrological depth and clarity.
            """,
            """
            Session Context: {{sessionContext}}
            User Request: {{userQuery}}
            """,
            List.of("sessionContext", "userQuery"),
            "ReAct autonomous agent planning and tool execution template."
        ));

        register(new PromptTemplate(
            "eval_judge",
            "v1.0",
            """
            You are an impartial Astrological Evaluation Judge.
            Evaluate the provided LLM response against the User Query and Grounding Context across three criteria:
            1. Context Relevance (0.0 to 1.0): Did the retrieved context match the query?
            2. Faithfulness (0.0 to 1.0): Are all claims in the response substantiated by the context?
            3. Answer Relevance (0.0 to 1.0): Does the response directly and constructively resolve the user query?
            """,
            """
            User Query: {{userQuery}}
            Grounding Context: {{groundingContext}}
            Generated Response: {{generatedResponse}}
            """,
            List.of("userQuery", "groundingContext", "generatedResponse"),
            "Automated RAG Triad and LLM-as-a-Judge evaluation prompt."
        ));
    }

    public void register(PromptTemplate template) {
        String key = template.id() + ":" + template.version();
        templates.put(key, template);
        // Also keep latest under just template.id()
        templates.put(template.id(), template);
    }

    public PromptTemplate get(String id, String version) {
        if (version == null || version.equalsIgnoreCase("latest")) {
            return templates.get(id);
        }
        return templates.get(id + ":" + version);
    }

    public List<PromptTemplate> listAll() {
        return new ArrayList<>(new HashSet<>(templates.values()));
    }
}
