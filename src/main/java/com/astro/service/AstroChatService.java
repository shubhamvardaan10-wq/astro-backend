package com.astro.service;

import com.astro.model.AstroChatRequest;
import com.astro.model.AstroChatResponse;
import com.astro.model.BirthRequest;
import com.astro.model.VedicChartResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AstroChatService — Stateful Multi-Turn Conversational Astrologer Service.
 *
 * Provides:
 * - Session memory retention (birth profile + chat thread) across turns.
 * - Dynamic natal chart grounding (Lagna, Moon Sign, Mahadasha, Transits).
 * - Multi-lingual support (Hindi, Tamil, Telugu, English).
 * - Structured action cards for timing windows and Vedic remedies.
 */
@Service
public class AstroChatService {

    private static final Logger log = LoggerFactory.getLogger(AstroChatService.class);

    private final AstroExpansionService expansionService;
    private final VedicService vedicService;
    private final AstroCacheService cacheService;

    // In-memory session store for native birth profiles
    private final Map<String, BirthRequest> sessionProfiles = new ConcurrentHashMap<>();

    @Autowired
    public AstroChatService(
            AstroExpansionService expansionService,
            VedicService vedicService,
            AstroCacheService cacheService
    ) {
        this.expansionService = expansionService;
        this.vedicService = vedicService;
        this.cacheService = cacheService;
    }

    /**
     * Executes a conversational chat turn with session memory and chart grounding.
     */
    public AstroChatResponse chat(AstroChatRequest request) {
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = "sess_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        }

        // 1. Update or retrieve session birth profile
        if (request.getBirth() != null && request.getBirth().getDob() != null) {
            sessionProfiles.put(sessionId, request.getBirth());
        }
        BirthRequest birth = sessionProfiles.get(sessionId);

        // 2. Compute natal payload if birth data available
        Map<String, Object> natalPayload = null;
        if (birth != null && birth.getDob() != null) {
            try {
                VedicChartResponse chart = vedicService.compute(birth);
                natalPayload = expansionService.buildVedicPayload(chart);
            } catch (Exception e) {
                log.warn("Could not compute chart for session {}: {}", sessionId, e.getMessage());
            }
        }

        // 3. Retrieve prior chat history
        List<Map<String, String>> history = cacheService.getChatHistory(sessionId);
        boolean contextRetained = history != null && !history.isEmpty();

        // 4. Invoke conversational engine
        String language = request.getLanguage() != null ? request.getLanguage() : "en";
        Map<String, Object> botResult = expansionService.conversationalChat(
                request.getMessage(),
                natalPayload,
                history,
                language
        );

        String replyText = (String) botResult.getOrDefault("replyText", "Namaste! How may I assist your astrological journey?");
        String intent = (String) botResult.getOrDefault("intent", "general");
        Map<String, Object> actionCards = (Map<String, Object>) botResult.get("actionCards");
        Map<String, Object> natalReference = (Map<String, Object>) botResult.get("natalReference");

        // 5. Persist turn to chat history
        cacheService.saveChatMessage(sessionId, "user", request.getMessage());
        cacheService.saveChatMessage(sessionId, "assistant", replyText);

        // 6. Build and return structured response
        AstroChatResponse response = new AstroChatResponse();
        response.setSessionId(sessionId);
        response.setReply(replyText);
        response.setIntent(intent);
        response.setLanguage(language);
        response.setActionCards(actionCards);
        response.setNatalReference(natalReference);
        response.setHistoryCount((history != null ? history.size() : 0) + 2);
        response.setSessionContextRetained(contextRetained);

        return response;
    }

    /**
     * Returns full chronological chat thread for a session.
     */
    public List<Map<String, String>> getHistory(String sessionId) {
        return cacheService.getChatHistory(sessionId);
    }

    /**
     * Clears session chat thread and profile.
     */
    public boolean clearSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return false;
        sessionProfiles.remove(sessionId);
        return true;
    }
}
