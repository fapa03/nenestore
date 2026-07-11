package com.nenestore.api.service;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SizeNormalizer {

    private static final Map<String, String> SIZE_MAP = Map.ofEntries(
            Map.entry("xsmall", "XS"),
            Map.entry("x-small", "XS"),
            Map.entry("small", "S"),
            Map.entry("medium", "M"),
            Map.entry("large", "L"),
            Map.entry("xlarge", "XL"),
            Map.entry("x-large", "XL"),
            Map.entry("xllarge", "XL"),
            Map.entry("xxlarge", "XXL"),
            Map.entry("xx-large", "XXL"),
            Map.entry("2xl", "XXL"),
            Map.entry("2x", "XXL"),
            Map.entry("xxxlarge", "XXXL"),
            Map.entry("3xl", "XXXL"));

    public String normalize(String size) {
        if (size == null || size.isBlank())
            return "";
        String key = size.trim().toLowerCase().replaceAll("\\s+", "");
        return SIZE_MAP.getOrDefault(key, size.trim().toUpperCase());
    }
}