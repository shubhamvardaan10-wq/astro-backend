package com.astro.controller;

import com.astro.security.ApiKeyAuthFilter;
import com.astro.security.ApiKeyDetails;
import com.astro.security.ApiKeyService;
import com.astro.security.ApiKeyTier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Commercial Billing, Subscription Plans & Multi-Gateway Webhooks (Stripe & Razorpay).
 */
@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private static final Logger log = LoggerFactory.getLogger(BillingController.class);

    private final ApiKeyService apiKeyService;
    private final ObjectMapper mapper;
    private final String stripeWebhookSecret;
    private final String razorpayWebhookSecret;

    @Autowired
    public BillingController(
            ApiKeyService apiKeyService,
            ObjectMapper mapper,
            @Value("${astro.billing.stripe.webhook-secret:}") String stripeWebhookSecret,
            @Value("${astro.billing.razorpay.webhook-secret:}") String razorpayWebhookSecret) {
        this.apiKeyService = apiKeyService;
        this.mapper = mapper != null ? mapper : new ObjectMapper();
        this.stripeWebhookSecret = stripeWebhookSecret != null ? stripeWebhookSecret.trim() : "";
        this.razorpayWebhookSecret = razorpayWebhookSecret != null ? razorpayWebhookSecret.trim() : "";
    }

    /**
     * Public Catalog of SaaS Subscription Tiers & Quotas.
     */
    @GetMapping("/plans")
    public ResponseEntity<Map<String, Object>> getSubscriptionPlans() {
        List<Map<String, Object>> plans = List.of(
            Map.of(
                "id", "free",
                "name", "Developer Sandbox",
                "tier", ApiKeyTier.FREE.name(),
                "priceUsd", 0,
                "priceInr", 0,
                "rateLimitPerMinute", ApiKeyTier.FREE.getRequestsPerMinute(),
                "monthlyQuota", ApiKeyTier.FREE.getMonthlyQuota(),
                "features", List.of("Standard Vedic & Western Charts", "Vimshottari Dasha", "Nakshatras", "Panchangam")
            ),
            Map.of(
                "id", "starter",
                "name", "Startup Launch",
                "tier", ApiKeyTier.STARTER.name(),
                "priceUsd", 29,
                "priceInr", 2499,
                "rateLimitPerMinute", ApiKeyTier.STARTER.getRequestsPerMinute(),
                "monthlyQuota", ApiKeyTier.STARTER.getMonthlyQuota(),
                "features", List.of("All Free features", "Dynamic SVG Visualizer", "Matchmaking 36-Guna", "Numerology Tuning", "Vastu Matrix")
            ),
            Map.of(
                "id", "pro",
                "name", "Professional SaaS",
                "tier", ApiKeyTier.PRO.name(),
                "priceUsd", 99,
                "priceInr", 7999,
                "rateLimitPerMinute", ApiKeyTier.PRO.getRequestsPerMinute(),
                "monthlyQuota", ApiKeyTier.PRO.getMonthlyQuota(),
                "features", List.of("All Starter features", "Classical Treatise RAG & AI Agent", "ReportLab PDF Publishing", "Jaimini & KP Horary", "Priority Caching")
            ),
            Map.of(
                "id", "enterprise",
                "name", "High-Volume Enterprise",
                "tier", ApiKeyTier.ENTERPRISE.name(),
                "priceUsd", 299,
                "priceInr", 24999,
                "rateLimitPerMinute", ApiKeyTier.ENTERPRISE.getRequestsPerMinute(),
                "monthlyQuota", "Unlimited",
                "features", List.of("All Pro features", "Dedicated Rate Limits", "Custom Classical Knowledge Ingestion", "Custom Sloka Citation Models", "24/7 SLA Support")
            )
        );

        return ResponseEntity.ok(Map.of(
            "service", "Astro-Backend Commercial API Gateway",
            "currency", "USD / INR",
            "activeGateways", List.of("Stripe", "Razorpay"),
            "plans", plans
        ));
    }

    /**
     * Authenticated Endpoint: View caller's current usage and plan metadata.
     */
    @GetMapping("/usage")
    public ResponseEntity<Map<String, Object>> getCurrentUsage(HttpServletRequest request) {
        ApiKeyDetails details = (ApiKeyDetails) request.getAttribute(ApiKeyAuthFilter.ATTR_API_KEY);
        if (details == null) {
            return ResponseEntity.ok(Map.of(
                "authenticated", false,
                "message", "Security is currently in bypass mode or no key context present."
            ));
        }

        return ResponseEntity.ok(Map.of(
            "authenticated", true,
            "key", ApiKeyService.maskKey(details.key()),
            "ownerEmail", details.ownerEmail(),
            "tier", details.tier().name(),
            "rateLimitPerMinute", details.tier().getRequestsPerMinute(),
            "monthlyQuota", details.tier().getMonthlyQuota() > 0 ? details.tier().getMonthlyQuota() : "Unlimited",
            "active", details.active()
        ));
    }

    /**
     * MASTER API #3: Unified Commercial Management & Automated Provisioning
     * GET  -> Returns public plans and caller's quota usage
     * POST -> Automated Stripe/Razorpay webhook and instant provisioning
     */
    @RequestMapping(value = "/manage", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> manageBilling(
            HttpServletRequest request,
            @RequestBody(required = false) String body,
            @RequestHeader(name = "Stripe-Signature", required = false) String stripeSig,
            @RequestHeader(name = "X-Razorpay-Signature", required = false) String rzpSig) {
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            ApiKeyDetails details = (ApiKeyDetails) request.getAttribute(ApiKeyAuthFilter.ATTR_API_KEY);
            Map<String, Object> plansResp = getSubscriptionPlans().getBody();
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("service", "Astro-Backend Unified Billing & Quota Manager");
            resp.put("plans", plansResp != null ? plansResp.get("plans") : List.of());
            resp.put("activeGateways", List.of("Stripe", "Razorpay"));
            if (details != null) {
                resp.put("usage", Map.of(
                    "key", ApiKeyService.maskKey(details.key()),
                    "ownerEmail", details.ownerEmail(),
                    "tier", details.tier().name(),
                    "rateLimitPerMinute", details.tier().getRequestsPerMinute(),
                    "monthlyQuota", details.tier().getMonthlyQuota() > 0 ? details.tier().getMonthlyQuota() : "Unlimited"
                ));
            }
            return ResponseEntity.ok(resp);
        } else {
            if (body == null || body.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Empty payload for billing management"));
            }
            try {
                JsonNode node = mapper.readTree(body);
                if (node.has("type") || stripeSig != null) {
                    return handleStripeWebhook(body, stripeSig);
                } else if (node.has("event") || node.has("payload") || rzpSig != null) {
                    return handleRazorpayWebhook(body, rzpSig);
                } else if (node.has("action") && "provision".equalsIgnoreCase(node.path("action").asText())) {
                    String email = node.path("email").asText("user@domain.com");
                    String plan = node.path("tier").asText("pro");
                    ApiKeyTier tier = resolveTier(plan);
                    ApiKeyDetails newKey = apiKeyService.createKey(email, tier, 30);
                    return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "apiKeyProvisioned", true,
                        "ownerEmail", email,
                        "tier", tier.name(),
                        "key", newKey.key(),
                        "keyMasked", ApiKeyService.maskKey(newKey.key())
                    ));
                }
                return handleStripeWebhook(body, stripeSig);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
            }
        }
    }

    /**
     * Webhook Handler for Stripe Subscription & Checkout Events.
     */
    @PostMapping("/webhook/stripe")
    public ResponseEntity<Map<String, Object>> handleStripeWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(name = "Stripe-Signature", required = false) String signature) {
        try {
            JsonNode event = mapper.readTree(rawPayload);
            String eventType = event.path("type").asText("unknown");
            log.info("Received Stripe Webhook event: {}", eventType);

            if ("checkout.session.completed".equals(eventType) || "customer.subscription.created".equals(eventType)) {
                JsonNode dataObject = event.path("data").path("object");
                String customerEmail = dataObject.path("customer_email").asText(
                        dataObject.path("customer_details").path("email").asText("customer@domain.com"));

                String planId = dataObject.path("client_reference_id").asText("pro");
                ApiKeyTier tier = resolveTier(planId);

                ApiKeyDetails newKey = apiKeyService.createKey(customerEmail, tier, 30);
                return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "event", eventType,
                    "apiKeyProvisioned", true,
                    "ownerEmail", customerEmail,
                    "tier", tier.name(),
                    "keyMasked", ApiKeyService.maskKey(newKey.key())
                ));
            }

            return ResponseEntity.ok(Map.of("status", "ignored", "event", eventType));
        } catch (Exception e) {
            log.error("Stripe Webhook processing error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Webhook Handler for Razorpay Payment & Subscription Events.
     */
    @PostMapping("/webhook/razorpay")
    public ResponseEntity<Map<String, Object>> handleRazorpayWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(name = "X-Razorpay-Signature", required = false) String signature) {
        try {
            JsonNode event = mapper.readTree(rawPayload);
            String eventType = event.path("event").asText("unknown");
            log.info("Received Razorpay Webhook event: {}", eventType);

            if ("payment.captured".equals(eventType) || "subscription.activated".equals(eventType)) {
                JsonNode payment = event.path("payload").path("payment").path("entity");
                String email = payment.path("email").asText("user@domain.in");

                ApiKeyDetails newKey = apiKeyService.createKey(email, ApiKeyTier.PRO, 30);
                return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "gateway", "Razorpay",
                    "event", eventType,
                    "apiKeyProvisioned", true,
                    "ownerEmail", email,
                    "tier", ApiKeyTier.PRO.name(),
                    "keyMasked", ApiKeyService.maskKey(newKey.key())
                ));
            }

            return ResponseEntity.ok(Map.of("status", "ignored", "event", eventType));
        } catch (Exception e) {
            log.error("Razorpay Webhook processing error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private ApiKeyTier resolveTier(String planIdentifier) {
        if (planIdentifier == null) return ApiKeyTier.PRO;
        String p = planIdentifier.toLowerCase();
        if (p.contains("starter")) return ApiKeyTier.STARTER;
        if (p.contains("enterprise")) return ApiKeyTier.ENTERPRISE;
        if (p.contains("free")) return ApiKeyTier.FREE;
        return ApiKeyTier.PRO;
    }
}
