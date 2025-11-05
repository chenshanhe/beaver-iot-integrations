package com.milesight.beaveriot.integration.msc.filter;

import java.util.Set;

/**
 * Interface for thing spec filter provider
 * Implement this interface and register as a Spring component to declare filter rules
 */
public interface ThingSpecFilterProvider {

    /**
     * Device model pattern for identification
     * Support prefix matching with wildcard, e.g., "EM500-SMTC*"
     *
     * @return model pattern string
     */
    String getModelPattern();

    /**
     * Filter mode: WHITELIST or BLACKLIST
     *
     * @return filter mode
     */
    FilterMode getMode();

    /**
     * Filter priority, higher value means higher priority
     * Used when multiple filters match the same device model
     *
     * @return priority value, default is 0
     */
    default int getPriority() {
        return 0;
    }

    /**
     * Get IDs to filter
     *
     * @return set of identifiers
     */
    Set<String> getIds();

    enum FilterMode {
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
