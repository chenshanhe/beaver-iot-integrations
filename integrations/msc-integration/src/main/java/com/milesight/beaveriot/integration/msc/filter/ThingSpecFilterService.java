package com.milesight.beaveriot.integration.msc.filter;

import com.milesight.beaveriot.integration.msc.config.ThingSpecFilterConfig;
import com.milesight.cloud.sdk.client.model.ThingSpec;
import com.milesight.cloud.sdk.client.model.TslEventSpec;
import com.milesight.cloud.sdk.client.model.TslPropertySpec;
import com.milesight.cloud.sdk.client.model.TslServiceSpec;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for filtering ThingSpec based on device model
 */
@Slf4j
@Service
public class ThingSpecFilterService {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private List<ThingSpecFilterProvider> thingSpecFilterProviders;

    @Autowired
    private ThingSpecFilterConfig filterConfig;

    private List<ThingSpecFilterRule> filterRules;

    @PostConstruct
    public void init() {
        if (filterRules == null) {
            filterRules = new ArrayList<>();
        }

        loadCodeBasedFilters();
        loadConfigBasedFilters();

        // Sort by priority (descending)
        filterRules.sort(Comparator.comparingInt(ThingSpecFilterRule::getPriority).reversed());

        log.info("Loaded {} thing spec filter rules", filterRules.size());
        filterRules.forEach(rule ->
                log.info("  - Pattern: {}, Mode: {}, Priority: {}, Source: {}",
                        rule.getModelPattern(), rule.getMode(), rule.getPriority(), rule.getSource()));
    }

    /**
     * Load filters from ThingSpecFilterProvider implementations
     */
    private void loadCodeBasedFilters() {
        if (thingSpecFilterProviders == null || thingSpecFilterProviders.isEmpty()) {
            log.info("No code-based filter found.");
            return;
        }

        thingSpecFilterProviders.forEach(provider -> {
            val rule = ThingSpecFilterRule.builder()
                    .modelPattern(provider.getModelPattern())
                    .mode(provider.getMode())
                    .priority(provider.getPriority())
                    .ids(provider.getIds())
                    .source(ThingSpecFilterRule.FilterSource.CODE)
                    .build();

            filterRules.add(rule);
            log.debug("Loaded code-based filter: {}", rule.getModelPattern());
        });
    }

    /**
     * Load filters from configuration file
     */
    private void loadConfigBasedFilters() {
        if (filterConfig.getModels() == null || filterConfig.getModels().isEmpty()) {
            log.debug("No config-based filters found");
            return;
        }

        filterConfig.getModels().forEach((name, config) -> {
            val mode = config.getMode() == ThingSpecFilterConfig.FilterMode.WHITELIST
                    ? ThingSpecFilterProvider.FilterMode.WHITELIST
                    : ThingSpecFilterProvider.FilterMode.BLACKLIST;

            val rule = ThingSpecFilterRule.builder()
                    .modelPattern(config.getModelPattern())
                    .mode(mode)
                    .priority(config.getPriority())
                    .ids(new HashSet<>(config.getIds()))
                    .source(ThingSpecFilterRule.FilterSource.CONFIG)
                    .build();

            filterRules.add(rule);
            log.debug("Loaded config-based filter: {} -> {}", name, rule.getModelPattern());
        });
    }

    /**
     * Filter ThingSpec based on device model
     *
     * @param thingSpec   original ThingSpec
     * @param deviceModel device model from DeviceDetailResponse.getModel()
     * @return filtered ThingSpec, or original if no filter matches
     */
    public ThingSpec filter(ThingSpec thingSpec, String deviceModel) {
        if (thingSpec == null || deviceModel == null || deviceModel.isEmpty()) {
            return thingSpec;
        }

        // Find matching filter rule with the highest priority
        val matchingRule = filterRules.stream()
                .filter(rule -> rule.matches(deviceModel))
                .findFirst()
                .orElse(null);

        if (matchingRule == null) {
            log.debug("No filter rule matches device model: {}", deviceModel);
            return thingSpec;
        }

        log.info("Applying filter rule for device model '{}': pattern={}, mode={}, priority={}, source={}",
                deviceModel, matchingRule.getModelPattern(), matchingRule.getMode(),
                matchingRule.getPriority(), matchingRule.getSource());

        return applyFilter(thingSpec, matchingRule);
    }

    /**
     * Apply filter rule to ThingSpec
     */
    private ThingSpec applyFilter(ThingSpec thingSpec, ThingSpecFilterRule rule) {
        val filtered = new ThingSpec();

        // Filter properties
        if (thingSpec.getProperties() != null && !thingSpec.getProperties().isEmpty()) {
            val filteredProperties = filterItems(
                    thingSpec.getProperties(),
                    rule.getIds(),
                    rule.getMode(),
                    TslPropertySpec::getId
            );
            filtered.setProperties(filteredProperties);

            log.info("Properties: {} -> {} (filtered out: {})",
                    thingSpec.getProperties().size(),
                    filteredProperties.size(),
                    thingSpec.getProperties().size() - filteredProperties.size());
        }

        // Filter events
        if (thingSpec.getEvents() != null && !thingSpec.getEvents().isEmpty()) {
            val filteredEvents = filterItems(
                    thingSpec.getEvents(),
                    rule.getIds(),
                    rule.getMode(),
                    TslEventSpec::getId
            );
            filtered.setEvents(filteredEvents);

            log.info("Events: {} -> {} (filtered out: {})",
                    thingSpec.getEvents().size(),
                    filteredEvents.size(),
                    thingSpec.getEvents().size() - filteredEvents.size());
        }

        // Filter services
        if (thingSpec.getServices() != null && !thingSpec.getServices().isEmpty()) {
            val filteredServices = filterItems(
                    thingSpec.getServices(),
                    rule.getIds(),
                    rule.getMode(),
                    TslServiceSpec::getId
            );
            filtered.setServices(filteredServices);

            log.info("Services: {} -> {} (filtered out: {})",
                    thingSpec.getServices().size(),
                    filteredServices.size(),
                    thingSpec.getServices().size() - filteredServices.size());
        }

        return filtered;
    }

    /**
     * Filter items based on whitelist or blacklist mode
     */
    private <T> List<T> filterItems(
            List<T> items,
            Set<String> filterIds,
            ThingSpecFilterProvider.FilterMode mode,
            java.util.function.Function<T, String> idExtractor
    ) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        if (filterIds == null || filterIds.isEmpty()) {
            // No filter IDs specified
            if (mode == ThingSpecFilterProvider.FilterMode.WHITELIST) {
                // Whitelist mode with empty list = keep nothing
                return List.of();
            } else {
                // Blacklist mode with empty list = keep everything
                return new ArrayList<>(items);
            }
        }

        // Build a set of IDs including parent IDs for nested structures
        val expandedFilterIds = expandNestedIds(filterIds);

        return items.stream()
                .filter(item -> {
                    String id = idExtractor.apply(item);
                    if (id == null) {
                        return false;
                    }

                    boolean matches = expandedFilterIds.stream()
                            .anyMatch(filterId -> id.equals(filterId));

                    // Whitelist: keep if matches, Blacklist: keep if NOT matches
                    return (mode == ThingSpecFilterProvider.FilterMode.WHITELIST) == matches;
                })
                .collect(Collectors.toList());
    }

    /**
     * Expand nested IDs to include parent IDs
     * e.g., "temperature_mutation_alarm.temperature" -> ["temperature_mutation_alarm", "temperature_mutation_alarm.temperature"]
     */
    private Set<String> expandNestedIds(Set<String> ids) {
        val expanded = new HashSet<>(ids);

        ids.forEach(id -> {
            if (id.contains(".")) {
                val parts = id.split("\\.");
                val parentBuilder = new StringBuilder();

                for (int i = 0; i < parts.length - 1; i++) {
                    if (i > 0) {
                        parentBuilder.append(".");
                    }
                    parentBuilder.append(parts[i]);
                    expanded.add(parentBuilder.toString());
                }
            }
        });

        return expanded;
    }
}
