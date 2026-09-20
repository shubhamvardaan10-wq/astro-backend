package com.astro.ai.provider;

import com.astro.ai.model.LlmRequest;
import com.astro.ai.model.LlmResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ModelFallbackChain {

    private static final Logger log = LoggerFactory.getLogger(ModelFallbackChain.class);

    private final List<LlmProvider> providers;
    private final MockLlmProvider defaultFallback;

    public ModelFallbackChain(List<LlmProvider> providers, MockLlmProvider defaultFallback) {
        this.providers = providers;
        this.defaultFallback = defaultFallback;
    }

    public LlmResponse executeWithFallback(LlmRequest request) {
        for (LlmProvider provider : providers) {
            if (provider == defaultFallback) continue; // Keep default fallback as ultimate safeguard
            if (provider.isAvailable()) {
                try {
                    LlmResponse response = provider.generate(request);
                    if (response != null && (response.getContent() != null || response.hasToolCalls())) {
                        return response;
                    }
                } catch (Exception e) {
                    log.warn("Provider {} failed for model {}, falling back to next provider. Reason: {}",
                        provider.getProviderName(), request.getModel(), e.getMessage());
                }
            }
        }

        // Ultimate deterministic fallback
        log.debug("Falling back to deterministic Mock/Offline provider.");
        return defaultFallback.generate(request);
    }
}
