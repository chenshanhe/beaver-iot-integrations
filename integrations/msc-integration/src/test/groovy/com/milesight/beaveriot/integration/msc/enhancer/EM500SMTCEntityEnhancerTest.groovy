package com.milesight.beaveriot.integration.msc.enhancer

import com.milesight.beaveriot.context.integration.enums.AccessMod
import com.milesight.beaveriot.context.integration.enums.EntityValueType
import com.milesight.beaveriot.context.integration.model.EntityBuilder
import spock.lang.Specification

/**
 * Test for EM500SMTCEntityEnhancer
 */
class EM500SMTCEntityEnhancerTest extends Specification {

    def "should add important attribute to temperature, soil_moisture and conductivity entities"() {
        given: "a list of entities for EM500-SMTC device"
        def integrationId = "msc-integration"
        def deviceKey = "msc-integration:test-device"

        def entities = [
                new EntityBuilder(integrationId, deviceKey)
                        .identifier("temperature")
                        .property("Temperature", AccessMod.R)
                        .valueType(EntityValueType.DOUBLE)
                        .build(),
                new EntityBuilder(integrationId, deviceKey)
                        .identifier("soil_moisture")
                        .property("Soil Moisture", AccessMod.R)
                        .valueType(EntityValueType.DOUBLE)
                        .build(),
                new EntityBuilder(integrationId, deviceKey)
                        .identifier("conductivity")
                        .property("Conductivity", AccessMod.R)
                        .valueType(EntityValueType.DOUBLE)
                        .build(),
                new EntityBuilder(integrationId, deviceKey)
                        .identifier("battery")
                        .property("Battery", AccessMod.R)
                        .valueType(EntityValueType.LONG)
                        .build()
        ]

        when: "enhance is called with EM500-SMTC model"
        EM500SMTCEntityEnhancer.enhance(entities, "EM500-SMTC")

        then: "temperature should have important=1"
        entities[0].attributes != null
        entities[0].attributes["important"] == 1

        and: "soil_moisture should have important=2"
        entities[1].attributes != null
        entities[1].attributes["important"] == 2

        and: "conductivity should have important=3"
        entities[2].attributes != null
        entities[2].attributes["important"] == 3

        and: "battery should not have important attribute"
        entities[3].attributes == null || entities[3].attributes["important"] == null
    }

    def "should handle EM500-SMTC variant models"() {
        given: "a temperature entity"
        def integrationId = "msc-integration"
        def deviceKey = "msc-integration:test-device"

        def entities = [
                new EntityBuilder(integrationId, deviceKey)
                        .identifier("temperature")
                        .property("Temperature", AccessMod.R)
                        .valueType(EntityValueType.DOUBLE)
                        .build()
        ]

        when: "enhance is called with EM500-SMTC-868M model"
        EM500SMTCEntityEnhancer.enhance(entities, "EM500-SMTC-868M")

        then: "temperature should have important=1"
        entities[0].attributes != null
        entities[0].attributes["important"] == 1
    }

    def "should not enhance entities for non-EM500-SMTC devices"() {
        given: "a temperature entity"
        def integrationId = "msc-integration"
        def deviceKey = "msc-integration:test-device"

        def entities = [
                new EntityBuilder(integrationId, deviceKey)
                        .identifier("temperature")
                        .property("Temperature", AccessMod.R)
                        .valueType(EntityValueType.DOUBLE)
                        .build()
        ]

        when: "enhance is called with different device model"
        EM500SMTCEntityEnhancer.enhance(entities, "EM310-UDL")

        then: "temperature should not have important attribute"
        entities[0].attributes == null || entities[0].attributes["important"] == null
    }

    def "should handle null device model gracefully"() {
        given: "a temperature entity"
        def integrationId = "msc-integration"
        def deviceKey = "msc-integration:test-device"

        def entities = [
                new EntityBuilder(integrationId, deviceKey)
                        .identifier("temperature")
                        .property("Temperature", AccessMod.R)
                        .valueType(EntityValueType.DOUBLE)
                        .build()
        ]

        when: "enhance is called with null device model"
        EM500SMTCEntityEnhancer.enhance(entities, null)

        then: "no exception is thrown"
        noExceptionThrown()

        and: "temperature should not have important attribute"
        entities[0].attributes == null || entities[0].attributes["important"] == null
    }

    def "should enhance entities with existing attributes"() {
        given: "a temperature entity with existing attributes"
        def integrationId = "msc-integration"
        def deviceKey = "msc-integration:test-device"

        def entities = [
                new EntityBuilder(integrationId, deviceKey)
                        .identifier("temperature")
                        .property("Temperature", AccessMod.R)
                        .valueType(EntityValueType.DOUBLE)
                        .attributes(["unit": "°C", "min": 0.0, "max": 50.0])
                        .build()
        ]

        when: "enhance is called"
        EM500SMTCEntityEnhancer.enhance(entities, "EM500-SMTC")

        then: "temperature should have important=1 along with existing attributes"
        entities[0].attributes != null
        entities[0].attributes["important"] == 1
        entities[0].attributes["unit"] == "°C"
        entities[0].attributes["min"] == 0.0
        entities[0].attributes["max"] == 50.0
    }

    def "should enhance nested entities"() {
        given: "a parent entity with children"
        def integrationId = "msc-integration"
        def deviceKey = "msc-integration:test-device"

        def childEntity = new EntityBuilder(integrationId, deviceKey)
                .identifier("temperature")
                .property("Temperature", AccessMod.R)
                .valueType(EntityValueType.DOUBLE)
                .build()

        def parentEntity = new EntityBuilder(integrationId, deviceKey)
                .identifier("sensor_data")
                .property("Sensor Data", AccessMod.R)
                .valueType(EntityValueType.OBJECT)
                .children([childEntity])
                .build()

        def entities = [parentEntity]

        when: "enhance is called"
        EM500SMTCEntityEnhancer.enhance(entities, "EM500-SMTC")

        then: "child temperature entity should have important=1"
        childEntity.attributes != null
        childEntity.attributes["important"] == 1
    }
}
