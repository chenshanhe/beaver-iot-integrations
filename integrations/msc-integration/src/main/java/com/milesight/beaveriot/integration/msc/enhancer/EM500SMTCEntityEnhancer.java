package com.milesight.beaveriot.integration.msc.enhancer;

import com.milesight.beaveriot.context.integration.model.Entity;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Entity enhancer for EM500-SMTC device model
 * Adds "important" attribute to specific entities with incremental values
 */
@Slf4j
public class EM500SMTCEntityEnhancer {

    /**
     * Model pattern to match EM500-SMTC devices
     */
    private static final String MODEL_PATTERN = "EM500-SMTC";

    /**
     * Mapping of entity identifiers to their importance values
     */
    private static final Map<String, Integer> IMPORTANCE_MAP = Map.of(
            "temperature", 1,
            "soil_moisture", 2,
            "conductivity", 3
    );

    /**
     * Enhance entities for EM500-SMTC device by adding "important" attribute
     *
     * @param entities    List of entities to enhance
     * @param deviceModel Device model string
     */
    public static void enhance(List<Entity> entities, String deviceModel) {
        if (deviceModel == null || !deviceModel.startsWith(MODEL_PATTERN)) {
            log.debug("Device model [{}] does not match EM500-SMTC pattern, skip enhancement", deviceModel);
            return;
        }

        log.debug("Enhancing entities for EM500-SMTC device model: {}", deviceModel);

        int enhancedCount = 0;
        for (Entity entity : entities) {
            if (enhanceEntity(entity)) {
                enhancedCount++;
            }
        }

        log.debug("Enhanced {} entities for EM500-SMTC device", enhancedCount);
    }

    /**
     * Enhance a single entity and its children
     *
     * @param entity Entity to enhance
     * @return true if entity was enhanced, false otherwise
     */
    private static boolean enhanceEntity(Entity entity) {
        boolean enhanced = false;

        // Check if this entity needs enhancement
        if (entity != null && entity.getIdentifier() != null) {
            Integer importance = IMPORTANCE_MAP.get(entity.getIdentifier());
            if (importance != null) {
                addImportantAttribute(entity, importance);
                log.debug("Added important={} to entity [{}]", importance, entity.getIdentifier());
                enhanced = true;
            }
        }

        // Recursively enhance children
        if (entity != null && entity.getChildren() != null) {
            for (Entity child : entity.getChildren()) {
                if (enhanceEntity(child)) {
                    enhanced = true;
                }
            }
        }

        return enhanced;
    }

    /**
     * Add "important" attribute to entity
     *
     * @param entity     Entity to modify
     * @param importance Importance value
     */
    private static void addImportantAttribute(Entity entity, Integer importance) {
        Map<String, Object> attributes = entity.getAttributes();

        if (attributes == null) {
            attributes = new HashMap<>();
            entity.setAttributes(attributes);
        }

        // Add or update the "important" attribute
        attributes.put("important", importance);
    }
}
