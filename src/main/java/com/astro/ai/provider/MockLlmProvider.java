package com.astro.ai.provider;

import com.astro.ai.model.*;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class MockLlmProvider implements LlmProvider {

    @Override
    public String getProviderName() {
        return "mock-provider";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public LlmResponse generate(LlmRequest request) {
        long start = System.currentTimeMillis();
        String lastUserText = extractLastUserMessage(request);
        String lastToolText = extractLastToolMessage(request);

        LlmResponse response;

        // 1. Tool-Calling Branch: If tools are provided and no tool execution observation exists yet
        if (request.getTools() != null && !request.getTools().isEmpty() && lastToolText == null) {
            ToolCall toolCall = determineToolCall(lastUserText);
            if (toolCall != null) {
                response = LlmResponse.withTools(List.of(toolCall), request.getModel(), getProviderName());
                response.setId("call-" + UUID.randomUUID().toString().substring(0, 8));
                response.setLatencyMs(System.currentTimeMillis() - start);
                return response;
            }
        }

        // 2. Evaluation Judge Branch
        if (lastUserText.contains("Grounding Context:") && lastUserText.contains("Generated Response:")) {
            String evalJson = """
                {
                  "contextRelevance": 0.94,
                  "faithfulness": 0.98,
                  "answerRelevance": 0.95,
                  "verdict": "PASSED",
                  "notes": "Directly grounded in cited Brihat Parashara Hora Shastra rules with accurate synthesis."
                }
                """;
            response = LlmResponse.text(evalJson, request.getModel(), getProviderName());
            response.setId("eval-" + UUID.randomUUID().toString().substring(0, 8));
            response.setLatencyMs(System.currentTimeMillis() - start);
            return response;
        }

        // 3. Post-Tool Observation Synthesis or Direct General Synthesis
        String answer = generateSynthesis(lastUserText, lastToolText);
        response = LlmResponse.text(answer, request.getModel(), getProviderName());
        response.setId("resp-" + UUID.randomUUID().toString().substring(0, 8));
        response.setUsage(UsageStats.of(45, 120, 0.00015));
        response.setLatencyMs(System.currentTimeMillis() - start);
        return response;
    }

    private ToolCall determineToolCall(String userText) {
        String lower = userText.toLowerCase(Locale.ROOT);

        // Vedic Chart & Career inquiries (English + Hindi / Hinglish)
        if (lower.contains("chart") || lower.contains("horoscope") || lower.contains("lagna") 
                || lower.contains("born on") || lower.contains("my future") || lower.contains("career")
                || lower.contains("kundli") || lower.contains("kundali") || lower.contains("bhavishya")
                || lower.contains("naukri") || lower.contains("कुंडली") || lower.contains("करियर")
                || lower.contains("भविष्य") || lower.contains("नौकरी")) {
            return new ToolCall("call_chart_1", "compute_vedic_chart", Map.of("dob", "1990-05-15", "time", "14:30:00", "city", "New Delhi"));
        } 
        // Gochar / Transits inquiries
        else if (lower.contains("transit") || lower.contains("today") || lower.contains("chandrashtama")
                || lower.contains("gochar") || lower.contains("गोचर") || lower.contains("आज का")) {
            return new ToolCall("call_transit_1", "check_transits_today", Map.of("dob", "1990-05-15", "time", "14:30:00", "city", "New Delhi"));
        } 
        // Gemstone / Ratna inquiries
        else if (lower.contains("gemstone") || lower.contains("carat") || lower.contains("crystal")
                || lower.contains("ratna") || lower.contains("pehan") || lower.contains("dharan")
                || lower.contains("रत्न") || lower.contains("पन्ना") || lower.contains("माणिक्य")) {
            return new ToolCall("call_gem_1", "calculate_gemology", Map.of("dob", "1990-05-15", "time", "14:30:00", "city", "New Delhi", "bodyWeightKg", 65.0));
        } 
        // Kundli Milan / Matchmaking inquiries
        else if (lower.contains("match") || lower.contains("marriage") || lower.contains("guna") 
                || lower.contains("compatibility") || lower.contains("milan") || lower.contains("shadi") 
                || lower.contains("shaadi") || lower.contains("vivah") || lower.contains("मिलान") 
                || lower.contains("विवाह") || lower.contains("शादी")) {
            return new ToolCall("call_match_1", "evaluate_kundli_milan", Map.of("partner1Name", "Seeker A", "partner2Name", "Seeker B"));
        } 
        // Shubh Muhurta inquiries
        else if (lower.contains("muhurta") || lower.contains("muhurat") || lower.contains("shubh") || lower.contains("मुहूर्त") || lower.contains("शुभ")) {
            return new ToolCall("call_muhurta_1", "find_shubh_muhurta", Map.of("eventType", "STARTUP_INCORPORATION", "startDate", "2026-09-22", "city", "New Delhi"));
        }
        // Classical Shastra RAG inquiries
        else if (lower.contains("shastra") || lower.contains("rule") || lower.contains("yoga") || lower.contains("dharma") || lower.contains("शास्त्र")) {
            return new ToolCall("call_rag_1", "rag_search_vedic_texts", Map.of("query", userText, "tradition", "all"));
        }
        return null;
    }

    private String generateSynthesis(String userText, String toolText) {
        boolean hindi = isHindi(userText);

        if (hindi) {
            if (toolText != null && !toolText.isBlank()) {
                if (toolText.contains("Gemology Dosage") || toolText.contains("Carat") || toolText.contains("Carats")) {
                    return "सादर प्रणाम। आपके शारीरिक भार (वज़न) और लग्नेश की स्थिति के आधार पर [खगोलीय रत्न गणना: " + toolText + "]:\n\n"
                        + "• **मुख्य अनुशंसित रत्न**: पन्ना (Emerald) / हरित तुर्मली (Green Tourmaline)\n"
                        + "• **सटीक शारीरिक मात्रा**: 7.0 कैरेट (वैज्ञानिक सूत्र: Weight / 10 + 0.5 Carats)\n"
                        + "• **धारण विधि**: कनिष्ठिका (Little Finger) में सोने या चांदी में जड़वाकर, बुधवार प्रातः काल 'ॐ बुं बुधाय नमः' मंत्र के साथ धारण करें।\n"
                        + "• **ज्योतिषीय प्रभाव**: यह रत्न आपकी बौद्धिक क्षमता, निर्णय-शक्ति, व्यापारिक लाभ और मानसिक स्थिरता को तीव्र गति प्रदान करेगा।";
                } else if (toolText.contains("Lagna:") || toolText.contains("Planets Count")) {
                    return "सादर प्रणाम। आपकी जन्म पत्रिका के सूक्ष्म खगोलीय विश्लेषण के आधार पर [कुंडली विवरण: " + toolText + "]:\n\n"
                        + "• **लग्न व स्वभाव**: आपका लग्न अत्यंत तीव्र बौद्धिक और विश्लेषणात्मक शक्ति से युक्त है।\n"
                        + "• **राजयोग स्थिति**: केंद्र और त्रिकोण भावों में शुभ ग्रहों की युति कार्यक्षेत्र में असाधारण नेतृत्व का निर्माण करती है।\n"
                        + "• **करियर परामर्श**: नौकरी में बंधकर रहने की बजाय स्वतंत्र तकनीक, सॉफ्टवेयर आर्किटेक्चर, प्रबंधन और उच्च बौद्धिक उद्यम में आपकी सर्वोच्च सफलता निश्चित है।";
                } else if (toolText.contains("Transit") || toolText.contains("Chandrashtama")) {
                    return "सादर प्रणाम। आज के ग्रह गोचर व तात्कालिक स्थिति के आधार पर [गोचर गणना: " + toolText + "]:\n\n"
                        + "• **चंद्राष्टम स्थिति**: पूर्णतः सुरक्षित (कोई चंद्र बाधा सक्रिय नहीं है)।\n"
                        + "• **दैनिक ऊर्जा**: कार्य व व्यापार में 85% से अधिक अनुकूलता विद्यमान है।\n"
                        + "• **कार्य निर्देश**: महत्वपूर्ण निर्णयों, वित्तीय निवेश और नवीन वार्ताओं के लिए आज का दिन अत्यंत शुभ व फलदायी है।";
                } else if (toolText.contains("Match Score") || toolText.contains("Gunas")) {
                    return "सादर प्रणाम। वैदिक अष्टकूट मिलान प्रणाली के आधार पर [कुंडली मिलान परिणाम: " + toolText + "]:\n\n"
                        + "• **कुल प्राप्तांक**: 36 में से 28 गुण (श्रेष्ठ मिलान वर्ग)।\n"
                        + "• **नाड़ी व भकूट दोष**: पूर्णतः शांत व दोषमुक्त।\n"
                        + "• **वैदिक निष्कर्ष**: दोनों जातकों के मध्य मानसिक, भावनात्मक और वैचारिक समरसता दीर्घायु व सुखद दांपत्य का निर्माण करेगी।";
                } else if (toolText.contains("Muhurta") || toolText.contains("Window")) {
                    return "सादर प्रणाम। वैदिक पंचांग एवं पंचक-रहित मुहूर्त गणना के अनुसार [शुभ मुहूर्त विवरण: " + toolText + "]:\n\n"
                        + "• **सर्वोत्तम अमृत काल**: आगामी शुक्ल पक्ष के अंतर्गत 'अभिजीत मुहूर्त' मध्याह्न 11:42 से 12:30 IST प्राप्त हो रहा है।\n"
                        + "• **राहुकाल / यमगंड**: पूर्णतः बहिष्कृत व सुरक्षित।\n"
                        + "• **शुभ कर्म निर्देश**: नए व्यापार का शुभारंभ, अनुबंध हस्ताक्षर या डिजिटल लॉन्च इसी 48 मिनट की अवधि में करें।";
                }

                return "सादर प्रणाम। आपकी जन्म पत्रिका और खगोलीय गणनाओं के आधार पर [खगोलीय गणना परिणाम: " + toolText + "], "
                    + "आपकी कुंडली में देवगुरु बृहस्पति और कर्मफल दाता शनि का शुभ व बलवान प्रभाव दृष्टिगोचर हो रहा है। "
                    + "यह ग्रह विन्यास आपकी बौद्धिक क्षमता, व्यावसायिक सफलता और दीर्घकालिक प्रतिष्ठा को दृढ़ता प्रदान करता है। "
                    + "महत्वपूर्ण कार्यों और नए उपक्रमों का आरंभ अनुकूल चंद्र नक्षत्र व शुभ मुहूर्त में करना विशेष कल्याणकारी सिद्ध होगा।";
            }

            if (userText.contains("Retrieved Classical Shastras:")) {
                return "प्राचीन वैदिक संहिताओं (महर्षि पराशर, जैमिनी एवं लाल किताब) के वचनों के अनुसार:\n\n"
                    + "केंद्र (1, 4, 7, 10) और त्रिकोण (1, 5, 9) भावों में शुभ ग्रहों की स्थिति जातक को अपार ज्ञान, यश और अटूट ऐश्वर्य प्रदान करती है। "
                    + "शास्त्र निष्काम कर्म, गुरु कृपा और आत्म-अनुशासन को सर्वोच्च भाग्यवर्धक कारक मानते हैं।";
            }

            return "सादर प्रणाम। वैदिक ज्योतिष के अनुसार आपके वर्तमान ग्रह चक्र बौद्धिक विकास, कार्यक्षेत्र में प्रगति और आंतरिक शक्ति की ओर संकेत कर रहे हैं। "
                + "धैर्य, नियमित साधना और आत्मविश्वास से आपके सभी मनोरथ सिद्ध होंगे।";
        }

        if (toolText != null && !toolText.isBlank()) {
            return "Based on your verified astronomical placements [Tool Execution Result: " + toolText + "], "
                + "your chart shows strong benefic support from Jupiter and Saturn. "
                + "The planetary configuration indicates high creative intellect and enduring enterprise potential. "
                + "Align major milestones with auspicious lunar cycles for optimal results.";
        }

        if (userText.contains("Retrieved Classical Shastras:")) {
            return "According to the classical authorities cited above, planetary combinations in Kendra and Trikona houses "
                + "act as supreme catalysts for wisdom and worldly achievements. The sacred texts emphasize ethical stewardship "
                + "and purposeful action (*Nishkama Karma*).";
        }

        return "Namaskar. Classical Jyotish reveals that your planetary cycles are currently oriented toward self-mastery, "
            + "strategic enterprise, and intellectual growth. Channel your efforts steadily and honor divine timing.";
    }

    private boolean isHindi(String text) {
        if (text == null) return false;
        // Check for Devanagari Unicode characters (0x0900 to 0x097F)
        for (char c : text.toCharArray()) {
            if (c >= '\u0900' && c <= '\u097F') {
                return true;
            }
        }
        // Check for common Hindi/Hinglish vocabulary
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("mera") || lower.contains("meri") || lower.contains("kaisa")
            || lower.contains("kripya") || lower.contains("batao") || lower.contains("namaste")
            || lower.contains("kundli") || lower.contains("kundali") || lower.contains("ratna")
            || lower.contains("shadi") || lower.contains("shaadi") || lower.contains("muhurat")
            || lower.contains("shubh") || lower.contains("karega") || lower.contains("hoga")
            || lower.contains("kaun") || lower.contains("kya") || lower.contains("hai");
    }

    private String extractLastUserMessage(LlmRequest request) {
        if (request.getMessages() == null) return "";
        for (int i = request.getMessages().size() - 1; i >= 0; i--) {
            LlmMessage msg = request.getMessages().get(i);
            if (msg.getRole() == LlmRole.USER) {
                return msg.getContent() != null ? msg.getContent() : "";
            }
        }
        return "";
    }

    private String extractLastToolMessage(LlmRequest request) {
        if (request.getMessages() == null) return null;
        for (int i = request.getMessages().size() - 1; i >= 0; i--) {
            LlmMessage msg = request.getMessages().get(i);
            if (msg.getRole() == LlmRole.TOOL) {
                return msg.getContent();
            }
        }
        return null;
    }
}
