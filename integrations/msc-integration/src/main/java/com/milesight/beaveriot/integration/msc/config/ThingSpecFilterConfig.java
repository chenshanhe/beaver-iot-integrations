package com.milesight.beaveriot.integration.msc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Configuration for thing spec filters
 */
@Data
@Component
@ConfigurationProperties(prefix = "msc.thing-spec-filter")
public class ThingSpecFilterConfig {

    /**
     * Map of thing spec filters
     * Key: unique filter name
     * Value: filter configuration
     */
    private Map<String, FilterConfig> models = new HashMap<>();

    @Data
    public static class FilterConfig {
        /**
         * Device model pattern (support wildcard *)
         * e.g., "EM500-SMTC*" matches all models starting with "EM500-SMTC"
         */
        private String modelPattern;

        /**
         * Filter mode: WHITELIST or BLACKLIST
         */
        private FilterMode mode = FilterMode.WHITELIST;

        /**
         * Priority (higher value = higher priority)
         */
        private int priority = 0;

        /**
         * IDs to filter
         */
        private Set<String> ids = Set.of();
    }

    public enum FilterMode {
        /**
         * Whitelist mode: only keep specified properties/events/services
         */
        WHITELIST,

        /**
         * Blacklist mode: exclude specified properties/events/services
         */
        BLACKLIST
    }
}
