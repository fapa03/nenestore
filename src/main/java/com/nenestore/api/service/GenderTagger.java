package com.nenestore.api.service;

import org.springframework.stereotype.Component;
import java.util.Set;

@Component
public class GenderTagger {

    private static final Set<String> FEMALE_KEYWORDS = Set.of(
        "legging", "leggings", "bra", "sports bra",
        "crop", "crop top", "skirt"
    );

    private static final Set<String> MALE_KEYWORDS = Set.of(
        "jogger", "joggers", "tank", "tee", "hoodie",
        "sweat", "sweats", "trunks"
    );

    public String tag(String orderId, String productName, String size) {

        // Universal rule — no size means accessory
        if (size == null || size.trim().equalsIgnoreCase("N/A") || size.trim().isEmpty()) {
            return "U";
        }

        String product = productName.toLowerCase().trim();

        // YoungLA logic
        if (orderId.startsWith("YLA")) {
            return productName.trim().startsWith("W") ? "F" : "M";
        }

        // Gymshark — keyword based
        for (String keyword : FEMALE_KEYWORDS) {
            if (product.contains(keyword)) return "F";
        }
        for (String keyword : MALE_KEYWORDS) {
            if (product.contains(keyword)) return "M";
        }

        // Default
        return "U";
    }
}