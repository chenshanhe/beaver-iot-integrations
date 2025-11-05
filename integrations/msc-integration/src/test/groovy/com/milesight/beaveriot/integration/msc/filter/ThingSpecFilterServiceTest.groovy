package com.milesight.beaveriot.integration.msc.filter

import com.milesight.beaveriot.integration.msc.config.ThingSpecFilterConfig
import com.milesight.cloud.sdk.client.model.ThingSpec
import com.milesight.cloud.sdk.client.model.TslEventSpec
import com.milesight.cloud.sdk.client.model.TslPropertySpec
import com.milesight.cloud.sdk.client.model.TslServiceSpec
import spock.lang.Specification
import spock.lang.Subject

class ThingSpecFilterServiceTest extends Specification {

    ThingSpecFilterConfig filterConfig = new ThingSpecFilterConfig()

    @Subject
    ThingSpecFilterService service = new ThingSpecFilterService()

    def setup() {
        service.filterConfig = filterConfig
    }

    def "should load code-based filters"() {
        given: "a device filter provider with annotation"
        def provider = new TestDeviceFilter()
        service.thingSpecFilterProviders = [provider]

        when: "initialize service"
        service.init()

        then: "filter rules should be loaded"
        service.filterRules.size() == 1
        service.filterRules[0].modelPattern == "TEST-MODEL*"
        service.filterRules[0].mode == ThingSpecFilterProvider.FilterMode.WHITELIST
        service.filterRules[0].priority == 100
    }

    def "should load config-based filters"() {
        given: "configuration with filter rules"
        def config = new ThingSpecFilterConfig.FilterConfig()
        config.modelPattern = "CONFIG-MODEL*"
        config.mode = ThingSpecFilterConfig.FilterMode.BLACKLIST
        config.priority = 50
        config.ids = ["prop1", "prop2"] as Set
        filterConfig.models = ["configFilter": config]

        when: "initialize service"
        service.init()

        then: "filter rules should be loaded from config"
        service.filterRules.size() == 1
        service.filterRules[0].modelPattern == "CONFIG-MODEL*"
        service.filterRules[0].mode == ThingSpecFilterProvider.FilterMode.BLACKLIST
        service.filterRules[0].priority == 50
        service.filterRules[0].ids.containsAll(["prop1", "prop2"])
    }

    def "should filter properties in whitelist mode"() {
        given: "a ThingSpec with multiple properties"
        def thingSpec = new ThingSpec()
        thingSpec.properties = [
                createProperty("temperature"),
                createProperty("humidity"),
                createProperty("pressure"),
                createProperty("debug_info")
        ]

        and: "a whitelist filter rule"
        def rule = ThingSpecFilterRule.builder()
                .modelPattern("EM500-SMTC*")
                .mode(ThingSpecFilterProvider.FilterMode.WHITELIST)
                .priority(100)
                .ids(["temperature", "humidity"] as Set)
                .build()
        service.filterRules = [rule]
        service.init()

        when: "apply filter"
        def filtered = service.filter(thingSpec, "EM500-SMTC-V1")

        then: "only whitelisted properties should remain"
        filtered.properties.size() == 2
        filtered.properties*.id.containsAll(["temperature", "humidity"])
        !filtered.properties*.id.contains("pressure")
        !filtered.properties*.id.contains("debug_info")
    }

    def "should filter properties in blacklist mode"() {
        given: "a ThingSpec with multiple properties"
        def thingSpec = new ThingSpec()
        thingSpec.properties = [
                createProperty("temperature"),
                createProperty("humidity"),
                createProperty("debug_info")
        ]

        and: "a blacklist filter rule"
        def rule = ThingSpecFilterRule.builder()
                .modelPattern("EM500-SMTC*")
                .mode(ThingSpecFilterProvider.FilterMode.BLACKLIST)
                .priority(100)
                .ids(["debug_info"] as Set)
                .build()
        service.filterRules = [rule]

        when: "apply filter"
        def filtered = service.filter(thingSpec, "EM500-SMTC-V1")

        then: "blacklisted properties should be removed"
        filtered.properties.size() == 2
        filtered.properties*.id.containsAll(["temperature", "humidity"])
        !filtered.properties*.id.contains("debug_info")
    }

    def "should filter nested struct properties"() {
        given: "a ThingSpec with nested properties"
        def thingSpec = new ThingSpec()
        thingSpec.properties = [
                createProperty("temperature_mutation_alarm"),
                createProperty("temperature_mutation_alarm.temperature"),
                createProperty("temperature_mutation_alarm.alarm_type"),
                createProperty("simple_prop")
        ]

        and: "a whitelist filter including parent struct"
        def rule = ThingSpecFilterRule.builder()
                .modelPattern("EM500-SMTC*")
                .mode(ThingSpecFilterProvider.FilterMode.WHITELIST)
                .priority(100)
                .ids([
                        "temperature_mutation_alarm",
                        "temperature_mutation_alarm.temperature"
                ] as Set)
                .build()
        service.filterRules = [rule]

        when: "apply filter"
        def filtered = service.filter(thingSpec, "EM500-SMTC-V1")

        then: "parent and whitelisted child should remain"
        filtered.properties.size() == 2
        filtered.properties*.id.containsAll([
                "temperature_mutation_alarm",
                "temperature_mutation_alarm.temperature"
        ])
        !filtered.properties*.id.contains("temperature_mutation_alarm.alarm_type")
    }

    def "should return original ThingSpec when no filter matches"() {
        given: "a ThingSpec"
        def thingSpec = new ThingSpec()
        thingSpec.properties = [createProperty("prop1"), createProperty("prop2")]

        and: "a filter rule for different model"
        def rule = ThingSpecFilterRule.builder()
                .modelPattern("OTHER-MODEL*")
                .mode(ThingSpecFilterProvider.FilterMode.WHITELIST)
                .priority(100)
                .ids(["prop1"] as Set)
                .build()
        service.filterRules = [rule]

        when: "apply filter with non-matching model"
        def filtered = service.filter(thingSpec, "EM500-SMTC")

        then: "original ThingSpec should be returned"
        filtered == thingSpec
        filtered.properties.size() == 2
    }

    def "should select highest priority filter when multiple match"() {
        given: "a ThingSpec"
        def thingSpec = new ThingSpec()
        thingSpec.properties = [
                createProperty("prop1"),
                createProperty("prop2"),
                createProperty("prop3")
        ]

        and: "multiple filter rules with different priorities"
        def lowPriorityRule = ThingSpecFilterRule.builder()
                .modelPattern("EM500*")
                .mode(ThingSpecFilterProvider.FilterMode.WHITELIST)
                .priority(10)
                .ids(["prop1"] as Set)
                .build()

        def highPriorityRule = ThingSpecFilterRule.builder()
                .modelPattern("EM500-SMTC*")
                .mode(ThingSpecFilterProvider.FilterMode.WHITELIST)
                .priority(100)
                .ids(["prop1", "prop2"] as Set)
                .build()

        service.filterRules = [highPriorityRule, lowPriorityRule]

        when: "apply filter"
        def filtered = service.filter(thingSpec, "EM500-SMTC-V1")

        then: "higher priority rule should be applied"
        filtered.properties.size() == 2
        filtered.properties*.id.containsAll(["prop1", "prop2"])
    }

    def "should filter services"() {
        given: "a ThingSpec with services"
        def thingSpec = new ThingSpec()
        thingSpec.services = [
                createService("reboot"),
                createService("clear_historical_data"),
                createService("debug_command")
        ]

        and: "a whitelist filter rule"
        def rule = ThingSpecFilterRule.builder()
                .modelPattern("EM500-SMTC*")
                .mode(ThingSpecFilterProvider.FilterMode.WHITELIST)
                .priority(100)
                .ids(["reboot", "clear_historical_data"] as Set)
                .build()
        service.filterRules = [rule]

        when: "apply filter"
        def filtered = service.filter(thingSpec, "EM500-SMTC-V1")

        then: "only whitelisted services should remain"
        filtered.services.size() == 2
        filtered.services*.id.containsAll(["reboot", "clear_historical_data"])
    }

    def "should filter events"() {
        given: "a ThingSpec with events"
        def thingSpec = new ThingSpec()
        thingSpec.events = [
                createEvent("alarm"),
                createEvent("status_change"),
                createEvent("debug_event")
        ]

        and: "a blacklist filter rule"
        def rule = ThingSpecFilterRule.builder()
                .modelPattern("EM500-SMTC*")
                .mode(ThingSpecFilterProvider.FilterMode.BLACKLIST)
                .priority(100)
                .ids(["debug_event"] as Set)
                .build()
        service.filterRules = [rule]

        when: "apply filter"
        def filtered = service.filter(thingSpec, "EM500-SMTC-V1")

        then: "blacklisted event should be removed"
        filtered.events.size() == 2
        filtered.events*.id.containsAll(["alarm", "status_change"])
    }

    // Helper methods
    private TslPropertySpec createProperty(String id) {
        def property = new TslPropertySpec()
        property.id = id
        return property
    }

    private TslServiceSpec createService(String id) {
        def service = new TslServiceSpec()
        service.id = id
        return service
    }

    private TslEventSpec createEvent(String id) {
        def event = new TslEventSpec()
        event.id = id
        return event
    }

    // Test filter provider
    static class TestDeviceFilter implements ThingSpecFilterProvider {
        @Override
        String getModelPattern() {
            return "TEST-MODEL*"
        }

        @Override
        FilterMode getMode() {
            return FilterMode.WHITELIST
        }

        @Override
        int getPriority() {
            return 100
        }

        @Override
        Set<String> getIds() {
            return ["prop1", "prop2"] as Set
        }
    }
}
