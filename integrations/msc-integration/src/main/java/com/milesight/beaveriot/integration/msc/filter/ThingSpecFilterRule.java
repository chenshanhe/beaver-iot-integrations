package com.milesight.beaveriot.integration.msc.filter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Thing spec filter rule
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThingSpecFilterRule {

    /**
     * Device model pattern (support wildcard *)
     * e.g., "EM500-SMTC*" matches all models starting with "EM500-SMTC"
     */
    private String modelPattern;

    /**
     * Filter mode
     */
    private ThingSpecFilterProvider.FilterMode mode;

    /**
     * Priority (higher value = higher priority)
     */
    private int priority;

    /**
     * IDs to filter
     */
    private Set<String> ids;

    /**
     * Source of the filter rule
     */
    private FilterSource source;

    public enum FilterSource {
        /**
         * From code annotation
         */
        CODE,

        /**
         * From configuration file
         */
        CONFIG
    }

    /**
     * Check if the model matches the pattern
     */
    public boolean matches(String model) {
        if (model == null || modelPattern == null) {
            return false;
        }

        // Convert pattern to regex
        String regex = modelPattern
                .replace(".", "\\.")
                .replace("*", ".*");

        return model.matches(regex);
    }
}
