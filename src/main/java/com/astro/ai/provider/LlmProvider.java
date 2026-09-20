package com.astro.ai.provider;

import com.astro.ai.model.LlmRequest;
import com.astro.ai.model.LlmResponse;

public interface LlmProvider {

    String getProviderName();

    boolean isAvailable();

    LlmResponse generate(LlmRequest request);
}
