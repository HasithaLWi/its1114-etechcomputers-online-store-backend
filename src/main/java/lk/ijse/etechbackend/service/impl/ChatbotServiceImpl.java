package lk.ijse.etechbackend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lk.ijse.etechbackend.dto.chat.ChatHistoryItemDTO;
import lk.ijse.etechbackend.dto.chat.ChatMessageRequestDTO;
import lk.ijse.etechbackend.dto.chat.ChatMessageResponseDTO;
import lk.ijse.etechbackend.dto.chat.ChatStatusResponseDTO;
import lk.ijse.etechbackend.entity.*;
import lk.ijse.etechbackend.enumiration.Status;
import lk.ijse.etechbackend.repository.*;
import lk.ijse.etechbackend.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatbotServiceImpl implements ChatbotService {

    private static final String OFFLINE_MESSAGE =
            "⚠️ E-T Chatbot is currently unavailable. Please get support through email: eteccomputers38@gmail.com";
    private static final Pattern ACTION_PRODUCT_PATTERN =
            Pattern.compile("\\[ACTION:SHOW_PRODUCT:(\\d+)\\]");

    // Repositories for live system grounding
    private final ProductRepository productRepository;
    private final HotDealRepository hotDealRepository;
    private final DealBundleRepository dealBundleRepository;
    private final HomeDealBannerRepository homeDealBannerRepository;
    private final BranchRepository branchRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final LegalPolicyRepository legalPolicyRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.model:}")
    private String geminiModel;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String geminiApiUrl;

    @Override
    public ChatStatusResponseDTO getChatbotStatus() {
        boolean isKeyPresent = geminiApiKey != null && !geminiApiKey.trim().isEmpty();
        if (isKeyPresent) {
            return ChatStatusResponseDTO.builder()
                    .available(true)
                    .model(geminiModel)
                    .message("E-T AI Assistant is online and ready.")
                    .build();
        } else {
            return ChatStatusResponseDTO.builder()
                    .available(false)
                    .model(geminiModel)
                    .message(OFFLINE_MESSAGE)
                    .build();
        }
    }

    @Override
    public ChatMessageResponseDTO processMessage(ChatMessageRequestDTO request, Authentication authentication) {
        String userMessage = request.getMessage() != null ? request.getMessage().trim() : "";
        if (userMessage.isEmpty()) {
            return ChatMessageResponseDTO.builder()
                    .reply("Please enter a question or topic so I can help you.")
                    .suggestedProducts(Collections.emptyList())
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        // Strict requirement: If API key is not provided, do not use any fallback. Simply inform user.
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty()) {
            log.warn("Gemini API key is not configured. Rejecting request with offline message.");
            return ChatMessageResponseDTO.builder()
                    .reply(OFFLINE_MESSAGE)
                    .suggestedProducts(Collections.emptyList())
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        try {
            log.info("Dispatching chat request to Gemini API (Model: {}, Auth: {})",
                    geminiModel, authentication != null ? authentication.getName() : "Guest");
            String aiReply = callGeminiApi(userMessage, request.getHistory(), request.getCart(), authentication);

            // Extract suggested product IDs from action tags
            List<Long> suggestedProductIds = new ArrayList<>();
            Matcher matcher = ACTION_PRODUCT_PATTERN.matcher(aiReply);
            while (matcher.find()) {
                try {
                    suggestedProductIds.add(Long.parseLong(matcher.group(1)));
                } catch (NumberFormatException ignored) {}
            }

            return ChatMessageResponseDTO.builder()
                    .reply(aiReply)
                    .suggestedProducts(suggestedProductIds)
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Failed to communicate with Gemini API: {}", e.getMessage(), e);
            // No mock fallback: simply inform user chatbot is currently unavailable
            return ChatMessageResponseDTO.builder()
                    .reply(OFFLINE_MESSAGE)
                    .suggestedProducts(Collections.emptyList())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    private String callGeminiApi(String currentMessage, List<ChatHistoryItemDTO> history, List<Object> cart, Authentication authentication) throws Exception {
        String endpoint = String.format("%s/%s:generateContent?key=%s",
                geminiApiUrl.replaceAll("/+$", ""),
                geminiModel.trim(),
                geminiApiKey.trim());

        Map<String, Object> requestPayload = new HashMap<>();

        // Comprehensive System Instruction with Live Store Data & Auth Customer Grounding
        Map<String, Object> systemInstruction = new HashMap<>();
        systemInstruction.put("parts", List.of(Map.of("text", buildComprehensiveSystemPrompt(cart, authentication))));
        requestPayload.put("systemInstruction", systemInstruction);

        // Contents (Multi-turn conversation history)
        List<Map<String, Object>> contents = new ArrayList<>();

        if (history != null && !history.isEmpty()) {
            int startIndex = Math.max(0, history.size() - 8);
            for (int i = startIndex; i < history.size(); i++) {
                ChatHistoryItemDTO item = history.get(i);
                if (item.getText() == null || item.getText().trim().isEmpty()) continue;

                String role = "user";
                if (item.getSender() != null) {
                    String senderLower = item.getSender().toLowerCase();
                    if (senderLower.contains("bot") || senderLower.contains("assistant") || senderLower.contains("model")) {
                        role = "model";
                    }
                }

                contents.add(Map.of(
                        "role", role,
                        "parts", List.of(Map.of("text", item.getText().trim()))
                ));
            }
        }

        // Append current user message
        contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", currentMessage))
        ));
        requestPayload.put("contents", contents);

        // Generation Config
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.7);
        generationConfig.put("maxOutputTokens", 1200);
        requestPayload.put("generationConfig", generationConfig);

        RestClient restClient = RestClient.builder().build();
        String responseBody = restClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestPayload)
                .retrieve()
                .body(String.class);

        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode candidates = root.path("candidates");
        if (candidates.isArray() && !candidates.isEmpty()) {
            JsonNode firstCandidate = candidates.get(0);
            JsonNode parts = firstCandidate.path("content").path("parts");
            if (parts.isArray() && !parts.isEmpty()) {
                return parts.get(0).path("text").asText("");
            }
        }

        throw new IllegalStateException("Empty or unrecognized response structure from Gemini API");
    }

    private String buildComprehensiveSystemPrompt(List<Object> cart, Authentication authentication) {
        StringBuilder sb = new StringBuilder();

        sb.append("You are E-T, the intelligent, friendly, and expert AI assistant for \"ETech Computers\" — Sri Lanka's leading high-performance computer and gaming hardware store.\n\n");

        sb.append("═════════════════════════════════════════════════════════════\n");
        sb.append("  CORE IDENTITY & OPERATING PRINCIPLES\n");
        sb.append("═════════════════════════════════════════════════════════════\n");
        sb.append("- Name: E-T\n");
        sb.append("- Identity: ETech AI Hardware Specialist & Customer Solutions Consultant.\n");
        sb.append("- Tone: Enthusiastic, tech-knowledgeable, conversational, concise, and helpful (2-4 sentences for standard answers, organized bullet points for specs/deals).\n");
        sb.append("- Currency: ALWAYS quote prices in Sri Lankan Rupees (LKR or Rs.). Never use USD ($).\n");
        sb.append("- Hardware Expertise: Deep understanding of PC building, GPUs (RTX 40-series), gaming laptops, high-refresh OLED monitors, mechanical keyboards, gaming mice, DDR5 memory, PSUs, and thermal performance.\n\n");

        sb.append("═════════════════════════════════════════════════════════════\n");
        sb.append("  CRITICAL MANDATES & ACTION TAG RULES\n");
        sb.append("═════════════════════════════════════════════════════════════\n");
        sb.append("1. MANDATORY PRODUCT CARDS: Whenever you mention, recommend, or discuss ANY product in the catalog, you MUST append `[ACTION:SHOW_PRODUCT:<id>]` on its own line at the very end of your response.\n");
        sb.append("   - This tag causes the user's interface to render an interactive product card with photo, price, and a 1-click 'Add to Cart' button.\n");
        sb.append("   - If you mention a product without appending this tag, the user cannot see or buy the item!\n\n");

        sb.append("2. HOT DEALS & PROMOTIONS INQUIRIES:\n");
        sb.append("   - When a user asks \"whats hot deal\", \"deals\", \"discounts\", \"sale\", \"offers\", \"promotions\", \"specials\", or \"bundles\":\n");
        sb.append("     a) Immediately highlight the active Hot Deals and Deal Bundles from the LIVE PROMOTIONS section below.\n");
        sb.append("     b) Clearly state the product name, the discounted PROMO PRICE (in LKR), the original price, and the DISCOUNT PERCENTAGE (e.g. \"Rs. 890,000 (was Rs. 990,000 - 10% OFF)\").\n");
        sb.append("     c) Note whether FREE SHIPPING is included.\n");
        sb.append("     d) ALWAYS append `[ACTION:SHOW_PRODUCT:<id>]` for the featured hot deal product(s) at the bottom.\n\n");

        sb.append("3. CART & NAVIGATION ACTIONS:\n");
        sb.append("   - If user asks about their cart or wants to view it: append `[ACTION:NAVIGATE#cart]`.\n");
        sb.append("   - If user asks to shop or browse the catalog: append `[ACTION:NAVIGATE#shop]`.\n");
        sb.append("   - If user asks to checkout or buy: append `[ACTION:NAVIGATE#checkout]`.\n");
        sb.append("   - If user asks to add a specific product to cart: append `[ACTION:ADD_TO_CART:<id>]`.\n");
        sb.append("   - Place all `[ACTION:...]` tags at the bottom of the response, each on its own line.\n\n");

        sb.append("4. STORE POLICIES & LOGISTICS:\n");
        sb.append("   - Shipping: Island-wide delivery from 4 regional hubs: Colombo, Galle, Matara, and Kandy. Base delivery starts at LKR 350.00.\n");
        sb.append("   - Warranty: 100% genuine authorized distributor warranty (standard 2 to 3 years on laptops/GPUs, lifetime on RAM).\n");
        sb.append("   - Replacements: 7-day hassle-free replacement for defective units.\n");
        sb.append("   - Support Email: eteccomputers38@gmail.com (Always provide as plain text, DO NOT make it a hyperlink).\n\n");

        // Dynamic Live Context
        sb.append(buildLiveCustomerAndOrdersContext(authentication));
        sb.append(buildLiveDealsContext());
        sb.append(buildLiveBranchesContext());
        sb.append(buildLiveStoreProfileContext());
        sb.append(buildLiveCartContext(cart));
        sb.append(buildLiveCatalogContext());

        return sb.toString();
    }

    private String buildLiveCustomerAndOrdersContext(Authentication authentication) {
        StringBuilder sb = new StringBuilder();

        boolean isAuthenticated = authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equalsIgnoreCase(authentication.getName());

        if (isAuthenticated) {
            String username = authentication.getName();
            Optional<User> userOpt = userRepository.findByUsername(username);

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                sb.append("═════════════════════════════════════════════════════════════\n");
                sb.append("  AUTHENTICATED CUSTOMER CONTEXT (Logged In)\n");
                sb.append("═════════════════════════════════════════════════════════════\n");
                sb.append(String.format("• Customer Name: %s (Username: %s, Email: %s)\n",
                        user.getName() != null ? user.getName() : user.getUsername(),
                        user.getUsername(),
                        user.getEmail() != null ? user.getEmail() : ""));
                sb.append(String.format("• Role: %s\n", user.getRole() != null ? user.getRole().name() : "CUSTOMER"));

                List<Order> orders = orderRepository.findByUserIdOrderByOrderDateDesc(user.getId());
                if (orders != null && !orders.isEmpty()) {
                    sb.append(String.format("• CUSTOMER'S ACTIVE & PAST ORDERS (%d orders found):\n", orders.size()));
                    int limit = Math.min(5, orders.size());
                    for (int i = 0; i < limit; i++) {
                        Order o = orders.get(i);
                        StringBuilder itemsSummary = new StringBuilder();
                        if (o.getItems() != null) {
                            for (OrderItem item : o.getItems()) {
                                String prodName = item.getProduct() != null ? item.getProduct().getName() : "Item";
                                if (itemsSummary.length() > 0) itemsSummary.append(", ");
                                itemsSummary.append(String.format("%s (x%d)", prodName, item.getQuantity() != null ? item.getQuantity() : 1));
                            }
                        }
                        sb.append(String.format("  - Order #%s | Date: %s | Status: %s | Total: LKR %,.2f | Items: [%s]\n",
                                o.getOrderCode(),
                                o.getOrderDate() != null ? o.getOrderDate().toLocalDate() : "Recent",
                                o.getStatus() != null ? o.getStatus().name() : "Processing",
                                o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO,
                                itemsSummary.toString()));
                    }

                    Order latest = orders.get(0);
                    sb.append(String.format("\nINSTRUCTION FOR THIS USER:\n"
                            + "- The customer is CURRENTLY LOGGED IN as %s.\n"
                            + "- When they ask \"whats orders i have\", \"my orders\", \"where is my order\", or ask about their account:\n"
                            + "  1. Directly answer them by listing their recent order(s) above (mention Order #%s, status, items, and total in LKR).\n"
                            + "  2. Append [ACTION:NAVIGATE#order-details?id=%s] at the very bottom so they can view details with one click!\n\n",
                            user.getName() != null ? user.getName() : user.getUsername(),
                            latest.getOrderCode(),
                            latest.getOrderCode()));
                } else {
                    sb.append("• ACTIVE & PAST ORDERS: 0 orders found (New customer, no previous purchases yet).\n\n");
                    sb.append("INSTRUCTION FOR THIS USER:\n"
                            + "- When they ask about their orders, inform them: \"You don't have any past orders yet. Browse our shop catalog to place your first order!\"\n"
                            + "- Append [ACTION:NAVIGATE#shop] at the bottom.\n\n");
                }

                return sb.toString();
            }
        }

        // Guest / Unauthenticated
        sb.append("═════════════════════════════════════════════════════════════\n");
        sb.append("  USER STATUS: GUEST (Not Logged In)\n");
        sb.append("═════════════════════════════════════════════════════════════\n");
        sb.append("INSTRUCTION FOR THIS USER:\n"
                + "- The user is currently browsing as an unauthenticated GUEST.\n"
                + "- If they ask \"whats orders i have\", \"my orders\", \"order status\", or ask about their account/profile:\n"
                + "  1. Politely inform them: \"You are currently browsing as a guest. To view your active orders and track deliveries, please log into your ETech Computers account.\"\n"
                + "  2. MANDATORY: Append [ACTION:NAVIGATE#login?redirect=account] on its own line at the end so they can log in with one click.\n\n");

        return sb.toString();
    }

    private String buildLiveDealsContext() {
        StringBuilder sb = new StringBuilder();
        sb.append("═════════════════════════════════════════════════════════════\n");
        sb.append("  LIVE HOT DEALS & PROMOTIONS IN STORE (Active Now)\n");
        sb.append("═════════════════════════════════════════════════════════════\n");

        try {
            List<HotDeal> hotDeals = hotDealRepository.findByIsActiveTrue();
            if (hotDeals != null && !hotDeals.isEmpty()) {
                sb.append("🔥 CURRENT HOT DEALS:\n");
                for (HotDeal hd : hotDeals) {
                    Product p = hd.getProduct();
                    if (p != null) {
                        sb.append(String.format("• [ID %d] %s — Promo Price: LKR %,.2f (Original: LKR %,.2f) | Discount: %d%% OFF | Free Shipping: %s | Badge: %s\n",
                                p.getId(),
                                p.getName(),
                                hd.getPromoPrice() != null ? hd.getPromoPrice() : p.getPrice(),
                                hd.getOriginalPrice() != null ? hd.getOriginalPrice() : p.getOriginalPrice(),
                                hd.getDiscountPercent() != null ? hd.getDiscountPercent() : 0,
                                Boolean.TRUE.equals(hd.getIsFreeShipping()) ? "YES" : "Standard",
                                hd.getBadge() != null ? hd.getBadge() : "HOT DEAL"));
                    }
                }
            } else {
                sb.append("• No active individual hot deals currently running.\n");
            }

            List<DealBundle> bundles = dealBundleRepository.findAll().stream()
                    .filter(b -> Boolean.TRUE.equals(b.getIsActive()))
                    .collect(Collectors.toList());
            if (!bundles.isEmpty()) {
                sb.append("\n📦 CURRENT DEAL BUNDLES:\n");
                for (DealBundle b : bundles) {
                    sb.append(String.format("• %s (%s) — Bundle Price: LKR %,.2f (Was: LKR %,.2f) | Free Shipping: %s | Badge: %s\n",
                            b.getTitle(),
                            b.getSubtitle() != null ? b.getSubtitle() : "",
                            b.getPrice(),
                            b.getOriginalPrice() != null ? b.getOriginalPrice() : b.getPrice(),
                            Boolean.TRUE.equals(b.getIsFreeShipping()) ? "YES" : "Standard",
                            b.getBadge() != null ? b.getBadge() : "BEST DEAL"));
                }
            }

            homeDealBannerRepository.findById(1).ifPresent(banner -> {
                if (Boolean.TRUE.equals(banner.getIsActive())) {
                    sb.append(String.format("\n🌟 STORE DEAL BANNER: %s — %s (%s)\n",
                            banner.getDealTag(), banner.getHeading(), banner.getSubtitle()));
                }
            });

        } catch (Exception e) {
            log.warn("Failed to load hot deals context: {}", e.getMessage());
        }

        sb.append("\n");
        return sb.toString();
    }

    private String buildLiveBranchesContext() {
        StringBuilder sb = new StringBuilder();
        sb.append("═════════════════════════════════════════════════════════════\n");
        sb.append("  REGIONAL HUBS & LOGISTICS (Island-wide Delivery)\n");
        sb.append("═════════════════════════════════════════════════════════════\n");

        try {
            List<Branch> branches = branchRepository.findAll();
            for (Branch b : branches) {
                if (Boolean.TRUE.equals(b.getActive())) {
                    sb.append(String.format("• [%s] %s (%s) — %s | Hotline: %s | Base Delivery: LKR %,.2f\n",
                            b.getId(), b.getName(), b.getCity(), b.getAddress(), b.getPhone(), b.getBaseShippingRate()));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load branches context: {}", e.getMessage());
        }

        sb.append("\n");
        return sb.toString();
    }

    private String buildLiveStoreProfileContext() {
        StringBuilder sb = new StringBuilder();
        sb.append("═════════════════════════════════════════════════════════════\n");
        sb.append("  STORE PROFILE & LEGAL POLICIES\n");
        sb.append("═════════════════════════════════════════════════════════════\n");

        try {
            businessProfileRepository.findById(1).ifPresent(bp -> {
                sb.append(String.format("• Store Name: %s | Tagline: %s\n", bp.getStoreName(), bp.getTagline()));
                sb.append(String.format("• Support Hotline: %s | Email: %s (do not link)\n", bp.getHotline(), bp.getSupportEmail()));
                sb.append(String.format("• Headquarters: %s | Working Hours: %s\n", bp.getHeadquarters(), bp.getWorkingHours()));
            });

            List<LegalPolicy> policies = legalPolicyRepository.findAll();
            for (LegalPolicy lp : policies) {
                sb.append(String.format("• Policy: %s (%s)\n", lp.getTitle(), lp.getSubtitle()));
            }
        } catch (Exception e) {
            log.warn("Failed to load store profile context: {}", e.getMessage());
        }

        sb.append("\n");
        return sb.toString();
    }

    private String buildLiveCartContext(List<Object> cart) {
        if (cart == null || cart.isEmpty()) {
            return "═════════════════════════════════════════════════════════════\n  USER CART: Currently Empty (Suggest popular products or deals).\n═════════════════════════════════════════════════════════════\n\n";
        }
        try {
            return String.format("═════════════════════════════════════════════════════════════\n  USER CART: %s\n═════════════════════════════════════════════════════════════\n\n",
                    objectMapper.writeValueAsString(cart));
        } catch (Exception e) {
            return "USER CART: Contains items.\n\n";
        }
    }

    private String buildLiveCatalogContext() {
        StringBuilder sb = new StringBuilder();
        sb.append("═════════════════════════════════════════════════════════════\n");
        sb.append("  OFFICIAL STORE CATALOG (Use Product IDs for [ACTION:SHOW_PRODUCT:id])\n");
        sb.append("═════════════════════════════════════════════════════════════\n");

        try {
            List<Product> products = productRepository.findAll().stream()
                    .filter(p -> p.getProductStatus() != null && p.getProductStatus() != Status.DELETED)
                    .collect(Collectors.toList());

            if (products.isEmpty()) {
                sb.append("• Catalog is currently being refreshed.\n");
                return sb.toString();
            }

            for (Product p : products) {
                String category = p.getCategory() != null ? p.getCategory().getName() : "Hardware";
                String brand = p.getBrand() != null ? p.getBrand().getName() : "ETech";
                String warranty = p.getWarranty() != null ? p.getWarranty() : "2-Year Warranty";
                String rating = p.getRating() != null ? p.getRating().toString() : "5.0";
                String badge = p.getBadge() != null ? p.getBadge().getName() : "";
                String descSnippet = p.getDescription() != null ? p.getDescription().replace("\n", " ").trim() : "";
                if (descSnippet.length() > 90) {
                    descSnippet = descSnippet.substring(0, 90) + "...";
                }

                sb.append(String.format("• ID %d: %s | LKR %,.2f | %s | %s | %s | ⭐ %s %s | %s\n",
                        p.getId(),
                        p.getName(),
                        p.getPrice() != null ? p.getPrice() : 0.0,
                        category,
                        brand,
                        warranty,
                        rating,
                        badge.isEmpty() ? "" : "[" + badge + "]",
                        descSnippet));
            }
        } catch (Exception e) {
            log.warn("Failed to generate catalog context: {}", e.getMessage());
        }

        sb.append("\n");
        return sb.toString();
    }
}
